package com.backend.StockLinker.Auth_Service.service;

import com.backend.StockLinker.Audit_Service.Services.AuditService;
import com.backend.StockLinker.Audit_Service.Dto.AuditLogRequest;
import com.backend.StockLinker.Auth_Service.dto.response.AuthResponse;
import com.backend.StockLinker.Auth_Service.enums.AccountStatus;
import com.backend.StockLinker.Audit_Service.Enums.AuditAction;
import com.backend.StockLinker.Audit_Service.Enums.ResourceType;
import com.backend.StockLinker.Exception.BaseException;
import com.backend.StockLinker.Exception.ErrorCode;
import com.backend.StockLinker.Exception.customExceptions.ResourceNotFoundException;
import com.backend.StockLinker.Audit_Service.Entity.AuditLog;
import com.backend.StockLinker.Auth_Service.model.User;
import com.backend.StockLinker.Auth_Service.model.UserDevice;
import com.backend.StockLinker.Auth_Service.repository.UserRepository;
import com.backend.StockLinker.Security.TokenService;
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
                new ResourceNotFoundException("User not found"));

        if (newUser.getAccountStatus().equals(AccountStatus.BLOCKED)) {
            auditFailure(newUser.getId(), AuditAction.LOGIN_FAILED, "Account blocked/locked", ip, userAgent, deviceId);
            throw new BaseException(ErrorCode.ACCOUNT_BLOCKED, "Account is blocked or locked.");
        }

        newUser.setLastLoginAt(LocalDateTime.now());
        newUser.setLastLoginIp(ip);
        newUser.setLastLoginUserAgent(userAgent);
        userRepository.save(newUser);

        UserDevice device = deviceSessionService.getOrCreate(newUser, deviceId, request);

        tokenService.generate(newUser, device.getDeviceId(), response);

        auditSuccess(newUser.getId(), AuditAction.LOGIN, "Provider: " + provider, ip, userAgent, deviceId);
        log.info("User {} authenticated successfully via {}", newUser.getId(), provider);

        return buildResponse(newUser);
    }

    private AuthResponse buildResponse(User user) {
        String roleName = user.getRole() != null ? user.getRole().getName() : null;

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