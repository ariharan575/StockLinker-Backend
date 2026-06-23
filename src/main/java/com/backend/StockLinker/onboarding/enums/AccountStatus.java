package com.backend.StockLinker.onboarding.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents account lifecycle status.
 */
@Getter
@RequiredArgsConstructor
public enum AccountStatus {

    ACTIVE("Active"),
    INACTIVE("Inactive"),
    BLOCKED("Blocked"),
    SUSPENDED("Suspended"),
    PENDING_APPROVAL("Pending Approval");

    private final String displayName;
}