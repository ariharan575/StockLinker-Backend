package com.backend.StockLinker.onboarding.validation.annotation;

import com.backend.StockLinker.onboarding.validation.validator.PincodeValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Custom validation annotation for Indian pincode validation.
 */
@Documented
@Constraint(validatedBy = PincodeValidator.class)
@Target({FIELD})
@Retention(RUNTIME)
public @interface ValidPincode {

    String message() default "Invalid pincode";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
