package com.backend.StockLinker.MessageService.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Marks all unread messages in a conversation, up to and including this message,
 * as read for the current user. If messageId is null, marks the entire conversation read.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadMessageRequest {

    private String messageId;
}