package com.backend.StockLinker.MessageService.service.impl;

import com.backend.StockLinker.MessageService.dto.request.EditMessageRequest;
import com.backend.StockLinker.MessageService.dto.request.ReadMessageRequest;
import com.backend.StockLinker.MessageService.dto.request.SendMessageRequest;
import com.backend.StockLinker.MessageService.dto.response.MessageResponse;
import com.backend.StockLinker.MessageService.dto.response.MessageStatusEvent;
import com.backend.StockLinker.MessageService.dto.response.PagedMessageResponse;
import com.backend.StockLinker.MessageService.dto.response.UnreadCountResponse;
import com.backend.StockLinker.MessageService.entity.Conversation;
import com.backend.StockLinker.MessageService.entity.Message;
import com.backend.StockLinker.MessageService.enums.MessageStatus;
import com.backend.StockLinker.MessageService.enums.MessageType;
import com.backend.StockLinker.MessageService.enums.UserRole;
import com.backend.StockLinker.MessageService.mapper.MessageMapper;
import com.backend.StockLinker.MessageService.repository.ConversationRepository;
import com.backend.StockLinker.MessageService.repository.MessageRepository;
import com.backend.StockLinker.MessageService.security.CurrentUserProvider;
import com.backend.StockLinker.MessageService.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageServiceImpl implements MessageService {

    private static final int MAX_MESSAGE_LENGTH = 4000;

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final MessageMapper messageMapper;
    private final CurrentUserProvider currentUserProvider;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public MessageResponse sendMessage(SendMessageRequest request) {
        String currentUserId = currentUserProvider.getCurrentUserId();

        String trimmed = request.getMessage() == null ? "" : request.getMessage().trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        if (trimmed.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message cannot exceed " + MAX_MESSAGE_LENGTH + " characters");
        }

        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + request.getConversationId()));

        if (!conversation.isParticipant(currentUserId)) {
            throw new IllegalStateException("You are not a participant in this conversation");
        }

        boolean senderIsBuyer = conversation.getBuyerId().equals(currentUserId);
        String receiverId = conversation.otherPartyId(currentUserId);

        if (senderIsBuyer && conversation.isBuyerBlocked()) {
            throw new IllegalStateException("You have blocked this user; unblock to send messages");
        }
        if (senderIsBuyer && conversation.isSellerBlocked()) {
            throw new IllegalStateException("You have been blocked in this conversation");
        }
        if (!senderIsBuyer && conversation.isSellerBlocked()) {
            throw new IllegalStateException("You have blocked this user; unblock to send messages");
        }
        if (!senderIsBuyer && conversation.isBuyerBlocked()) {
            throw new IllegalStateException("You have been blocked in this conversation");
        }

        UserRole senderRole = senderIsBuyer ? UserRole.BUYER : UserRole.SELLER;
        UserRole receiverRole = senderIsBuyer ? UserRole.SELLER : UserRole.BUYER;

        Instant now = Instant.now();

        Message message = Message.builder()
                .conversationId(conversation.getId())
                .senderId(currentUserId)
                .receiverId(receiverId)
                .senderRole(senderRole)
                .receiverRole(receiverRole)
                .message(trimmed)
                .messageType(MessageType.TEXT)
                .status(MessageStatus.SENT)
                .edited(false)
                .deleted(false)
                .sentAt(now)
                .build();

        message = messageRepository.save(message);

        conversation.setLastMessage(trimmed);
        conversation.setLastMessageSenderId(currentUserId);
        conversation.setLastMessageType(MessageType.TEXT);
        conversation.setLastMessageAt(now);

        if (senderIsBuyer) {
            conversation.setSellerUnreadCount(conversation.getSellerUnreadCount() + 1);
            conversation.setSellerDeleted(false);
        } else {
            conversation.setBuyerUnreadCount(conversation.getBuyerUnreadCount() + 1);
            conversation.setBuyerDeleted(false);
        }

        conversationRepository.save(conversation);

        log.info("Message {} sent in conversation {} by {}", message.getId(), conversation.getId(), currentUserId);

        // ── Real-time push ──────────────────────────────────────────
        MessageResponse forReceiver = messageMapper.toResponse(message, receiverId);
        messagingTemplate.convertAndSend("/topic/conversation/" + conversation.getId(), forReceiver);

        // 🚀 GLOBAL NOTIFICATION TO RECEIVER (Updates their left Sidebar dynamically like WhatsApp)
        messagingTemplate.convertAndSendToUser(receiverId, "/queue/chat", "REFRESH_SIDEBAR");

        return messageMapper.toResponse(message, currentUserId);
    }

    @Override
    public PagedMessageResponse getMessages(String conversationId, Pageable pageable) {
        String currentUserId = currentUserProvider.getCurrentUserId();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        if (!conversation.isParticipant(currentUserId)) {
            throw new IllegalStateException("You are not a participant in this conversation");
        }

        Page<Message> page = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);

        List<MessageResponse> responses = page.getContent().stream()
                .map(m -> messageMapper.toResponse(m, currentUserId))
                .toList();

        return PagedMessageResponse.builder()
                .messages(responses)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    @Override
    @Transactional
    public MessageResponse markAsRead(String conversationId, ReadMessageRequest request) {
        String currentUserId = currentUserProvider.getCurrentUserId();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        if (!conversation.isParticipant(currentUserId)) {
            throw new IllegalStateException("You are not a participant in this conversation");
        }

        List<Message> unread = messageRepository.findUnreadForUserInConversation(conversationId, currentUserId);

        Instant now = Instant.now();
        Message lastRead = null;

        for (Message message : unread) {
            message.setStatus(MessageStatus.READ);
            message.setReadAt(now);
            messageRepository.save(message);
            lastRead = message;
        }

        if (conversation.getBuyerId().equals(currentUserId)) {
            conversation.setBuyerUnreadCount(0);
        } else {
            conversation.setSellerUnreadCount(0);
        }
        conversationRepository.save(conversation);

        if (lastRead != null) {
            messagingTemplate.convertAndSend("/topic/conversation/" + conversationId,
                    MessageStatusEvent.builder()
                            .conversationId(conversationId)
                            .messageId(lastRead.getId())
                            .userId(currentUserId)
                            .status(MessageStatus.READ)
                            .build());

            return messageMapper.toResponse(lastRead, currentUserId);
        }

        Page<Message> latest = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                conversationId, Pageable.ofSize(1));
        if (latest.hasContent()) {
            return messageMapper.toResponse(latest.getContent().get(0), currentUserId);
        }
        return null;
    }

    @Override
    @Transactional
    public MessageResponse markAsDelivered(String messageId) {
        String currentUserId = currentUserProvider.getCurrentUserId();
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        if (!message.getReceiverId().equals(currentUserId)) {
            throw new IllegalStateException("Only the receiver can mark a message as delivered");
        }

        if (message.getStatus() == MessageStatus.SENT) {
            message.setStatus(MessageStatus.DELIVERED);
            message.setDeliveredAt(Instant.now());
            message = messageRepository.save(message);

            messagingTemplate.convertAndSend("/topic/conversation/" + message.getConversationId(),
                    MessageStatusEvent.builder()
                            .conversationId(message.getConversationId())
                            .messageId(message.getId())
                            .userId(currentUserId)
                            .status(MessageStatus.DELIVERED)
                            .build());
        }

        return messageMapper.toResponse(message, currentUserId);
    }

    @Override
    @Transactional
    public MessageResponse editMessage(String messageId, EditMessageRequest request) {
        String currentUserId = currentUserProvider.getCurrentUserId();
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        if (!message.getSenderId().equals(currentUserId)) {
            throw new IllegalStateException("Only the sender can edit this message");
        }
        if (message.isDeleted()) {
            throw new IllegalStateException("Cannot edit a deleted message");
        }

        String trimmed = request.getMessage() == null ? "" : request.getMessage().trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Message cannot be empty");
        }
        if (trimmed.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message cannot exceed " + MAX_MESSAGE_LENGTH + " characters");
        }

        message.setMessage(trimmed);
        message.setEdited(true);
        message.setEditedAt(Instant.now());
        message = messageRepository.save(message);

        Conversation conversation = conversationRepository.findById(message.getConversationId()).orElse(null);
        if (conversation != null
                && message.getCreatedAt() != null
                && message.getCreatedAt().equals(conversation.getLastMessageAt())) {
            conversation.setLastMessage(trimmed);
            conversationRepository.save(conversation);
        }

        messagingTemplate.convertAndSend("/topic/conversation/" + message.getConversationId(),
                messageMapper.toResponse(message, message.getReceiverId()));

        return messageMapper.toResponse(message, currentUserId);
    }

    @Override
    @Transactional
    public void deleteMessage(String messageId) {
        String currentUserId = currentUserProvider.getCurrentUserId();
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        if (!message.getSenderId().equals(currentUserId)) {
            throw new IllegalStateException("Only the sender can delete this message");
        }

        message.setDeleted(true);
        message.setDeletedAt(Instant.now());
        messageRepository.save(message);

        messagingTemplate.convertAndSend("/topic/conversation/" + message.getConversationId(),
                messageMapper.toResponse(message, message.getReceiverId()));

        log.info("Message {} soft-deleted by {}", messageId, currentUserId);
    }

    @Override
    public UnreadCountResponse getUnreadCount() {
        String currentUserId = currentUserProvider.getCurrentUserId();
        long totalUnread = messageRepository.countByReceiverIdAndStatusNot(currentUserId, MessageStatus.READ);

        long buyerConvCount = conversationRepository
                .countByBuyerIdAndBuyerDeletedFalseAndBuyerUnreadCountGreaterThan(currentUserId, 0);
        long sellerConvCount = conversationRepository
                .countBySellerIdAndSellerDeletedFalseAndSellerUnreadCountGreaterThan(currentUserId, 0);

        return UnreadCountResponse.builder()
                .totalUnreadCount(totalUnread)
                .conversationsWithUnread(buyerConvCount + sellerConvCount)
                .build();
    }
}