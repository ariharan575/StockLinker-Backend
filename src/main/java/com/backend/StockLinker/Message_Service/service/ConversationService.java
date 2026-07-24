package com.backend.StockLinker.Message_Service.service;

import com.backend.StockLinker.Message_Service.dto.request.ConversationSearchRequest;
import com.backend.StockLinker.Message_Service.dto.request.CreateConversationRequest;
import com.backend.StockLinker.Message_Service.dto.response.ConversationListResponse;
import com.backend.StockLinker.Message_Service.dto.response.ConversationResponse;

public interface ConversationService {

    ConversationResponse createOrGetConversation(CreateConversationRequest request);

    ConversationResponse getConversationById(String conversationId);

    ConversationListResponse listConversations(ConversationSearchRequest request);
}