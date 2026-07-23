package com.backend.StockLinker.MessageService.dto.response;

import com.backend.StockLinker.MessageService.enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight real-time event broadcast over a conversation's WebSocket topic
 * when delivery/read status changes — cheaper than re-sending full MessageResponse
 * payloads for every status transition. userId is whoever triggered the change
 * (the receiver marking something delivered/read).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatusEvent {
    private String conversationId;
    private String messageId;
    private String userId;
    private MessageStatus status;
}