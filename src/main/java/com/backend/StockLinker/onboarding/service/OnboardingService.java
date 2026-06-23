package com.backend.StockLinker.onboarding.service;

import com.backend.StockLinker.onboarding.dto.request.BusinessIdentityRequest;
import com.backend.StockLinker.onboarding.dto.request.BusinessLocationRequest;
import com.backend.StockLinker.onboarding.dto.request.LanguageSelectionRequest;
import com.backend.StockLinker.onboarding.dto.request.ShopkeeperSetupRequest;
import com.backend.StockLinker.onboarding.dto.request.WholesalerSetupRequest;
import com.backend.StockLinker.onboarding.dto.response.CommonBusinessProfileResponse;
import com.backend.StockLinker.onboarding.dto.response.OnboardingCompletionResponse;
import com.backend.StockLinker.onboarding.dto.response.OnboardingDraftResponse;
import com.backend.StockLinker.onboarding.dto.response.OnboardingStatusResponse;
import com.backend.StockLinker.onboarding.dto.response.ShopkeeperBusinessResponse;
import com.backend.StockLinker.onboarding.dto.response.WholesalerBusinessResponse;
import com.backend.StockLinker.onboarding.enums.UserRole;

/**
 * Main onboarding service contract.
 */
public interface OnboardingService {

    /**
     * Save preferred language selection.
     *
     * @param userId authenticated user id
     * @param request language request
     * @return updated profile response
     */
    CommonBusinessProfileResponse saveLanguageSelection(
            Long userId,
            LanguageSelectionRequest request
    );

    /**
     * Save business identity details.
     *
     * @param userId authenticated user id
     * @param request business identity request
     * @return updated profile response
     */
    CommonBusinessProfileResponse saveBusinessIdentity(
            Long userId,
            BusinessIdentityRequest request
    );

    /**
     * Save business location details.
     *
     * @param userId authenticated user id
     * @param request business location request
     * @return updated profile response
     */
    CommonBusinessProfileResponse saveBusinessLocation(
            Long userId,
            BusinessLocationRequest request
    );

    /**
     * Save wholesaler onboarding setup.
     *
     * @param userId authenticated user id
     * @param role authenticated user role
     * @param request wholesaler setup request
     * @return wholesaler onboarding response
     */
    WholesalerBusinessResponse saveWholesalerSetup(
            Long userId,
            UserRole role,
            WholesalerSetupRequest request
    );

    /**
     * Save shopkeeper onboarding setup.
     *
     * @param userId authenticated user id
     * @param role authenticated user role
     * @param request shopkeeper setup request
     * @return shopkeeper onboarding response
     */
    ShopkeeperBusinessResponse saveShopkeeperSetup(
            Long userId,
            UserRole role,
            ShopkeeperSetupRequest request
    );

    /**
     * Fetch onboarding draft data.
     *
     * @param userId authenticated user id
     * @param role authenticated user role
     * @return onboarding draft response
     */
    OnboardingDraftResponse getOnboardingDraft(
            Long userId,
            UserRole role
    );

    /**
     * Mark onboarding process as completed.
     *
     * @param userId authenticated user id
     * @return onboarding completion response
     */
    OnboardingCompletionResponse completeOnboarding(
            Long userId
    );

    /**
     * Fetch onboarding status.
     *
     * @param userId authenticated user id
     * @return onboarding status response
     */
    OnboardingStatusResponse getOnboardingStatus(
            Long userId
    );
}