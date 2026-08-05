package com.backend.StockLinker.Message_Service.service;

import com.backend.StockLinker.Message_Service.dto.request.EditMessageRequest;
import com.backend.StockLinker.Message_Service.dto.request.ReadMessageRequest;
import com.backend.StockLinker.Message_Service.dto.request.SendMessageRequest;
import com.backend.StockLinker.Message_Service.dto.response.MessageResponse;
import com.backend.StockLinker.Message_Service.dto.response.PagedMessageResponse;
import com.backend.StockLinker.Message_Service.dto.response.UnreadCountResponse;
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