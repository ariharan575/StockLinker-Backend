package com.backend.StockLinker.Message_Service.entity;

import com.backend.StockLinker.Message_Service.enums.MessageStatus;
import com.backend.StockLinker.Message_Service.enums.MessageType;
import com.backend.StockLinker.Message_Service.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "message", indexes = {
        @Index(name = "conversation_createdAt_idx", columnList = "conversation_id, created_at DESC"),
        @Index(name = "receiver_status_idx", columnList = "receiver_id, status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "conversation_id")
    private String conversationId;

    @Column(name = "sender_id")
    private String senderId;

    @Column(name = "receiver_id")
    private String receiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_role")
    private UserRole senderRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "receiver_role")
    private UserRole receiverRole;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "message_type")
    private MessageType messageType = MessageType.TEXT;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status")
    private MessageStatus status = MessageStatus.SENT;

    @Builder.Default
    @Column(name = "edited", nullable = false)
    private boolean edited = false;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}