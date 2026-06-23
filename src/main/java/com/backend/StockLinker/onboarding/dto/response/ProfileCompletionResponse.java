package com.backend.StockLinker.onboarding.dto.response;

import com.backend.StockLinker.onboarding.enums.OnboardingStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for onboarding completion tracking.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileCompletionResponse {

    private Long userId;

    private Integer completionPercentage;

    private OnboardingStep currentStep;

    private Boolean onboardingCompleted;

    private Boolean draftSaved;
}