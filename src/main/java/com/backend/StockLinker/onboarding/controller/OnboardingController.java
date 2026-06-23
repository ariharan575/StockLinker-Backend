package com.backend.StockLinker.onboarding.controller;

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
import com.backend.StockLinker.onboarding.response.ApiSuccessResponse;
import com.backend.StockLinker.onboarding.service.OnboardingService;
import com.backend.StockLinker.onboarding.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST controller for onboarding operations.
 */
@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    /**
     * Save language selection.
     *
     * @param request language request
     * @return profile response
     */
    @PostMapping("/language")
    public ResponseEntity<ApiSuccessResponse<
            CommonBusinessProfileResponse>>
    saveLanguageSelection(
            @Valid
            @RequestBody
            final LanguageSelectionRequest request
    ) {

        Long userId =
                SecurityUtil.getCurrentUserId();

        CommonBusinessProfileResponse response =
                onboardingService.saveLanguageSelection(
                        userId,
                        request
                );

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ApiSuccessResponse
                                .<CommonBusinessProfileResponse>
                                        builder()
                                .success(Boolean.TRUE)
                                .message(
                                        "Language selection saved successfully"
                                )
                                .data(response)
                                .timestamp(
                                        LocalDateTime.now()
                                )
                                .build()
                );
    }

    /**
     * Save business identity.
     *
     * @param request business identity request
     * @return profile response
     */
    @PostMapping("/business-identity")
    public ResponseEntity<ApiSuccessResponse<
            CommonBusinessProfileResponse>>
    saveBusinessIdentity(
            @Valid
            @RequestBody
            final BusinessIdentityRequest request
    ) {

        Long userId =
                SecurityUtil.getCurrentUserId();

        CommonBusinessProfileResponse response =
                onboardingService.saveBusinessIdentity(
                        userId,
                        request
                );

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ApiSuccessResponse
                                .<CommonBusinessProfileResponse>
                                        builder()
                                .success(Boolean.TRUE)
                                .message(
                                        "Business identity saved successfully"
                                )
                                .data(response)
                                .timestamp(
                                        LocalDateTime.now()
                                )
                                .build()
                );
    }

    /**
     * Save business location.
     *
     * @param request business location request
     * @return profile response
     */
    @PostMapping("/business-location")
    public ResponseEntity<ApiSuccessResponse<
            CommonBusinessProfileResponse>>
    saveBusinessLocation(
            @Valid
            @RequestBody
            final BusinessLocationRequest request
    ) {

        Long userId =
                SecurityUtil.getCurrentUserId();

        CommonBusinessProfileResponse response =
                onboardingService.saveBusinessLocation(
                        userId,
                        request
                );

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ApiSuccessResponse
                                .<CommonBusinessProfileResponse>
                                        builder()
                                .success(Boolean.TRUE)
                                .message(
                                        "Business location saved successfully"
                                )
                                .data(response)
                                .timestamp(
                                        LocalDateTime.now()
                                )
                                .build()
                );
    }

    /**
     * Save wholesaler setup.
     *
     * @param request wholesaler setup request
     * @return wholesaler response
     */
    @PostMapping("/wholesaler-setup")
    public ResponseEntity<ApiSuccessResponse<
            WholesalerBusinessResponse>>
    saveWholesalerSetup(
            @Valid
            @RequestBody
            final WholesalerSetupRequest request
    ) {

        Long userId =
                SecurityUtil.getCurrentUserId();

        UserRole role =
                SecurityUtil.getCurrentUserRole();

        WholesalerBusinessResponse response =
                onboardingService.saveWholesalerSetup(
                        userId,
                        role,
                        request
                );

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ApiSuccessResponse
                                .<WholesalerBusinessResponse>
                                        builder()
                                .success(Boolean.TRUE)
                                .message(
                                        "Wholesaler setup saved successfully"
                                )
                                .data(response)
                                .timestamp(
                                        LocalDateTime.now()
                                )
                                .build()
                );
    }

    /**
     * Save shopkeeper setup.
     *
     * @param request shopkeeper setup request
     * @return shopkeeper response
     */
    @PostMapping("/shopkeeper-setup")
    public ResponseEntity<ApiSuccessResponse<
            ShopkeeperBusinessResponse>>
    saveShopkeeperSetup(
            @Valid
            @RequestBody
            final ShopkeeperSetupRequest request
    ) {

        Long userId =
                SecurityUtil.getCurrentUserId();

        UserRole role =
                SecurityUtil.getCurrentUserRole();

        ShopkeeperBusinessResponse response =
                onboardingService.saveShopkeeperSetup(
                        userId,
                        role,
                        request
                );

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ApiSuccessResponse
                                .<ShopkeeperBusinessResponse>
                                        builder()
                                .success(Boolean.TRUE)
                                .message(
                                        "Shopkeeper setup saved successfully"
                                )
                                .data(response)
                                .timestamp(
                                        LocalDateTime.now()
                                )
                                .build()
                );
    }

    /**
     * Recover onboarding draft.
     *
     * @return onboarding draft response
     */
    @GetMapping("/draft")
    public ResponseEntity<ApiSuccessResponse<
            OnboardingDraftResponse>>
    getOnboardingDraft() {

        Long userId =
                SecurityUtil.getCurrentUserId();

        UserRole role =
                SecurityUtil.getCurrentUserRole();

        OnboardingDraftResponse response =
                onboardingService.getOnboardingDraft(
                        userId,
                        role
                );

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ApiSuccessResponse
                                .<OnboardingDraftResponse>
                                        builder()
                                .success(Boolean.TRUE)
                                .message(
                                        "Onboarding draft fetched successfully"
                                )
                                .data(response)
                                .timestamp(
                                        LocalDateTime.now()
                                )
                                .build()
                );
    }

    /**
     * Complete onboarding process.
     *
     * @return completion response
     */
    @PostMapping("/complete")
    public ResponseEntity<ApiSuccessResponse<
            OnboardingCompletionResponse>>
    completeOnboarding() {

        Long userId =
                SecurityUtil.getCurrentUserId();

        OnboardingCompletionResponse response =
                onboardingService.completeOnboarding(
                        userId
                );

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ApiSuccessResponse
                                .<OnboardingCompletionResponse>
                                        builder()
                                .success(Boolean.TRUE)
                                .message(
                                        "Onboarding completed successfully"
                                )
                                .data(response)
                                .timestamp(
                                        LocalDateTime.now()
                                )
                                .build()
                );
    }

    /**
     * Fetch onboarding status.
     *
     * @return onboarding status response
     */
    @GetMapping("/status")
    public ResponseEntity<ApiSuccessResponse<
            OnboardingStatusResponse>>
    getOnboardingStatus() {

        Long userId =
                SecurityUtil.getCurrentUserId();

        OnboardingStatusResponse response =
                onboardingService.getOnboardingStatus(
                        userId
                );

        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        ApiSuccessResponse
                                .<OnboardingStatusResponse>
                                        builder()
                                .success(Boolean.TRUE)
                                .message(
                                        "Onboarding status fetched successfully"
                                )
                                .data(response)
                                .timestamp(
                                        LocalDateTime.now()
                                )
                                .build()
                );
    }
}