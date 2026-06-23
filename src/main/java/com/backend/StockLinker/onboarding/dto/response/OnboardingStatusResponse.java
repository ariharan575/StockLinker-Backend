package com.backend.StockLinker.onboarding.dto.response;

import com.backend.StockLinker.onboarding.enums.OnboardingStep;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for onboarding status.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingStatusResponse {

    private Long userId;

    private OnboardingStep currentStep;

    private Integer completionPercentage;

    private Boolean onboardingCompleted;

    private Boolean draftAvailable;

    private String nextAction;

    private String statusMessage;
}
