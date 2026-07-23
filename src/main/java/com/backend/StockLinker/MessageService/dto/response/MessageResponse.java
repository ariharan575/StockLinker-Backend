package com.backend.StockLinker.MessageService.dto.response;

import com.backend.StockLinker.MessageService.enums.MessageStatus;
import com.backend.StockLinker.MessageService.enums.MessageType;
import com.backend.StockLinker.MessageService.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private String id;
    private String conversationId;
    private String senderId;
    private String receiverId;
    private UserRole senderRole;
    private UserRole receiverRole;
    private String message;
    private MessageType messageType;
    private MessageStatus status;
    private boolean edited;
    private Instant editedAt;
    private boolean deleted;
    private Instant sentAt;
    private Instant deliveredAt;
    private Instant readAt;
    private Instant createdAt;

    /**
     * True if the current requesting user is the sender of this message.
     * Populated by the service layer, not by the mapper, since it depends on the viewer.
     */
    private boolean mine;
}