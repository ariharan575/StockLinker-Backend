package com.backend.StockLinker.MessageService.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to start (or fetch, if already existing) a conversation with another user.
 * The caller's own id/role is never accepted here — it is resolved from the JWT.
 * Only the counterpart's id is required; the caller's role determines whether
 * the counterpart is treated as buyer or seller.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {

    @NotBlank(message = "Counterpart user id is required")
    private String counterpartId;

    @NotBlank(message = "Counterpart name is required")
    private String counterpartName;

    private String counterpartBusinessName;

    private String counterpartProfileImage;
}