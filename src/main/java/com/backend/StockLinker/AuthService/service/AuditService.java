package com.backend.StockLinker.AuthService.service;

import com.backend.StockLinker.AuthService.dto.request.AuditLogRequest;
import com.backend.StockLinker.AuthService.model.AuditLog;
import com.backend.StockLinker.AuthService.model.User;
import com.backend.StockLinker.AuthService.repository.AuditLogRepository;
import com.backend.StockLinker.AuthService.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class AuditService {

    private final AuditLogRepository repository;

    private final UserRepository userRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(@Valid AuditLogRequest request) {

        if (request == null) {
            log.error("Audit logging failed: AuditLogRequest is null");
            return;
        }

        try {
            AuditLog auditLog = buildAuditLog(request);
            repository.save(auditLog);

            if (log.isDebugEnabled()) {
                log.debug(
                        "Audit log saved successfully: action={}, userId={}, resourceType={}, status={}",
                        request.getAction(),
                        request.getUserId(),
                        request.getResourceType(),
                        request.getStatus()
                );
            }
        } catch (Exception e) {
            String userId = (request != null && request.getUserId() != null)
                    ? request.getUserId()
                    : "anonymous";

            log.error(
                    "Audit log persistence failed: action={}, userId={}, resourceType={}, resourceId={}, status={}, error={}",
                    (request != null ? request.getAction() : "UNKNOWN"),
                    userId,
                    (request != null ? request.getResourceType() : "UNKNOWN"),
                    (request != null ? request.getResourceId() : "UNKNOWN"),
                    (request != null ? request.getStatus() : "UNKNOWN"),
                    e.getMessage(),
                    e
            );
        }
    }

    private AuditLog buildAuditLog(AuditLogRequest request) {

        // getReferenceById creates a lazy proxy without hitting the database
        User user = userRepository.findById(request.getUserId()).orElse(null);

        return AuditLog.builder()
                .user(user)
                .action(request.getAction())
                .resourceType(request.getResourceType())
                .resourceId(request.getResourceId())
                .oldValue(request.getOldValue())
                .newValue(request.getNewValue())
                .ipAddress(request.getIpAddress())
                .userAgent(request.getUserAgent())
                .deviceId(request.getDeviceId())
                .requestUri(request.getRequestUri())
                .httpMethod(request.getHttpMethod())
                .responseStatus(request.getResponseStatus())
                .status(request.getStatus())
                .failureReason(request.getFailureReason())
                .build();
    }
}