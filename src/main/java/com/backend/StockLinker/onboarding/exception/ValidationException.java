package com.backend.StockLinker.onboarding.exception;

/**
 * Exception thrown for onboarding validation failures.
 */
public class ValidationException
        extends RuntimeException {

    public ValidationException(
            final String message
    ) {

        super(message);
    }
}