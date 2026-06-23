package com.backend.StockLinker.onboarding.validation.annotation;

import com.backend.StockLinker.onboarding.validation.validator.StepTransitionValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Custom validation annotation for onboarding step validation.
 */
@Documented
@Constraint(validatedBy = StepTransitionValidator.class)
@Target({TYPE})
@Retention(RUNTIME)
public @interface ValidStepTransition {

    String message() default "Invalid onboarding step transition";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}