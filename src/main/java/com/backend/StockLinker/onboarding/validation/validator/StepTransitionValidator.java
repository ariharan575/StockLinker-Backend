package com.backend.StockLinker.onboarding.validation.validator;

import com.backend.StockLinker.onboarding.validation.annotation.ValidStepTransition;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for onboarding step transitions.
 */
public class StepTransitionValidator
        implements ConstraintValidator<ValidStepTransition, Object> {

    @Override
    public boolean isValid(
            final Object value,
            final ConstraintValidatorContext context
    ) {

        return value != null;
    }
}

