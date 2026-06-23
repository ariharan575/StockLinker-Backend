package com.backend.StockLinker.onboarding.exception;

/**
 * Exception thrown for invalid onboarding step transitions.
 */
public class InvalidOnboardingStepException
        extends RuntimeException {

    public InvalidOnboardingStepException(
            final String message
    ) {

        super(message);
    }
}
