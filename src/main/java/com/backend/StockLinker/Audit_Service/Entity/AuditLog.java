package com.backend.StockLinker.Audit_Service.Entity;

import com.backend.StockLinker.Audit_Service.Enums.AuditAction;
import com.backend.StockLinker.Audit_Service.Enums.ResourceType;
import com.backend.StockLinker.Auth_Service.model.BaseEntity;
import com.backend.StockLinker.Auth_Service.model.User;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "audit_logs")
@CompoundIndexes({
        @CompoundIndex(name = "idx_audit_resource", def = "{'resource_type': 1, 'resource_id': 1}"),
        // Assuming your BaseEntity has a 'createdAt' field. Adjust the name if it differs.
        @CompoundIndex(name = "idx_audit_created", def = "{'createdAt': -1}")
})
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(exclude = "user")
public class AuditLog extends BaseEntity {

    @DBRef(lazy = true)
    @Indexed(name = "idx_audit_user")
    @Field("user_id")
    private User user;

    @Indexed(name = "idx_audit_action")
    @Field("action")
    private AuditAction action;

    @Field("resource_id")
    private String resourceId;

    @Field("resource_type")
    private ResourceType resourceType;

    @Field("old_value")
    private String oldValue;

    @Field("new_value")
    private String newValue;

    @Field("ip_address")
    private String ipAddress;

    @Field("user_agent")
    private String userAgent;

    @Indexed(name = "idx_audit_status")
    @Field("status")
    private Status status;

    @Field("request_uri")
    private String requestUri;

    @Field("http_method")
    private String httpMethod;

    @Field("response_status")
    private Integer responseStatus;

    @Field("failure_reason")
    private String failureReason;

    @Indexed(name = "idx_audit_device_id")
    @Field("device_id")
    private String deviceId;

    public enum Status {
        SUCCESS, FAILURE
    }
}