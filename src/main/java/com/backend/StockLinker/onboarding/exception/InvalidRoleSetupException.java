package com.backend.StockLinker.onboarding.exception;

/**
 * Exception thrown for invalid role-based onboarding access.
 */
public class InvalidRoleSetupException
        extends RuntimeException {

    public InvalidRoleSetupException(
            final String message
    ) {

        super(message);
    }
}
