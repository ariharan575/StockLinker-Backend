package com.backend.StockLinker.MessageService.entity;

import com.backend.StockLinker.MessageService.enums.MessageStatus;
import com.backend.StockLinker.MessageService.enums.MessageType;
import com.backend.StockLinker.MessageService.enums.UserRole;
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
 * A single text message belonging to a Conversation.
 * Soft-deleted only — the deleted flag hides content, the document is never removed.
 */
@Document(collection = "message")
@CompoundIndexes({
        @CompoundIndex(name = "conversation_createdAt_idx", def = "{'conversationId': 1, 'createdAt': -1}"),
        @CompoundIndex(name = "receiver_status_idx", def = "{'receiverId': 1, 'status': 1}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    private String id;

    @Field("conversationId")
    @Indexed
    private String conversationId;

    @Field("senderId")
    private String senderId;

    @Field("receiverId")
    @Indexed
    private String receiverId;

    @Field("senderRole")
    private UserRole senderRole;

    @Field("receiverRole")
    private UserRole receiverRole;

    @Field("message")
    private String message;

    @Field("messageType")
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Field("status")
    @Indexed
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    @Field("edited")
    @Builder.Default
    private boolean edited = false;

    @Field("editedAt")
    private Instant editedAt;

    @Field("deleted")
    @Builder.Default
    private boolean deleted = false;

    @Field("deletedAt")
    private Instant deletedAt;

    @Field("sentAt")
    private Instant sentAt;

    @Field("deliveredAt")
    private Instant deliveredAt;

    @Field("readAt")
    private Instant readAt;

    @CreatedDate
    @Field("createdAt")
    @Indexed
    private Instant createdAt;

    @LastModifiedDate
    @Field("updatedAt")
    private Instant updatedAt;
}