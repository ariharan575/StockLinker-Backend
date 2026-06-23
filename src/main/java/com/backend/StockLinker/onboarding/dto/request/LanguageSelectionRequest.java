package com.backend.StockLinker.onboarding.dto.request;

import com.backend.StockLinker.onboarding.enums.PreferredLanguage;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for language selection step.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LanguageSelectionRequest {

    @NotNull(message = "Preferred language is required")
    private PreferredLanguage preferredLanguage;
}