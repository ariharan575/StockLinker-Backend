package com.backend.StockLinker.Message_Service.service.impl;

import com.backend.StockLinker.Audit_Service.Dto.AuditLogRequest;
import com.backend.StockLinker.Audit_Service.Entity.AuditLog;
import com.backend.StockLinker.Audit_Service.Enums.AuditAction;
import com.backend.StockLinker.Audit_Service.Enums.ResourceType;
import com.backend.StockLinker.Audit_Service.Services.AuditService;
import com.backend.StockLinker.Auth_Service.service.IpAddressService;
import com.backend.StockLinker.Exception.customExceptions.BadRequestException;
import com.backend.StockLinker.Exception.customExceptions.ForbiddenException;
import com.backend.StockLinker.Exception.customExceptions.ResourceNotFoundException;
import com.backend.StockLinker.Message_Service.dto.request.ConversationSearchRequest;
import com.backend.StockLinker.Message_Service.dto.request.CreateConversationRequest;
import com.backend.StockLinker.Message_Service.dto.response.ConversationListResponse;
import com.backend.StockLinker.Message_Service.dto.response.ConversationResponse;
import com.backend.StockLinker.Message_Service.entity.Conversation;
import com.backend.StockLinker.Message_Service.enums.ConversationStatus;
import com.backend.StockLinker.Message_Service.enums.UserRole;
import com.backend.StockLinker.Message_Service.mapper.ConversationMapper;
import com.backend.StockLinker.Message_Service.repository.ConversationRepository;
import com.backend.StockLinker.Message_Service.security.CurrentUserProvider;
import com.backend.StockLinker.Message_Service.service.ConversationService;
import com.backend.StockLinker.Profile_Service.model.BusinessProfile;
import com.backend.StockLinker.Profile_Service.repository.BusinessProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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

    private final AuditService auditService;
    private final IpAddressService ipAddressService;

    private HttpServletRequest getCurrentHttpRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attrs).getRequest();
        }
        return null;
    }

    @Override
    @Transactional
    public ConversationResponse createOrGetConversation(CreateConversationRequest request) {
        String currentUserId = currentUserProvider.getCurrentUserId();
        UserRole currentRole = currentUserProvider.getCurrentUserRole();

        if (currentUserId.equals(request.getCounterpartId())) {
            throw new BadRequestException("Cannot start a conversation with yourself");
        }

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

                    logAudit(currentUserId, AuditAction.CONVERSATION_STARTED, "Started conversation with: " + request.getCounterpartId());

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
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        if (!conversation.isParticipant(currentUserId)) {
            throw new ForbiddenException("You are not a participant in this conversation");
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

    private void logAudit(String userId, AuditAction action, String details) {
        HttpServletRequest request = getCurrentHttpRequest();
        String ip = (request != null) ? ipAddressService.getClientIp(request) : "Unknown";
        String userAgent = (request != null) ? request.getHeader(HttpHeaders.USER_AGENT) : "Unknown";
        String deviceId = (request != null) ? (String) request.getAttribute("deviceId") : "Unknown";

        auditService.log(AuditLogRequest.builder()
                .userId(userId)
                .action(action)
                .resourceType(ResourceType.MESSAGE)
                .ipAddress(ip)
                .userAgent(userAgent)
                .deviceId(deviceId)
                .status(AuditLog.Status.SUCCESS)
                .newValue(details)
                .build());
    }
}