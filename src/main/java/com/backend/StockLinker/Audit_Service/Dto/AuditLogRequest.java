package com.backend.StockLinker.Audit_Service.Dto;

import com.backend.StockLinker.Audit_Service.Enums.AuditAction;
import com.backend.StockLinker.Audit_Service.Enums.ResourceType;
import com.backend.StockLinker.Audit_Service.Entity.AuditLog;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogRequest {

    @NotBlank(message = "User ID is required for audit logging")
    private String userId;

    @NotNull(message = "Audit action is required")
    private AuditAction action;

    @NotNull(message = "Resource type is required")
    private ResourceType resourceType;

    private String resourceId;

    private String oldValue;

    private String newValue;

    private String ipAddress;

    private String userAgent;

    private String deviceId;

    private String requestUri;

    private String httpMethod;

    private Integer responseStatus;

    @NotNull(message = "Status is required")
    private AuditLog.Status status;

    private String failureReason;
}