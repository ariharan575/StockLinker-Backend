package com.backend.StockLinker.onboarding.service.impl;

import com.backend.StockLinker.onboarding.dto.request.BusinessIdentityRequest;
import com.backend.StockLinker.onboarding.dto.request.BusinessLocationRequest;
import com.backend.StockLinker.onboarding.dto.request.LanguageSelectionRequest;
import com.backend.StockLinker.onboarding.dto.request.ShopkeeperSetupRequest;
import com.backend.StockLinker.onboarding.dto.request.WholesalerSetupRequest;
import com.backend.StockLinker.onboarding.dto.response.CommonBusinessProfileResponse;
import com.backend.StockLinker.onboarding.dto.response.OnboardingCompletionResponse;
import com.backend.StockLinker.onboarding.dto.response.OnboardingDraftResponse;
import com.backend.StockLinker.onboarding.dto.response.OnboardingStatusResponse;
import com.backend.StockLinker.onboarding.dto.response.ProfileCompletionResponse;
import com.backend.StockLinker.onboarding.dto.response.ShopkeeperBusinessResponse;
import com.backend.StockLinker.onboarding.dto.response.WholesalerBusinessResponse;
import com.backend.StockLinker.onboarding.entity.CommonBusinessProfile;
import com.backend.StockLinker.onboarding.entity.ShopkeeperBusinessDetails;
import com.backend.StockLinker.onboarding.entity.WholesalerBusinessDetails;
import com.backend.StockLinker.onboarding.enums.OnboardingStep;
import com.backend.StockLinker.onboarding.enums.UserRole;
import com.backend.StockLinker.onboarding.mapper.CommonBusinessProfileMapper;
import com.backend.StockLinker.onboarding.mapper.ShopkeeperBusinessMapper;
import com.backend.StockLinker.onboarding.mapper.WholesalerBusinessMapper;
import com.backend.StockLinker.onboarding.repository.CommonBusinessProfileRepository;
import com.backend.StockLinker.onboarding.repository.ShopkeeperBusinessDetailsRepository;
import com.backend.StockLinker.onboarding.repository.WholesalerBusinessDetailsRepository;
import com.backend.StockLinker.onboarding.service.CompletionCalculationService;
import com.backend.StockLinker.onboarding.service.DraftManagementService;
import com.backend.StockLinker.onboarding.service.OnboardingService;
import com.backend.StockLinker.onboarding.service.ValidationOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Enterprise onboarding service implementation.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OnboardingServiceImpl implements OnboardingService {

    private final CommonBusinessProfileRepository
            commonBusinessProfileRepository;

    private final WholesalerBusinessDetailsRepository
            wholesalerBusinessDetailsRepository;

    private final ShopkeeperBusinessDetailsRepository
            shopkeeperBusinessDetailsRepository;

    private final CommonBusinessProfileMapper
            commonBusinessProfileMapper;

    private final WholesalerBusinessMapper
            wholesalerBusinessMapper;

    private final ShopkeeperBusinessMapper
            shopkeeperBusinessMapper;

    private final CompletionCalculationService
            completionCalculationService;

    private final DraftManagementService
            draftManagementService;

    private final ValidationOrchestratorService
            validationOrchestratorService;

    @Override
    public CommonBusinessProfileResponse saveLanguageSelection(
            final Long userId,
            final LanguageSelectionRequest request
    ) {

        CommonBusinessProfile profile =
                getOrCreateProfile(userId);

        commonBusinessProfileMapper.updateLanguageSelection(
                request,
                profile
        );

        profile.setOnboardingStep(
                OnboardingStep.LANGUAGE_SELECTION
        );

        profile.setLastProfileUpdatedAt(LocalDateTime.now());

        draftManagementService.markDraftSaved(profile);

        CommonBusinessProfile savedProfile =
                commonBusinessProfileRepository.save(profile);

        return commonBusinessProfileMapper.toResponse(
                savedProfile
        );
    }

    @Override
    public CommonBusinessProfileResponse saveBusinessIdentity(
            final Long userId,
            final BusinessIdentityRequest request
    ) {

        CommonBusinessProfile profile =
                getOrCreateProfile(userId);

        commonBusinessProfileMapper.updateBusinessIdentity(
                request,
                profile
        );

        profile.setOnboardingStep(
                OnboardingStep.BUSINESS_IDENTITY
        );

        profile.setLastProfileUpdatedAt(LocalDateTime.now());

        draftManagementService.markDraftSaved(profile);

        CommonBusinessProfile savedProfile =
                commonBusinessProfileRepository.save(profile);

        return commonBusinessProfileMapper.toResponse(
                savedProfile
        );
    }

    @Override
    public CommonBusinessProfileResponse saveBusinessLocation(
            final Long userId,
            final BusinessLocationRequest request
    ) {

        CommonBusinessProfile profile =
                getOrCreateProfile(userId);

        commonBusinessProfileMapper.updateBusinessLocation(
                request,
                profile
        );

        profile.setOnboardingStep(
                OnboardingStep.BUSINESS_LOCATION
        );

        profile.setLastProfileUpdatedAt(LocalDateTime.now());

        draftManagementService.markDraftSaved(profile);

        CommonBusinessProfile savedProfile =
                commonBusinessProfileRepository.save(profile);

        return commonBusinessProfileMapper.toResponse(
                savedProfile
        );
    }

    @Override
    public WholesalerBusinessResponse saveWholesalerSetup(
            final Long userId,
            final UserRole role,
            final WholesalerSetupRequest request
    ) {

        validationOrchestratorService.validateRoleAccess(
                role,
                UserRole.WHOLESALER
        );

        WholesalerBusinessDetails details =
                wholesalerBusinessDetailsRepository
                        .findByUserId(userId)
                        .orElse(
                                WholesalerBusinessDetails
                                        .builder()
                                        .userId(userId)
                                        .build()
                        );

        wholesalerBusinessMapper.updateWholesalerDetails(
                request,
                details
        );

        WholesalerBusinessDetails savedDetails =
                wholesalerBusinessDetailsRepository
                        .save(details);

        return wholesalerBusinessMapper.toResponse(
                savedDetails
        );
    }

    @Override
    public ShopkeeperBusinessResponse saveShopkeeperSetup(
            final Long userId,
            final UserRole role,
            final ShopkeeperSetupRequest request
    ) {

        validationOrchestratorService.validateRoleAccess(
                role,
                UserRole.SHOPKEEPER
        );

        ShopkeeperBusinessDetails details =
                shopkeeperBusinessDetailsRepository
                        .findByUserId(userId)
                        .orElse(
                                ShopkeeperBusinessDetails
                                        .builder()
                                        .userId(userId)
                                        .build()
                        );

        shopkeeperBusinessMapper.updateShopkeeperDetails(
                request,
                details
        );

        ShopkeeperBusinessDetails savedDetails =
                shopkeeperBusinessDetailsRepository
                        .save(details);

        return shopkeeperBusinessMapper.toResponse(
                savedDetails
        );
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingDraftResponse getOnboardingDraft(
            final Long userId,
            final UserRole role
    ) {

        CommonBusinessProfile profile =
                draftManagementService.resumeDraft(userId);

        WholesalerBusinessResponse wholesalerResponse = null;
        ShopkeeperBusinessResponse shopkeeperResponse = null;

        if (role == UserRole.WHOLESALER) {

            wholesalerResponse =
                    wholesalerBusinessDetailsRepository
                            .findByUserId(userId)
                            .map(
                                    wholesalerBusinessMapper::toResponse
                            )
                            .orElse(null);
        }

        if (role == UserRole.SHOPKEEPER) {

            shopkeeperResponse =
                    shopkeeperBusinessDetailsRepository
                            .findByUserId(userId)
                            .map(
                                    shopkeeperBusinessMapper::toResponse
                            )
                            .orElse(null);
        }

        return OnboardingDraftResponse.builder()
                .commonBusinessProfile(
                        commonBusinessProfileMapper.toResponse(
                                profile
                        )
                )
                .wholesalerBusinessDetails(
                        wholesalerResponse
                )
                .shopkeeperBusinessDetails(
                        shopkeeperResponse
                )
                .profileCompletion(
                        ProfileCompletionResponse.builder()
                                .userId(userId)
                                .completionPercentage(
                                        profile.getCompletionPercentage()
                                )
                                .currentStep(
                                        profile.getOnboardingStep()
                                )
                                .draftSaved(
                                        profile.getIsDraftSaved()
                                )
                                .onboardingCompleted(
                                        profile.getIsOnboardingCompleted()
                                )
                                .build()
                )
                .build();
    }

    @Override
    public OnboardingCompletionResponse completeOnboarding(
            final Long userId
    ) {

        CommonBusinessProfile profile =
                getOrCreateProfile(userId);

        validationOrchestratorService
                .validateDuplicateCompletion(
                        profile.getIsOnboardingCompleted()
                );

        validationOrchestratorService
                .validateCompletionEligibility(profile);

        profile.setIsOnboardingCompleted(Boolean.TRUE);

        profile.setOnboardingStep(OnboardingStep.COMPLETED);

        profile.setCompletionPercentage(100);

        draftManagementService.clearDraft(profile);

        commonBusinessProfileRepository.save(profile);

        return OnboardingCompletionResponse.builder()
                .userId(userId)
                .onboardingCompleted(Boolean.TRUE)
                .completionPercentage(100)
                .completionMessage(
                        "Onboarding completed successfully"
                )
                .completedAt(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingStatusResponse getOnboardingStatus(
            final Long userId
    ) {

        CommonBusinessProfile profile =
                getOrCreateProfile(userId);

        return OnboardingStatusResponse.builder()
                .userId(userId)
                .currentStep(profile.getOnboardingStep())
                .completionPercentage(
                        profile.getCompletionPercentage()
                )
                .onboardingCompleted(
                        profile.getIsOnboardingCompleted()
                )
                .draftAvailable(
                        profile.getIsDraftSaved()
                )
                .nextAction(
                        Boolean.TRUE.equals(
                                profile.getIsOnboardingCompleted()
                        )
                                ? "GO_TO_DASHBOARD"
                                : "CONTINUE_ONBOARDING"
                )
                .statusMessage(
                        Boolean.TRUE.equals(
                                profile.getIsOnboardingCompleted()
                        )
                                ? "Onboarding completed"
                                : "Onboarding in progress"
                )
                .build();
    }

    private CommonBusinessProfile getOrCreateProfile(
            final Long userId
    ) {

        return commonBusinessProfileRepository
                .findByUserId(userId)
                .orElse(
                        CommonBusinessProfile.builder()
                                .userId(userId)
                                .completionPercentage(0)
                                .isDraftSaved(Boolean.TRUE)
                                .isOnboardingCompleted(Boolean.FALSE)
                                .build()
                );
    }
}
