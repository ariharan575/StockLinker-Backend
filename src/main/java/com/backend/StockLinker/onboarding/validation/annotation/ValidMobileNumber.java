package com.backend.StockLinker.onboarding.validation.annotation;

import com.backend.StockLinker.onboarding.validation.validator.MobileNumberValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Custom validation annotation for mobile numbers.
 */
@Documented
@Constraint(validatedBy = MobileNumberValidator.class)
@Target({FIELD})
@Retention(RUNTIME)
public @interface ValidMobileNumber {

    String message() default "Invalid mobile number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}