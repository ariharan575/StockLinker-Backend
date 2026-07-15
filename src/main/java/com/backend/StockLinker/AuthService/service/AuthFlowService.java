package com.backend.StockLinker.AuthService.service;

import com.backend.StockLinker.AuthService.dto.request.AuditLogRequest;
import com.backend.StockLinker.AuthService.dto.response.AuthResponse;
import com.backend.StockLinker.AuthService.enums.AccountStatus;
import com.backend.StockLinker.AuthService.enums.AuditAction;
import com.backend.StockLinker.AuthService.enums.ResourceType;
import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;
import com.backend.StockLinker.AuthService.model.AuditLog;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.model.UserDevice;
import com.backend.StockLinker.AuthService.repository.UserRepository;
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
public class AuthFlowService {

    private final UserRepository userRepository;
    private final DeviceSessionService deviceSessionService;
    private final TokenService tokenService;
    private final AuditService auditService;
    private final IpAddressService ipAddressService;

    @Transactional
    public AuthResponse processLogin(User user, String provider, String deviceId,
                                     HttpServletRequest request, HttpServletResponse response) {

        String ip = ipAddressService.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        User newUser = userRepository.findById(user.getId()).orElseThrow(()->
                new BaseException(ErrorCode.USER_NOT_FOUND,"user not found"));

        // 1. Validate Account Status
        if (newUser.getAccountStatus().equals(AccountStatus.BLOCKED)) {
            auditFailure(newUser.getId(), AuditAction.LOGIN_FAILED, "Account blocked/locked", ip, userAgent, deviceId);
            throw new BaseException(ErrorCode.ACCOUNT_BLOCKED, "Account is blocked or locked.");
        }

        // 2. Update Last Login Metadata
        newUser.setLastLoginAt(LocalDateTime.now());
        newUser.setLastLoginIp(ip);
        newUser.setLastLoginUserAgent(userAgent);
        userRepository.save(newUser);

        // 3. Create or Update Device Session
        UserDevice device = deviceSessionService.getOrCreate(newUser, deviceId, request);

        // 4. Generate JWT Access & Refresh Tokens
        tokenService.generate(newUser, device.getDeviceId(), response);

        // 5. Audit Success
        auditSuccess(newUser.getId(), AuditAction.LOGIN, "Provider: " + provider, ip, userAgent, deviceId);
        log.info("User {} authenticated successfully via {}", newUser.getId(), provider);

        // 6. Build and Return AuthResponse
        return buildResponse(newUser, provider);
    }

    private AuthResponse buildResponse(User user, String provider) {

        String roleName = user.getRole() != null ? user.getRole().getName() : (provider.equals("GUEST") ? "GUEST" : null);

        return AuthResponse.builder()
                .userId(user.getId())
                .role(roleName)
                .accountStatus(user.getAccountStatus())
                .build();
    }

    private void auditSuccess(String userId, AuditAction action, String details, String ip, String userAgent, String deviceId) {
        auditService.log(AuditLogRequest.builder()
                .userId(userId)
                .action(action)
                .resourceType(ResourceType.AUTH)
                .resourceId(userId)
                .ipAddress(ip)
                .userAgent(userAgent)
                .deviceId(deviceId)
                .status(AuditLog.Status.SUCCESS)
                .newValue(details)
                .build());
    }

    private void auditFailure(String userId, AuditAction action, String reason, String ip, String userAgent, String deviceId) {
        auditService.log(AuditLogRequest.builder()
                .userId(userId != null ? userId : "UNKNOWN")
                .action(action)
                .resourceType(ResourceType.AUTH)
                .resourceId(userId)
                .ipAddress(ip)
                .userAgent(userAgent)
                .deviceId(deviceId)
                .status(AuditLog.Status.FAILURE)
                .failureReason(reason)
                .build());
    }
}