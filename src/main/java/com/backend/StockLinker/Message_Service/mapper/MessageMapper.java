package com.backend.StockLinker.Message_Service.mapper;

import com.backend.StockLinker.Message_Service.dto.response.MessageResponse;
import com.backend.StockLinker.Message_Service.entity.Message;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps Message entities to MessageResponse DTOs.
 * "mine" is viewer-relative and always set explicitly by the caller.
 */
@Mapper(componentModel = "spring")
public abstract class MessageMapper {

    @Mapping(target = "mine", expression = "java(message.getSenderId().equals(viewerUserId))")
    @Mapping(target = "message", expression = "java(message.isDeleted() ? \"This message was deleted\" : message.getMessage())")
    public abstract MessageResponse toResponse(Message message, @Context String viewerUserId);
}