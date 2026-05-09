package com.backend.StockLinker.AuthService.service;

import com.backend.StockLinker.AuthService.constant.AuditAction;
import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;
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
import java.util.Map;

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
    public Map<String,Object> login(
            User user,
            String provider,
            String deviceId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            // ✅ ACCOUNT CHECK
            if (!user.isActive()) {
                throw new BaseException(ErrorCode.ACCOUNT_BLOCKED);
            }

            // ✅ LOAD USER WITH ROLES
            user = userRepository.findByIdWithRoles(user.getId())
                    .orElseThrow();

            // ✅ CHECK IF USER HAS ANY BUSINESS ROLE (SHOPKEEPER or WHOLESALER)
            boolean hasBusinessRole = roleService.hasBusinessRole(user);

            // ✅ DETERMINE IF NEW USER (NO ROLES AT ALL)
            boolean isNewUser = user.getRoles().isEmpty();

            // ✅ ASSIGN GUEST ROLE IF NEW USER
            if (isNewUser) {
                roleService.assignGuestRole(user);
                user = userRepository.findByIdWithRoles(user.getId())
                        .orElseThrow();
                log.info("Assigned GUEST role to user: {}", user.getId());
            }

            // ✅ UPDATE LOGIN META
            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(ip);
            user.resetFailedAttempts();
            userRepository.save(user);

            // ✅ DEVICE SESSION
            UserDevice device = deviceService.getOrCreate(user, deviceId, request);

            // ✅ TOKEN GENERATION
            TokenService.TokenPair tokens = tokenService.generate(user, device.getDeviceId(), response);

            // ✅ AUDIT LOG
            auditService.log(auditService.success(
                    user, AuditAction.LOGIN.name(), "AUTH", user.getId(), ip, userAgent
            ));

            // ✅ isNewUser should be TRUE only if user has NO business role AND just got GUEST role
            // But for frontend, we want to show role selection if user doesn't have SHOPKEEPER/WHOLESALER
            boolean needsRoleSelection = !hasBusinessRole;

            return Map.of(
                    "accessToken", tokens.accessToken(),
                    "refreshToken", tokens.refreshToken(),
                    "userId", user.getId(),
                    "isNewUser", needsRoleSelection,
                    "hasBusinessRole", hasBusinessRole,
                    "roles", roleService.getUserRoles(user)
            );

        } catch (Exception e) {
            auditService.log(auditService.failure(
                    user, AuditAction.LOGIN_FAILED.name(), e.getMessage(), ip, userAgent
            ));
            throw e;
        }
    }

    // =========================================================
    // 🔄 REFRESH FLOW (FIXED — DEVICE SAFE)
    // =========================================================
    public Map<String, Object> refresh(
            String refreshToken,
            String deviceId,
            HttpServletResponse response
    ) {

        // VALIDATE + ROTATE TOKEN
        RefreshToken rotated =
                refreshTokenService.rotate(refreshToken, deviceId);

        User user = rotated.getUser();

        if (!user.isActive()) {
            throw new BaseException(ErrorCode.ACCOUNT_BLOCKED);
        }

        String newAccess = tokenService.generate(user, deviceId, response).accessToken();
        String newRefresh = rotated.getToken();

        return Map.of(
                "accessToken", newAccess,
                "refreshToken", newRefresh
        );
    }

    // =========================================================
    // 🚪 LOGOUT (SINGLE DEVICE)
    // =========================================================
    public void logout(
            String refreshToken,
            String deviceId,
            HttpServletResponse response
    ) {

        RefreshToken token =
                refreshTokenService.validate(refreshToken, deviceId);

        User user = token.getUser();

        refreshTokenService.revoke(refreshToken, deviceId);

        tokenService.clear(response);

        auditService.log(
                auditService.success(
                        user,
                        AuditAction.LOGOUT.name(),
                        "AUTH",
                        user.getId(),
                        null,
                        null
                )
        );
    }

    // =========================================================
    // 🚪 LOGOUT ALL DEVICES
    // =========================================================
    public void logoutAll(User user) {

        refreshTokenService.revokeAll(user);

        auditService.log(
                auditService.success(
                        user,
                        "LOGOUT_ALL",
                        "AUTH",
                        user.getId(),
                        null,
                        null
                )
        );
    }


    // =========================================================
    // 🌐 IP HELPER
    // =========================================================
    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return xf != null ? xf.split(",")[0] : request.getRemoteAddr();
    }
}