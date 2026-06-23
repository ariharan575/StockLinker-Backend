package com.backend.StockLinker.onboarding.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for onboarding draft recovery.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingDraftResponse {

    private CommonBusinessProfileResponse commonBusinessProfile;

    private WholesalerBusinessResponse wholesalerBusinessDetails;

    private ShopkeeperBusinessResponse shopkeeperBusinessDetails;

    private ProfileCompletionResponse profileCompletion;
}
