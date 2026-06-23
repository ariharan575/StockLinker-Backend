package com.backend.StockLinker.onboarding.util;

import com.backend.StockLinker.onboarding.enums.OnboardingStep;

/**
 * Utility class for onboarding step operations.
 */
public final class OnboardingStepUtil {

    private OnboardingStepUtil() {
    }

    /**
     * Get next onboarding step.
     *
     * @param currentStep current step
     * @return next step
     */
    public static OnboardingStep getNextStep(
            final OnboardingStep currentStep
    ) {

        if (currentStep == null) {
            return OnboardingStep.LANGUAGE_SELECTION;
        }

        return switch (currentStep) {

            case LANGUAGE_SELECTION ->
                    OnboardingStep.BUSINESS_IDENTITY;

            case BUSINESS_IDENTITY ->
                    OnboardingStep.BUSINESS_LOCATION;

            case BUSINESS_LOCATION ->
                    OnboardingStep.ROLE_BASED_SETUP;

            case ROLE_BASED_SETUP ->
                    OnboardingStep.COMPLETED;

            default ->
                    OnboardingStep.COMPLETED;
        };
    }

    /**
     * Check onboarding completion.
     *
     * @param step onboarding step
     * @return true if completed
     */
    public static boolean isCompleted(
            final OnboardingStep step
    ) {

        return step == OnboardingStep.COMPLETED;
    }
}
