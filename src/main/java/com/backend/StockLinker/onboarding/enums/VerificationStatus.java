package com.backend.StockLinker.onboarding.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents verification lifecycle status.
 */
@Getter
@RequiredArgsConstructor
public enum VerificationStatus {

    PENDING("Pending"),
    UNDER_REVIEW("Under Review"),
    VERIFIED("Verified"),
    REJECTED("Rejected"),
    SUSPENDED("Suspended");

    private final String displayName;
}