package com.backend.StockLinker.onboarding.service.impl;

import com.backend.StockLinker.onboarding.entity.CommonBusinessProfile;
import com.backend.StockLinker.onboarding.enums.OnboardingStep;
import com.backend.StockLinker.onboarding.enums.UserRole;
import com.backend.StockLinker.onboarding.exception.DuplicateOnboardingCompletionException;
import com.backend.StockLinker.onboarding.exception.InvalidOnboardingStepException;
import com.backend.StockLinker.onboarding.exception.InvalidRoleSetupException;
import com.backend.StockLinker.onboarding.exception.ValidationException;
import com.backend.StockLinker.onboarding.service.ValidationOrchestratorService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Implementation for onboarding validation orchestration.
 */
@Service
public class ValidationOrchestratorServiceImpl
        implements ValidationOrchestratorService {

    /**
     * Validate onboarding step transition.
     *
     * @param currentStep current onboarding step
     * @param requestedStep requested onboarding step
     */
    @Override
    public void validateStepTransition(
            final OnboardingStep currentStep,
            final OnboardingStep requestedStep
    ) {

        if (currentStep == null) {
            return;
        }

        if (requestedStep.getStepOrder()
                < currentStep.getStepOrder()) {

            throw new InvalidOnboardingStepException(
                    "Invalid onboarding step transition detected"
            );
        }
    }

    /**
     * Validate onboarding completion eligibility.
     *
     * @param profile business profile
     */
    @Override
    public void validateCompletionEligibility(
            final CommonBusinessProfile profile
    ) {

        if (profile.getPreferredLanguage() == null) {
            throw new ValidationException(
                    "Preferred language is required"
            );
        }

        if (!StringUtils.hasText(profile.getBusinessName())) {
            throw new ValidationException(
                    "Business identity details are incomplete"
            );
        }

        if (!StringUtils.hasText(profile.getAddressLine1())) {
            throw new ValidationException(
                    "Business location details are incomplete"
            );
        }
    }

    /**
     * Validate role-based onboarding access.
     *
     * @param role authenticated role
     * @param requiredRole required role
     */
    @Override
    public void validateRoleAccess(
            final UserRole role,
            final UserRole requiredRole
    ) {

        if (role != requiredRole) {

            throw new InvalidRoleSetupException(
                    "Access denied for requested onboarding setup"
            );
        }
    }

    /**
     * Validate duplicate onboarding completion.
     *
     * @param onboardingCompleted onboarding completion flag
     */
    @Override
    public void validateDuplicateCompletion(
            final Boolean onboardingCompleted
    ) {

        if (Boolean.TRUE.equals(onboardingCompleted)) {

            throw new DuplicateOnboardingCompletionException(
                    "Onboarding already completed"
            );
        }
    }
}
