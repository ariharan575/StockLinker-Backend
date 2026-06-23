package com.backend.StockLinker.onboarding.service;

import com.backend.StockLinker.onboarding.entity.CommonBusinessProfile;
import com.backend.StockLinker.onboarding.enums.OnboardingStep;
import com.backend.StockLinker.onboarding.enums.UserRole;

/**
 * Service contract for onboarding validation orchestration.
 */
public interface ValidationOrchestratorService {

    /**
     * Validate onboarding step transition.
     *
     * @param currentStep current onboarding step
     * @param requestedStep requested onboarding step
     */
    void validateStepTransition(
            OnboardingStep currentStep,
            OnboardingStep requestedStep
    );

    /**
     * Validate onboarding completion eligibility.
     *
     * @param profile business profile
     */
    void validateCompletionEligibility(
            CommonBusinessProfile profile
    );

    /**
     * Validate role-based onboarding access.
     *
     * @param role authenticated user role
     * @param requiredRole required role
     */
    void validateRoleAccess(
            UserRole role,
            UserRole requiredRole
    );

    /**
     * Validate duplicate onboarding completion.
     *
     * @param onboardingCompleted onboarding completion flag
     */
    void validateDuplicateCompletion(
            Boolean onboardingCompleted
    );
}
