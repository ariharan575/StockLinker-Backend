package com.backend.StockLinker.Message_Service.entity;

import com.backend.StockLinker.Message_Service.enums.ConversationStatus;
import com.backend.StockLinker.Message_Service.enums.MessageType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "conversation", indexes = {
        @Index(name = "buyer_seller_unique_idx", columnList = "buyer_id, seller_id", unique = true),
        @Index(name = "buyer_lastMessageAt_idx", columnList = "buyer_id, last_message_at DESC"),
        @Index(name = "seller_lastMessageAt_idx", columnList = "seller_id, last_message_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "conversation_code", unique = true)
    private String conversationCode;

    @Column(name = "buyer_id")
    private String buyerId;

    @Column(name = "seller_id")
    private String sellerId;

    @Column(name = "buyer_name")
    private String buyerName;

    @Column(name = "seller_name")
    private String sellerName;

    @Column(name = "buyer_business_name")
    private String buyerBusinessName;

    @Column(name = "seller_business_name")
    private String sellerBusinessName;

    @Column(name = "buyer_profile_image")
    private String buyerProfileImage;

    @Column(name = "seller_profile_image")
    private String sellerProfileImage;

    @Column(name = "last_message", columnDefinition = "TEXT")
    private String lastMessage;

    @Column(name = "last_message_sender_id")
    private String lastMessageSenderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_message_type")
    private MessageType lastMessageType;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Builder.Default
    @Column(name = "buyer_unread_count", nullable = false)
    private int buyerUnreadCount = 0;

    @Builder.Default
    @Column(name = "seller_unread_count", nullable = false)
    private int sellerUnreadCount = 0;

    @Builder.Default
    @Column(name = "buyer_archived", nullable = false)
    private boolean buyerArchived = false;

    @Builder.Default
    @Column(name = "seller_archived", nullable = false)
    private boolean sellerArchived = false;

    @Builder.Default
    @Column(name = "buyer_deleted", nullable = false)
    private boolean buyerDeleted = false;

    @Builder.Default
    @Column(name = "seller_deleted", nullable = false)
    private boolean sellerDeleted = false;

    @Builder.Default
    @Column(name = "buyer_blocked", nullable = false)
    private boolean buyerBlocked = false;

    @Builder.Default
    @Column(name = "seller_blocked", nullable = false)
    private boolean sellerBlocked = false;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status")
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public String otherPartyId(String userId) {
        if (buyerId != null && buyerId.equals(userId)) {
            return sellerId;
        }
        if (sellerId != null && sellerId.equals(userId)) {
            return buyerId;
        }
        return null;
    }

    public boolean isParticipant(String userId) {
        return (buyerId != null && buyerId.equals(userId)) ||
                (sellerId != null && sellerId.equals(userId));
    }
}