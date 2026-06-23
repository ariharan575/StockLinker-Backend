package com.backend.StockLinker.onboarding.validation.annotation;

import com.backend.StockLinker.onboarding.validation.validator.BusinessNameValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Custom validation annotation for business name validation.
 */
@Documented
@Constraint(validatedBy = BusinessNameValidator.class)
@Target({FIELD})
@Retention(RUNTIME)
public @interface ValidBusinessName {

    String message() default "Invalid business name";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
