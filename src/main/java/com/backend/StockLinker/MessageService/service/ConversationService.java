package com.backend.StockLinker.MessageService.service;

import com.backend.StockLinker.MessageService.dto.request.ConversationSearchRequest;
import com.backend.StockLinker.MessageService.dto.request.CreateConversationRequest;
import com.backend.StockLinker.MessageService.dto.response.ConversationListResponse;
import com.backend.StockLinker.MessageService.dto.response.ConversationResponse;

public interface ConversationService {

    ConversationResponse createOrGetConversation(CreateConversationRequest request);

    ConversationResponse getConversationById(String conversationId);

    ConversationListResponse listConversations(ConversationSearchRequest request);
}