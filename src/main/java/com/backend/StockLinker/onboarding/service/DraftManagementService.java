package com.backend.StockLinker.onboarding.service;

import com.backend.StockLinker.onboarding.entity.CommonBusinessProfile;

/**
 * Service contract for onboarding draft management.
 */
public interface DraftManagementService {

    /**
     * Mark onboarding draft as saved.
     *
     * @param profile business profile
     */
    void markDraftSaved(CommonBusinessProfile profile);

    /**
     * Mark onboarding draft as completed.
     *
     * @param profile business profile
     */
    void clearDraft(CommonBusinessProfile profile);

    /**
     * Resume onboarding draft.
     *
     * @param userId authenticated user id
     * @return business profile
     */
    CommonBusinessProfile resumeDraft(Long userId);
}
