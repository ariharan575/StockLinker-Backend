package com.backend.StockLinker.MessageService.entity;

import com.backend.StockLinker.MessageService.enums.ConversationStatus;
import com.backend.StockLinker.MessageService.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

/**
 * A one-to-one Buyer <-> Seller conversation thread.
 * One Conversation document exists per unique (buyerId, sellerId) pair.
 */
@Document(collection = "conversation")
@CompoundIndexes({
        @CompoundIndex(name = "buyer_seller_unique_idx", def = "{'buyerId': 1, 'sellerId': 1}", unique = true),
        @CompoundIndex(name = "buyer_lastMessageAt_idx", def = "{'buyerId': 1, 'lastMessageAt': -1}"),
        @CompoundIndex(name = "seller_lastMessageAt_idx", def = "{'sellerId': 1, 'lastMessageAt': -1}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    private String id;

    @Field("conversationCode")
    @Indexed(unique = true)
    private String conversationCode;

    @Field("buyerId")
    @Indexed
    private String buyerId;

    @Field("sellerId")
    @Indexed
    private String sellerId;

    @Field("buyerName")
    private String buyerName;

    @Field("sellerName")
    private String sellerName;

    @Field("buyerBusinessName")
    private String buyerBusinessName;

    @Field("sellerBusinessName")
    private String sellerBusinessName;

    @Field("buyerProfileImage")
    private String buyerProfileImage;

    @Field("sellerProfileImage")
    private String sellerProfileImage;

    @Field("lastMessage")
    private String lastMessage;

    @Field("lastMessageSenderId")
    private String lastMessageSenderId;

    @Field("lastMessageType")
    private MessageType lastMessageType;

    @Field("lastMessageAt")
    @Indexed
    private Instant lastMessageAt;

    @Field("buyerUnreadCount")
    @Builder.Default
    private int buyerUnreadCount = 0;

    @Field("sellerUnreadCount")
    @Builder.Default
    private int sellerUnreadCount = 0;

    @Field("buyerArchived")
    @Builder.Default
    private boolean buyerArchived = false;

    @Field("sellerArchived")
    @Builder.Default
    private boolean sellerArchived = false;

    @Field("buyerDeleted")
    @Builder.Default
    private boolean buyerDeleted = false;

    @Field("sellerDeleted")
    @Builder.Default
    private boolean sellerDeleted = false;

    @Field("buyerBlocked")
    @Builder.Default
    private boolean buyerBlocked = false;

    @Field("sellerBlocked")
    @Builder.Default
    private boolean sellerBlocked = false;

    @Field("active")
    @Builder.Default
    private boolean active = true;

    @Field("status")
    @Builder.Default
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @CreatedDate
    @Field("createdAt")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updatedAt")
    private Instant updatedAt;

    /**
     * Returns the counterpart user id for a given user id in this conversation.
     */
    public String otherPartyId(String userId) {
        if (buyerId.equals(userId)) {
            return sellerId;
        }
        if (sellerId.equals(userId)) {
            return buyerId;
        }
        return null;
    }

    public boolean isParticipant(String userId) {
        return buyerId.equals(userId) || sellerId.equals(userId);
    }
}
