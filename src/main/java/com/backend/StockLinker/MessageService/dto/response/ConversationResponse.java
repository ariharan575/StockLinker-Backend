package com.backend.StockLinker.MessageService.dto.response;

import com.backend.StockLinker.MessageService.enums.ConversationStatus;
import com.backend.StockLinker.MessageService.enums.MessageType;
import com.backend.StockLinker.MessageService.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Conversation as seen from the current viewer's perspective —
 * "counterpart*" fields are always the OTHER party, regardless of whether
 * the viewer is the buyer or the seller. This is what the frontend Messenger.jsx
 * conversation list/header actually consumes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private String id;
    private String conversationCode;

    private String counterpartId;
    private String counterpartName;
    private String counterpartBusinessName;
    private String counterpartProfileImage;
    private UserRole counterpartRole;

    private UserRole myRole;

    private String lastMessage;
    private String lastMessageSenderId;
    private MessageType lastMessageType;
    private Instant lastMessageAt;

    private int unreadCount;
    private boolean archived;
    private boolean blocked;
    private boolean counterpartBlocked;
    private boolean active;
    private ConversationStatus status;

    private Instant createdAt;
    private Instant updatedAt;
}