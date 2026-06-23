package com.backend.StockLinker.onboarding.exception;

/**
 * Exception thrown when onboarding draft recovery fails.
 */
public class DraftRecoveryException
        extends RuntimeException {

    public DraftRecoveryException(
            final String message
    ) {

        super(message);
    }
}