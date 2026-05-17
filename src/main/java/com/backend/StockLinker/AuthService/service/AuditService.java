package com.backend.StockLinker.AuthService.service;

import com.backend.StockLinker.AuthService.model.AuditLog;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository repository;

    // =========================================================
    // 📝 SAVE LOG (ASYNC - NON BLOCKING)
    // =========================================================
    @Async
    @Transactional
    public void log(AuditLog logData) {
        try {
            repository.save(logData);
            log.debug("Audit log saved: {} for user: {}",
                    logData.getAction(),
                    logData.getUser() != null ? logData.getUser().getId() : "anonymous");
        } catch (Exception e) {
            // Never break main flow because of audit failure
            log.error("Audit log failed: {}", e.getMessage());
        }
    }

    // =========================================================
    // ✅ SUCCESS LOG BUILDER
    // =========================================================
    public AuditLog success(User user, String action, String resourceType,
                            String resourceId, String ip, String userAgent) {
        return AuditLog.builder()
                .user(user)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ipAddress(ip)
                .userAgent(userAgent)
                .status(AuditLog.Status.SUCCESS)
                .build();
    }

    // =========================================================
    // ✅ SUCCESS LOG WITH DEVICE
    // =========================================================
    public AuditLog success(User user, String action, String resourceType,
                            String resourceId, String ip, String userAgent, String deviceId) {
        return AuditLog.builder()
                .user(user)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ipAddress(ip)
                .userAgent(userAgent)
                .deviceId(deviceId)
                .status(AuditLog.Status.SUCCESS)
                .build();
    }

    // =========================================================
    // ✅ SUCCESS LOG WITH VALUE CHANGES
    // =========================================================
    public AuditLog successWithChanges(User user, String action, String resourceType,
                                       String resourceId, String oldValue, String newValue,
                                       String ip, String userAgent) {
        return AuditLog.builder()
                .user(user)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ip)
                .userAgent(userAgent)
                .status(AuditLog.Status.SUCCESS)
                .build();
    }

    // =========================================================
    // ❌ FAILURE LOG BUILDER
    // =========================================================
    public AuditLog failure(User user, String action, String reason, String ip, String userAgent) {
        return AuditLog.builder()
                .user(user)
                .action(action)
                .failureReason(reason)
                .ipAddress(ip)
                .userAgent(userAgent)
                .status(AuditLog.Status.FAILURE)
                .build();
    }

    // =========================================================
    // ❌ FAILURE LOG WITH DEVICE
    // =========================================================
    public AuditLog failure(User user, String action, String reason,
                            String ip, String userAgent, String deviceId) {
        return AuditLog.builder()
                .user(user)
                .action(action)
                .failureReason(reason)
                .ipAddress(ip)
                .userAgent(userAgent)
                .deviceId(deviceId)
                .status(AuditLog.Status.FAILURE)
                .build();
    }

    // =========================================================
    // ❌ FAILURE LOG FOR NON-EXISTENT USER
    // =========================================================
    public AuditLog failureAnonymous(String action, String reason, String ip, String userAgent) {
        return AuditLog.builder()
                .user(null)
                .action(action)
                .failureReason(reason)
                .ipAddress(ip)
                .userAgent(userAgent)
                .status(AuditLog.Status.FAILURE)
                .build();
    }
}