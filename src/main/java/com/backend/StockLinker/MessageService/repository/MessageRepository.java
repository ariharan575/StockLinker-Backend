package com.backend.StockLinker.MessageService.repository;

import com.backend.StockLinker.MessageService.entity.Message;
import com.backend.StockLinker.MessageService.enums.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {

    Page<Message> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    List<Message> findByConversationIdAndReceiverIdAndStatusNot(
            String conversationId, String receiverId, MessageStatus status);

    @Query("{ 'conversationId': ?0, 'receiverId': ?1, 'status': { '$ne': 'READ' } }")
    List<Message> findUnreadForUserInConversation(String conversationId, String receiverId);

    long countByConversationIdAndReceiverIdAndStatusNot(
            String conversationId, String receiverId, MessageStatus status);

    long countByReceiverIdAndStatusNot(String receiverId, MessageStatus status);
}