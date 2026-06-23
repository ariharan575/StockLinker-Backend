package com.backend.StockLinker.onboarding.service.impl;

import com.backend.StockLinker.onboarding.entity.CommonBusinessProfile;
import com.backend.StockLinker.onboarding.exception.DraftRecoveryException;
import com.backend.StockLinker.onboarding.repository.CommonBusinessProfileRepository;
import com.backend.StockLinker.onboarding.service.DraftManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation for onboarding draft management.
 */
@Service
@RequiredArgsConstructor
public class DraftManagementServiceImpl
        implements DraftManagementService {

    private final CommonBusinessProfileRepository
            commonBusinessProfileRepository;

    /**
     * Mark onboarding draft as saved.
     *
     * @param profile business profile
     */
    @Override
    @Transactional
    public void markDraftSaved(final CommonBusinessProfile profile) {

        profile.setIsDraftSaved(Boolean.TRUE);

        commonBusinessProfileRepository.save(profile);
    }

    /**
     * Clear draft after onboarding completion.
     *
     * @param profile business profile
     */
    @Override
    @Transactional
    public void clearDraft(final CommonBusinessProfile profile) {

        profile.setIsDraftSaved(Boolean.FALSE);

        commonBusinessProfileRepository.save(profile);
    }

    /**
     * Resume onboarding draft.
     *
     * @param userId authenticated user id
     * @return business profile
     */
    @Override
    @Transactional(readOnly = true)
    public CommonBusinessProfile resumeDraft(final Long userId) {

        return commonBusinessProfileRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new DraftRecoveryException(
                                "No onboarding draft found for user id: "
                                        + userId
                        )
                );
    }
}
