package com.backend.StockLinker.onboarding.util;

import com.backend.StockLinker.onboarding.entity.CommonBusinessProfile;
import org.springframework.util.StringUtils;

/**
 * Utility class for onboarding completion calculation.
 */
public final class CompletionPercentageUtil {

    private CompletionPercentageUtil() {
    }

    /**
     * Calculate completion percentage.
     *
     * @param profile business profile
     * @return completion percentage
     */
    public static int calculatePercentage(
            final CommonBusinessProfile profile
    ) {

        int completedFields = 0;
        int totalFields = 16;

        if (profile.getPreferredLanguage() != null) {
            completedFields++;
        }

        if (StringUtils.hasText(profile.getOwnerName())) {
            completedFields++;
        }

        if (StringUtils.hasText(profile.getBusinessName())) {
            completedFields++;
        }

        if (profile.getBusinessType() != null) {
            completedFields++;
        }

        if (StringUtils.hasText(profile.getBusinessCategory())) {
            completedFields++;
        }

        if (StringUtils.hasText(profile.getMobile())) {
            completedFields++;
        }

        if (StringUtils.hasText(profile.getEmail())) {
            completedFields++;
        }

        if (StringUtils.hasText(profile.getAddressLine1())) {
            completedFields++;
        }

        if (StringUtils.hasText(profile.getCity())) {
            completedFields++;
        }

        if (StringUtils.hasText(profile.getDistrict())) {
            completedFields++;
        }

        if (StringUtils.hasText(profile.getState())) {
            completedFields++;
        }

        if (StringUtils.hasText(profile.getCountry())) {
            completedFields++;
        }

        if (StringUtils.hasText(profile.getPincode())) {
            completedFields++;
        }

        if (profile.getLatitude() != null) {
            completedFields++;
        }

        if (profile.getLongitude() != null) {
            completedFields++;
        }

        if (Boolean.TRUE.equals(
                profile.getIsOnboardingCompleted()
        )) {

            completedFields++;
        }

        return (completedFields * 100) / totalFields;
    }
}
