package com.backend.StockLinker.onboarding.service;

import com.backend.StockLinker.onboarding.entity.CommonBusinessProfile;
import com.backend.StockLinker.onboarding.enums.UserRole;

/**
 * Service contract for onboarding completion tracking.
 */
public interface CompletionCalculationService {

    /**
     * Calculate onboarding completion percentage.
     *
     * @param profile common business profile
     * @param role authenticated user role
     * @return completion percentage
     */
    Integer calculateCompletionPercentage(
            CommonBusinessProfile profile,
            UserRole role
    );
}