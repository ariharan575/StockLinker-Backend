package com.backend.StockLinker.Message_Service.repository;

import com.backend.StockLinker.Message_Service.entity.Message;
import com.backend.StockLinker.Message_Service.enums.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, String> {

    Page<Message> findByConversationIdOrderByCreatedAtDesc(String conversationId, Pageable pageable);

    List<Message> findByConversationIdAndReceiverIdAndStatusNot(
            String conversationId, String receiverId, MessageStatus status);

    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId AND m.receiverId = :receiverId AND m.status != com.backend.StockLinker.Message_Service.enums.MessageStatus.READ")
    List<Message> findUnreadForUserInConversation(@Param("conversationId") String conversationId, @Param("receiverId") String receiverId);

    long countByConversationIdAndReceiverIdAndStatusNot(
            String conversationId, String receiverId, MessageStatus status);

    long countByReceiverIdAndStatusNot(String receiverId, MessageStatus status);
}