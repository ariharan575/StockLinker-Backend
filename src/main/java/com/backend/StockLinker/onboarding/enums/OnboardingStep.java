package com.backend.StockLinker.onboarding.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents onboarding workflow steps.
 */
@Getter
@RequiredArgsConstructor
public enum OnboardingStep {

    LANGUAGE_SELECTION(1, "Language Selection"),
    BUSINESS_IDENTITY(2, "Business Identity"),
    BUSINESS_LOCATION(3, "Business Location"),
    ROLE_BASED_SETUP(4, "Role Based Setup"),
    COMPLETED(5, "Completed");

    private final Integer stepOrder;
    private final String displayName;
}