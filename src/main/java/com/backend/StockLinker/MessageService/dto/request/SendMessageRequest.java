package com.backend.StockLinker.MessageService.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    @NotBlank(message = "Conversation id is required")
    private String conversationId;

    @NotBlank(message = "Message text cannot be empty")
    @Size(max = 4000, message = "Message cannot exceed 4000 characters")
    private String message;
}