package com.backend.StockLinker.MessageService.mapper;

import com.backend.StockLinker.MessageService.dto.response.ConversationResponse;
import com.backend.StockLinker.MessageService.entity.Conversation;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * Maps Conversation entities to viewer-relative ConversationResponse DTOs.
 * "Viewer-relative" means all counterpart* fields resolve to the OTHER party,
 * which depends on which of buyerId/sellerId matches the requesting user —
 * so every mapping method takes the viewer's userId explicitly.
 */
@Mapper(componentModel = "spring")
public abstract class ConversationMapper {

    @Mapping(target = "id", source = "conversation.id")
    @Mapping(target = "conversationCode", source = "conversation.conversationCode")
    @Mapping(target = "counterpartId", expression = "java(resolveCounterpartId(conversation, viewerUserId))")
    @Mapping(target = "counterpartName", expression = "java(isBuyer(conversation, viewerUserId) ? conversation.getSellerName() : conversation.getBuyerName())")
    @Mapping(target = "counterpartBusinessName", expression = "java(isBuyer(conversation, viewerUserId) ? conversation.getSellerBusinessName() : conversation.getBuyerBusinessName())")
    @Mapping(target = "counterpartProfileImage", expression = "java(isBuyer(conversation, viewerUserId) ? conversation.getSellerProfileImage() : conversation.getBuyerProfileImage())")
    @Mapping(target = "counterpartRole", expression = "java(isBuyer(conversation, viewerUserId) ? com.backend.StockLinker.MessageService.enums.UserRole.SELLER : com.backend.StockLinker.MessageService.enums.UserRole.BUYER)")
    @Mapping(target = "myRole", expression = "java(isBuyer(conversation, viewerUserId) ? com.backend.StockLinker.MessageService.enums.UserRole.BUYER : com.backend.StockLinker.MessageService.enums.UserRole.SELLER)")
    @Mapping(target = "lastMessage", source = "conversation.lastMessage")
    @Mapping(target = "lastMessageSenderId", source = "conversation.lastMessageSenderId")
    @Mapping(target = "lastMessageType", source = "conversation.lastMessageType")
    @Mapping(target = "lastMessageAt", source = "conversation.lastMessageAt")
    @Mapping(target = "unreadCount", expression = "java(isBuyer(conversation, viewerUserId) ? conversation.getBuyerUnreadCount() : conversation.getSellerUnreadCount())")
    @Mapping(target = "archived", expression = "java(isBuyer(conversation, viewerUserId) ? conversation.isBuyerArchived() : conversation.isSellerArchived())")
    @Mapping(target = "blocked", expression = "java(isBuyer(conversation, viewerUserId) ? conversation.isSellerBlocked() : conversation.isBuyerBlocked())")
    @Mapping(target = "counterpartBlocked", expression = "java(isBuyer(conversation, viewerUserId) ? conversation.isBuyerBlocked() : conversation.isSellerBlocked())")
    @Mapping(target = "active", source = "conversation.active")
    @Mapping(target = "status", source = "conversation.status")
    @Mapping(target = "createdAt", source = "conversation.createdAt")
    @Mapping(target = "updatedAt", source = "conversation.updatedAt")
    public abstract ConversationResponse toResponse(Conversation conversation, @Context String viewerUserId);

    @Named("isBuyer")
    protected boolean isBuyer(Conversation conversation, String viewerUserId) {
        return conversation.getBuyerId().equals(viewerUserId);
    }

    protected String resolveCounterpartId(Conversation conversation, String viewerUserId) {
        return conversation.otherPartyId(viewerUserId);
    }
}