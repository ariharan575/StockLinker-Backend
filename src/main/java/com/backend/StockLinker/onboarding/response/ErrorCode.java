package com.backend.StockLinker.onboarding.response;

/**
 * Standard error codes for onboarding module.
 */
public enum ErrorCode {

    VALIDATION_ERROR,
    INVALID_STEP,
    RESOURCE_NOT_FOUND,
    INVALID_ROLE_ACCESS,
    UNAUTHORIZED_ACCESS,
    DRAFT_RECOVERY_FAILED,
    ONBOARDING_ALREADY_COMPLETED,
    CONSTRAINT_VIOLATION,
    INTERNAL_SERVER_ERROR
}
