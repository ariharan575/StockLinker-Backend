package com.backend.StockLinker.Audit_Service.Services;

import com.backend.StockLinker.Audit_Service.Dto.AuditLogRequest;
import com.backend.StockLinker.Audit_Service.Entity.AuditLog;
import com.backend.StockLinker.Audit_Service.Repository.AuditLogRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class AuditService {

    private final AuditLogRepository repository;

    @Async
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
            String userId = (request.getUserId() != null)
                    ? request.getUserId()
                    : "anonymous";

            log.error(
                    "Audit log persistence failed: action={}, userId={}, resourceType={}, resourceId={}, status={}, error={}",
                    (request.getAction() != null ? request.getAction() : "UNKNOWN"),
                    userId,
                    (request.getResourceType() != null ? request.getResourceType() : "UNKNOWN"),
                    (request.getResourceId() != null ? request.getResourceId() : "UNKNOWN"),
                    (request.getStatus() != null ? request.getStatus() : "UNKNOWN"),
                    e.getMessage(),
                    e
            );
        }
    }

    private AuditLog buildAuditLog(AuditLogRequest request) {
        return AuditLog.builder()
                .userId(request.getUserId()) // Mapping directly as a String
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
                .createdAt(LocalDateTime.now()) // Set manually for Mongo
                .updatedAt(LocalDateTime.now()) // Set manually for Mongo
                .build();
    }
}