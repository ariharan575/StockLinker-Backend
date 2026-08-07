package com.backend.StockLinker.Audit_Service.Entity;

import com.backend.StockLinker.Audit_Service.Enums.AuditAction;
import com.backend.StockLinker.Audit_Service.Enums.ResourceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_resource", columnList = "resource_type, resource_id"),
        @Index(name = "idx_audit_created", columnList = "created_at DESC"),
        @Index(name = "idx_audit_user", columnList = "user_id"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_status", columnList = "status"),
        @Index(name = "idx_audit_device_id", columnList = "device_id")
})
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id")
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action")
    private AuditAction action;

    @Column(name = "resource_id")
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type")
    private ResourceType resourceType;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @Column(name = "request_uri")
    private String requestUri;

    @Column(name = "http_method")
    private String httpMethod;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "device_id")
    private String deviceId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status {
        SUCCESS, FAILURE
    }
}