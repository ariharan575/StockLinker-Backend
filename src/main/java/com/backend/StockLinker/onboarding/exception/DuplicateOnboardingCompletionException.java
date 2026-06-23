package com.backend.StockLinker.onboarding.exception;

/**
 * Exception thrown when onboarding is already completed.
 */
public class DuplicateOnboardingCompletionException
        extends RuntimeException {

    public DuplicateOnboardingCompletionException(
            final String message
    ) {

        super(message);
    }
}
