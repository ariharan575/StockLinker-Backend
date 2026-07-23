package com.backend.StockLinker.MessageService.service;

import com.backend.StockLinker.MessageService.dto.request.EditMessageRequest;
import com.backend.StockLinker.MessageService.dto.request.ReadMessageRequest;
import com.backend.StockLinker.MessageService.dto.request.SendMessageRequest;
import com.backend.StockLinker.MessageService.dto.response.MessageResponse;
import com.backend.StockLinker.MessageService.dto.response.PagedMessageResponse;
import com.backend.StockLinker.MessageService.dto.response.UnreadCountResponse;
import org.springframework.data.domain.Pageable;

public interface MessageService {

    MessageResponse sendMessage(SendMessageRequest request);

    PagedMessageResponse getMessages(String conversationId, Pageable pageable);

    MessageResponse markAsRead(String conversationId, ReadMessageRequest request);

    MessageResponse markAsDelivered(String messageId);

    MessageResponse editMessage(String messageId, EditMessageRequest request);

    void deleteMessage(String messageId);

    UnreadCountResponse getUnreadCount();
}