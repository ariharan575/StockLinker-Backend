package com.backend.StockLinker.MessageService.service.impl;

import com.backend.StockLinker.MessageService.dto.request.ConversationSearchRequest;
import com.backend.StockLinker.MessageService.dto.request.CreateConversationRequest;
import com.backend.StockLinker.MessageService.dto.response.ConversationListResponse;
import com.backend.StockLinker.MessageService.dto.response.ConversationResponse;
import com.backend.StockLinker.MessageService.entity.Conversation;
import com.backend.StockLinker.MessageService.enums.ConversationStatus;
import com.backend.StockLinker.MessageService.enums.UserRole;
import com.backend.StockLinker.MessageService.mapper.ConversationMapper;
import com.backend.StockLinker.MessageService.repository.ConversationRepository;
import com.backend.StockLinker.MessageService.security.CurrentUserProvider;
import com.backend.StockLinker.MessageService.service.ConversationService;
import com.backend.StockLinker.ProfileService.model.BusinessProfile;
import com.backend.StockLinker.ProfileService.repository.postgres.BusinessProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMapper conversationMapper;
    private final CurrentUserProvider currentUserProvider;
    private final BusinessProfileRepository profileRepository;

    @Override
    @Transactional
    public ConversationResponse createOrGetConversation(CreateConversationRequest request) {
        String currentUserId = currentUserProvider.getCurrentUserId();
        UserRole currentRole = currentUserProvider.getCurrentUserRole();

        if (currentUserId.equals(request.getCounterpartId())) {
            throw new IllegalArgumentException("Cannot start a conversation with yourself");
        }

        // FETCH CURRENT USER'S REAL DETAILS TO PREVENT "UNKNOWN" BUG
        BusinessProfile myProfile = profileRepository.findByUserId(currentUserId).orElse(null);
        String myName = (myProfile != null && myProfile.getBusinessName() != null) ? myProfile.getBusinessName() : "User";
        String myBusiness = (myProfile != null && myProfile.getBusinessType() != null) ? myProfile.getBusinessType() : "Business";
        String myAvatar = "https://ui-avatars.com/api/?name=" + myName.replace(" ", "+") + "&background=0D9488&color=fff";

        String buyerId, sellerId, buyerName, sellerName, buyerBusinessName, sellerBusinessName, buyerProfileImage, sellerProfileImage;

        if (currentRole == UserRole.BUYER) {
            buyerId = currentUserId;
            sellerId = request.getCounterpartId();
            buyerName = myName;
            buyerBusinessName = myBusiness;
            buyerProfileImage = myAvatar;

            sellerName = request.getCounterpartName();
            sellerBusinessName = request.getCounterpartBusinessName();
            sellerProfileImage = request.getCounterpartProfileImage();
        } else {
            sellerId = currentUserId;
            buyerId = request.getCounterpartId();
            sellerName = myName;
            sellerBusinessName = myBusiness;
            sellerProfileImage = myAvatar;

            buyerName = request.getCounterpartName();
            buyerBusinessName = request.getCounterpartBusinessName();
            buyerProfileImage = request.getCounterpartProfileImage();
        }

        Conversation conversation = conversationRepository
                .findByBuyerIdAndSellerId(buyerId, sellerId)
                .orElseGet(() -> {
                    Conversation created = Conversation.builder()
                            .conversationCode(generateConversationCode())
                            .buyerId(buyerId)
                            .sellerId(sellerId)
                            .buyerName(buyerName)
                            .sellerName(sellerName)
                            .buyerBusinessName(buyerBusinessName)
                            .sellerBusinessName(sellerBusinessName)
                            .buyerProfileImage(buyerProfileImage)
                            .sellerProfileImage(sellerProfileImage)
                            .buyerUnreadCount(0)
                            .sellerUnreadCount(0)
                            .buyerArchived(false)
                            .sellerArchived(false)
                            .buyerDeleted(false)
                            .sellerDeleted(false)
                            .buyerBlocked(false)
                            .sellerBlocked(false)
                            .active(true)
                            .status(ConversationStatus.ACTIVE)
                            .build();
                    log.info("Creating new conversation between buyer {} and seller {}", buyerId, sellerId);
                    return conversationRepository.save(created);
                });

        boolean changed = false;
        if (conversation.getBuyerId().equals(currentUserId) && conversation.isBuyerDeleted()) {
            conversation.setBuyerDeleted(false);
            changed = true;
        }
        if (conversation.getSellerId().equals(currentUserId) && conversation.isSellerDeleted()) {
            conversation.setSellerDeleted(false);
            changed = true;
        }
        if (changed) {
            conversation = conversationRepository.save(conversation);
        }

        return conversationMapper.toResponse(conversation, currentUserId);
    }

    @Override
    public ConversationResponse getConversationById(String conversationId) {
        String currentUserId = currentUserProvider.getCurrentUserId();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        if (!conversation.isParticipant(currentUserId)) {
            throw new IllegalStateException("You are not a participant in this conversation");
        }
        return conversationMapper.toResponse(conversation, currentUserId);
    }

    @Override
    public ConversationListResponse listConversations(ConversationSearchRequest request) {
        String currentUserId = currentUserProvider.getCurrentUserId();
        Pageable pageable = PageRequest.of(
                request.getPage(), request.getSize(), Sort.by(Sort.Direction.DESC, "lastMessageAt"));

        Page<Conversation> page;
        if (StringUtils.hasText(request.getKeyword())) {
            String safeRegex = Pattern.quote(request.getKeyword().trim());
            page = conversationRepository.searchForUser(currentUserId, safeRegex, pageable);
        } else if (request.isIncludeArchived()) {
            page = conversationRepository.findAllActiveForUser(currentUserId, pageable);
        } else {
            page = conversationRepository.findAllActiveNonArchivedForUser(currentUserId, pageable);
        }

        List<ConversationResponse> responses = page.getContent().stream()
                .map(conversation -> conversationMapper.toResponse(conversation, currentUserId))
                .toList();

        long totalUnread = responses.stream()
                .mapToLong(ConversationResponse::getUnreadCount)
                .sum();

        return ConversationListResponse.builder()
                .conversations(responses)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .totalUnreadCount(totalUnread)
                .build();
    }

    private String generateConversationCode() {
        return "CONV-" + Instant.now().toEpochMilli() + "-" + (int) (Math.random() * 9000 + 1000);
    }
}