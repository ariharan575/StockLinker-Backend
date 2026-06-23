package com.backend.StockLinker.onboarding.exception;

/**
 * Exception thrown for unauthorized onboarding access.
 */
public class UnauthorizedOnboardingAccessException
        extends RuntimeException {

    public UnauthorizedOnboardingAccessException(
            final String message
    ) {

        super(message);
    }
}