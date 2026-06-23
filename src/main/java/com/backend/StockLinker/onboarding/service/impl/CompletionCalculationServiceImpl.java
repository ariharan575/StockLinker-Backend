package com.backend.StockLinker.onboarding.service.impl;

import com.backend.StockLinker.onboarding.entity.CommonBusinessProfile;
import com.backend.StockLinker.onboarding.enums.UserRole;
import com.backend.StockLinker.onboarding.service.CompletionCalculationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Implementation for onboarding completion calculation service.
 */
@Service
public class CompletionCalculationServiceImpl
        implements CompletionCalculationService {

    private static final int TOTAL_STEPS = 5;

    private static final int STEP_WEIGHT = 20;

    /**
     * Calculate onboarding completion percentage.
     *
     * @param profile common business profile
     * @param role authenticated role
     * @return completion percentage
     */
    @Override
    public Integer calculateCompletionPercentage(
            final CommonBusinessProfile profile,
            final UserRole role
    ) {

        int completedSteps = 0;

        if (profile.getPreferredLanguage() != null) {
            completedSteps++;
        }

        if (isBusinessIdentityCompleted(profile)) {
            completedSteps++;
        }

        if (isBusinessLocationCompleted(profile)) {
            completedSteps++;
        }

        if (isRoleSpecificSetupCompleted(profile, role)) {
            completedSteps++;
        }

        if (Boolean.TRUE.equals(profile.getIsOnboardingCompleted())) {
            completedSteps++;
        }

        return Math.min(completedSteps * STEP_WEIGHT, 100);
    }

    /**
     * Validate business identity completion.
     *
     * @param profile common profile
     * @return completion status
     */
    private boolean isBusinessIdentityCompleted(
            final CommonBusinessProfile profile
    ) {

        return StringUtils.hasText(profile.getOwnerName())
                && StringUtils.hasText(profile.getBusinessName())
                && profile.getBusinessType() != null
                && StringUtils.hasText(profile.getBusinessCategory())
                && StringUtils.hasText(profile.getMobile())
                && StringUtils.hasText(profile.getEmail());
    }

    /**
     * Validate business location completion.
     *
     * @param profile common profile
     * @return completion status
     */
    private boolean isBusinessLocationCompleted(
            final CommonBusinessProfile profile
    ) {

        return StringUtils.hasText(profile.getAddressLine1())
                && StringUtils.hasText(profile.getCity())
                && StringUtils.hasText(profile.getDistrict())
                && StringUtils.hasText(profile.getState())
                && StringUtils.hasText(profile.getCountry())
                && StringUtils.hasText(profile.getPincode());
    }

    /**
     * Validate role-specific setup completion.
     *
     * @param profile common profile
     * @param role authenticated role
     * @return completion status
     */
    private boolean isRoleSpecificSetupCompleted(
            final CommonBusinessProfile profile,
            final UserRole role
    ) {

        return role != null;
    }
}