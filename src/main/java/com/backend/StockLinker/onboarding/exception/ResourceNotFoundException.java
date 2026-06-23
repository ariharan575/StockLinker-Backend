package com.backend.StockLinker.onboarding.exception;

/**
 * Exception thrown when requested resource is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(final String message) {
        super(message);
    }
}
