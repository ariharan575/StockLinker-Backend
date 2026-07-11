package com.backend.StockLinker.AuthService.service;

import com.backend.StockLinker.AuthService.enums.AccountStatus;
import com.backend.StockLinker.AuthService.enums.LoginProvider;
import com.backend.StockLinker.AuthService.exception.customExceptions.InvalidTokenException;
import com.backend.StockLinker.AuthService.exception.customExceptions.ResourceNotFoundException;
import com.backend.StockLinker.AuthService.dto.response.AuthResponse;
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

    // =========================================================
    // ✅ MAIN LOGIN FLOW (SINGLE ENTERPRISE PIPELINE)
    // =========================================================
    public AuthResponse login(
            User user,
            LoginProvider provider,
            String deviceId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            if (deviceId == null || deviceId.isBlank()) {
                throw new IllegalArgumentException("Device ID is required");
            }

            user = userRepository.findByIdWithRoles(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: "));

            // 2. Update Login Metadata
            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(ip);
            user.setLastLoginUserAgent(userAgent);
            userRepository.save(user);

            // 3. Device Session Management
            UserDevice device = deviceService.getOrCreate(user, deviceId, request);

            // 4. Token Generation
            TokenService.TokenPair tokens = tokenService.generate(user, device.getDeviceId(), response);

            // 5. Asynchronous Audit Logging
//            auditService.log(auditService.success(
//                    user, AuditAction.LOGIN,
//                    ResourceType.AUTH,
//                    ip, userAgent, deviceId
//            ));

            log.info("User {} logged in successfully with provider: {}", user.getId(), provider);

            // 6. Return standard Enterprise AuthResponse
            return buildAuthResponse(tokens, user);

        } catch ( ResourceNotFoundException e) {
//            auditService.log(auditService.failure(
//                    user, AuditAction.LOGIN_FAILED,
//                    e.getMessage(), ip, userAgent, deviceId
//            ));
            throw e;
        } catch (Exception e) {
//            auditService.log(auditService.failure(
//                    user, AuditAction.LOGIN_FAILED,
//                    "Unexpected error: " + e.getMessage(), ip, userAgent, deviceId
//            ));
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
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token is required");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("Device ID is required");
        }

        try {
            RefreshToken rotated = refreshTokenService.rotate(refreshToken, deviceId);
            User user = rotated.getUser();

            TokenService.TokenPair tokens = tokenService.generate(user, deviceId, response);

//            auditService.log(auditService.success(
//                    user, AuditAction.TOKEN_REFRESH,
//                    ResourceType.AUTH, null, null, deviceId
//            ));

            log.info("Token refreshed for user: {}", user.getId());

            return buildAuthResponse(tokens, user);

        } catch (InvalidTokenException  e) {
//            auditService.log(auditService.failureAnonymous(
//                    AuditAction.TOKEN_REFRESH_FAILED,
//                    e.getMessage(), null, null
//            ));
            throw e;
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
//            auditService.log(auditService.failureAnonymous(
//                    AuditAction.TOKEN_REFRESH_FAILED,
//                    "Unexpected error: " + e.getMessage(), null, null
//            ));
            throw new InvalidTokenException("Failed to refresh token: " + e.getMessage());
        }
    }

    // =========================================================
    // 🚪 LOGOUT (SINGLE DEVICE)
    // =========================================================
    public void logout(String refreshToken, String deviceId, HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token is required");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("Device ID is required");
        }

        try {
            RefreshToken token = refreshTokenService.validate(refreshToken, deviceId);
            User user = token.getUser();

            refreshTokenService.revoke(refreshToken, deviceId);
            tokenService.clear(response);

//            auditService.log(auditService.success(
//                    user, AuditAction.LOGOUT,
//                    ResourceType.AUTH, null, null, deviceId
//            ));

            log.info("User {} logged out from device {}", user.getId(), deviceId);

        } catch (InvalidTokenException e) {
            log.warn("Logout failed - invalid token: {}", e.getMessage());
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
            refreshTokenService.revokeAll(user);

//            auditService.log(auditService.success(
//                    user, AuditAction.LOGOUT_ALL,
//                    ResourceType.AUTH, user.getId(),
//                    getClientIp(request),
//                    request.getHeader("User-Agent")
//            ));

            log.info("User {} logged out from all devices", user.getId());

        } catch (Exception e) {
            log.error("Logout all failed for user {}: {}", user.getId(), e.getMessage());
            throw new RuntimeException("Failed to logout from all devices: " + e.getMessage(), e);
        }
    }

    // =========================================================
    // 🔐 VALIDATE USER SESSION
    // =========================================================
    @Transactional(readOnly = true)
    public User validateSession(String refreshToken, String deviceId) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token is required");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("Device ID is required");
        }

        RefreshToken token = refreshTokenService.validate(refreshToken, deviceId);
        return token.getUser();
    }

    // =========================================================
    // 🧠 HELPER: DETERMINE FRONTEND ROUTING
    // =========================================================
    private String determineNextStep(User user) {
        if ("GUEST".equalsIgnoreCase(user.getRole())) return "GUEST_DASHBOARD";
        if (AccountStatus.PENDING_ROLE.equals(user.getAccountStatus())) return "ROLE_SELECTION";
        if (AccountStatus.PENDING_ONBOARDING.equals(user.getAccountStatus())) return "ONBOARDING";
        return "DASHBOARD";
    }

    private AuthResponse buildAuthResponse(TokenService.TokenPair tokens, User user) {
        // Assume AuthResponse has a builder or a full constructor setup for these fields
        return AuthResponse.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .userId(user.getId())
                .role(user.getRole())
                .accountStatus(user.getAccountStatus())
                .nextStep(determineNextStep(user))
                .build();
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