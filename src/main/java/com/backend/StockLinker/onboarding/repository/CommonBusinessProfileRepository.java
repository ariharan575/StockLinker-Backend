package com.backend.StockLinker.onboarding.repository;

import com.backend.StockLinker.onboarding.entity.CommonBusinessProfile;
import com.backend.StockLinker.onboarding.enums.OnboardingStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for common business profile onboarding data.
 */
@Repository
public interface CommonBusinessProfileRepository extends JpaRepository<CommonBusinessProfile, Long> {

    /**
     * Find common business profile by user id.
     *
     * @param userId authenticated user id
     * @return optional business profile
     */
    Optional<CommonBusinessProfile> findByUserId(Long userId);

    /**
     * Check profile existence by user id.
     *
     * @param userId authenticated user id
     * @return true if exists
     */
    boolean existsByUserId(Long userId);

    /**
     * Check onboarding completion state.
     *
     * @param userId authenticated user id
     * @param isOnboardingCompleted onboarding completion flag
     * @return true if matching state exists
     */
    boolean existsByUserIdAndIsOnboardingCompleted(
            Long userId,
            Boolean isOnboardingCompleted
    );

    /**
     * Find profile by user id and onboarding step.
     *
     * @param userId authenticated user id
     * @param onboardingStep onboarding step
     * @return optional profile
     */
    Optional<CommonBusinessProfile> findByUserIdAndOnboardingStep(
            Long userId,
            OnboardingStep onboardingStep
    );
}