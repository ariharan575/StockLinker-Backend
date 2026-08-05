package com.backend.StockLinker.Notification_Service.entity;

import com.backend.StockLinker.Auth_Service.model.BaseEntity;
import com.backend.StockLinker.Notification_Service.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_recipient", columnList = "recipient_id"),
        @Index(name = "idx_notif_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Notification extends BaseEntity {

    @Column(name = "recipient_id", nullable = false)
    private String recipientId;

    @Column(name = "sender_name")
    private String senderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;
}