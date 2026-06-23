package com.backend.StockLinker.AuthService.service;

import com.backend.StockLinker.AuthService.constant.AuditAction;
import com.backend.StockLinker.AuthService.exception.customExceptions.AccountBlockedException;
import com.backend.StockLinker.AuthService.exception.customExceptions.InvalidTokenException;
import com.backend.StockLinker.AuthService.exception.customExceptions.ResourceNotFoundException;
import com.backend.StockLinker.AuthService.dto.response.AuthResponse;
import com.backend.StockLinker.AuthService.model.AuditLog;
import com.backend.StockLinker.AuthService.model.RefreshToken;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.model.UserDevice;
import com.backend.StockLinker.AuthService.repository.UserRepository;
import com.backend.StockLinker.AuthService.security.RefreshTokenService;
import com.backend.StockLinker.AuthService.security.TokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthFlowService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final RefreshTokenService refreshTokenService;
    private final DeviceSessionService deviceService;
    private final AuditService auditService;
    private final RoleService roleService;

    // =========================================================
    // ✅ MAIN LOGIN FLOW (ALL LOGIN TYPES COME HERE)
    // =========================================================
    public AuthResponse login(
            User user,
            String provider,
            String deviceId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            // Validate device ID
            if (deviceId == null || deviceId.isBlank()) {
                throw new IllegalArgumentException("Device ID is required");
            }

            // Account validation - Using custom exception
            if (!user.isActive()) {
                auditService.log(auditService.failure(
                        user, AuditAction.LOGIN_FAILED.name(),
                        "Account blocked or locked", ip, userAgent, deviceId
                ));
                throw new AccountBlockedException("Account is blocked or locked. Please contact support.");
            }

            // Load user with roles - Using custom exception
            user = userRepository.findByIdWithRoles(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: "));

            // Check if user has business role
            boolean hasBusinessRole = roleService.hasBusinessRole(user);

            // Determine if user needs role selection
            boolean needsRoleSelection = !hasBusinessRole && !user.getRoles().isEmpty();

            // Assign guest role if new user (no roles at all)
            boolean isNewUser = user.getRoles().isEmpty();
            if (isNewUser) {
                roleService.assignGuestRole(user);
                user = userRepository.findByIdWithRoles(user.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found after role assignment"));
                log.info("Assigned GUEST role to user: {}", user.getId());
                needsRoleSelection = true;
            }

            // Update login metadata
            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(ip);
            user.setLastLoginUserAgent(userAgent);
            user.resetFailedAttempts();
            userRepository.save(user);

            // Device session management
            UserDevice device = deviceService.getOrCreate(user, deviceId, request);

            // Token generation
            TokenService.TokenPair tokens = tokenService.generate(user, device.getDeviceId(), response);

            // Audit log - SUCCESS
            auditService.log(auditService.success(
                    user, AuditAction.LOGIN.name(),
                    AuditLog.ResourceType.AUTH.name(), user.getId(),
                    ip, userAgent, deviceId
            ));

            log.info("User {} logged in successfully with provider: {}", user.getId(), provider);

            // Build and return response using DTO
            return AuthResponse.fromUser(
                    isNewUser,
                    needsRoleSelection,
                    hasBusinessRole,
                    tokens.accessToken(),
                    tokens.refreshToken(),
                    user
            );

        } catch (AccountBlockedException | ResourceNotFoundException e) {
            // Audit log - FAILURE
            auditService.log(auditService.failure(
                    user, AuditAction.LOGIN_FAILED.name(),
                    e.getMessage(), ip, userAgent, deviceId
            ));
            throw e;
        } catch (Exception e) {
            // Audit log - FAILURE
            auditService.log(auditService.failure(
                    user, AuditAction.LOGIN_FAILED.name(),
                    "Unexpected error: " + e.getMessage(), ip, userAgent, deviceId
            ));
            log.error("Login failed for user {}: {}", user.getId(), e.getMessage());
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }

    // =========================================================
    // 🔄 REFRESH TOKEN FLOW
    // =========================================================
    public AuthResponse refresh(
            String refreshToken,
            String deviceId,
            HttpServletResponse response
    ) {
        // Validate inputs
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token is required");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("Device ID is required");
        }

        try {
            // Validate and rotate token - This throws InvalidTokenException or TokenExpiredException
            RefreshToken rotated = refreshTokenService.rotate(refreshToken, deviceId);
            User user = rotated.getUser();

            // Check if user is active
            if (!user.isActive()) {
                throw new AccountBlockedException("Account is blocked or locked");
            }

            // Generate new tokens
            TokenService.TokenPair tokens = tokenService.generate(user, deviceId, response);

            // Audit log
            auditService.log(auditService.success(
                    user,
                    AuditAction.TOKEN_REFRESH.name(),
                    AuditLog.ResourceType.AUTH.name(),
                    user.getId(),
                    null,
                    null,
                    deviceId
            ));

            log.info("Token refreshed for user: {}", user.getId());

            // Return response using DTO
            return AuthResponse.simple(tokens.accessToken(), tokens.refreshToken(), user.getId());

        } catch (InvalidTokenException | AccountBlockedException e) {
            // Audit failure
            auditService.log(auditService.failureAnonymous(
                    AuditAction.TOKEN_REFRESH_FAILED.name(),
                    e.getMessage(),
                    null,
                    null
            ));
            throw e;
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            auditService.log(auditService.failureAnonymous(
                    AuditAction.TOKEN_REFRESH_FAILED.name(),
                    "Unexpected error: " + e.getMessage(),
                    null,
                    null
            ));
            throw new InvalidTokenException("Failed to refresh token: " + e.getMessage());
        }
    }

    // =========================================================
    // 🚪 LOGOUT (SINGLE DEVICE)
    // =========================================================
    public void logout(
            String refreshToken,
            String deviceId,
            HttpServletResponse response
    ) {
        // Validate inputs
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token is required");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("Device ID is required");
        }

        try {
            // Validate token - This throws InvalidTokenException or TokenExpiredException
            RefreshToken token = refreshTokenService.validate(refreshToken, deviceId);
            User user = token.getUser();

            // Revoke token
            refreshTokenService.revoke(refreshToken, deviceId);

            // Clear cookies
            tokenService.clear(response);

            // Audit log
            auditService.log(auditService.success(
                    user,
                    AuditAction.LOGOUT.name(),
                    AuditLog.ResourceType.AUTH.name(),
                    user.getId(),
                    null,
                    null,
                    deviceId
            ));

            log.info("User {} logged out from device {}", user.getId(), deviceId);

        } catch (InvalidTokenException e) {
            log.warn("Logout failed - invalid token: {}", e.getMessage());
            // Still clear cookies even if token is invalid
            tokenService.clear(response);
            throw e;
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            tokenService.clear(response);
            throw new RuntimeException("Logout failed: " + e.getMessage(), e);
        }
    }

    // =========================================================
    // 🚪 LOGOUT ALL DEVICES
    // =========================================================
    public void logoutAll(User user, HttpServletRequest request) {
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        try {
            // Revoke all user tokens
            refreshTokenService.revokeAll(user);

            // Audit log
            auditService.log(auditService.success(
                    user,
                    AuditAction.LOGOUT_ALL.name(),
                    AuditLog.ResourceType.AUTH.name(),
                    user.getId(),
                    getClientIp(request),
                    request.getHeader("User-Agent")
            ));

            log.info("User {} logged out from all devices", user.getId());

        } catch (Exception e) {
            log.error("Logout all failed for user {}: {}", user.getId(), e.getMessage());
            throw new RuntimeException("Failed to logout from all devices: " + e.getMessage(), e);
        }
    }

    // =========================================================
    // 🔐 VALIDATE USER SESSION (UTILITY METHOD)
    // =========================================================
    public User validateSession(String refreshToken, String deviceId) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token is required");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("Device ID is required");
        }

        RefreshToken token = refreshTokenService.validate(refreshToken, deviceId);
        User user = token.getUser();

        if (!user.isActive()) {
            throw new AccountBlockedException("Account is blocked or locked");
        }

        return user;
    }

    // =========================================================
    // 🌐 IP HELPER
    // =========================================================
    private String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        String ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}