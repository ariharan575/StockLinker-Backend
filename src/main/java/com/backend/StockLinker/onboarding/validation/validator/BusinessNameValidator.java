package com.backend.StockLinker.onboarding.validation.validator;

import com.backend.StockLinker.onboarding.validation.annotation.ValidBusinessName;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Validator for business name validation.
 */
public class BusinessNameValidator
        implements ConstraintValidator<ValidBusinessName, String> {

    private static final String BUSINESS_NAME_REGEX =
            "^[a-zA-Z0-9&.,()\\-\\s]{3,150}$";

    private static final Pattern BUSINESS_NAME_PATTERN =
            Pattern.compile(BUSINESS_NAME_REGEX);

    @Override
    public boolean isValid(
            final String value,
            final ConstraintValidatorContext context
    ) {

        if (!StringUtils.hasText(value)) {
            return false;
        }

        return BUSINESS_NAME_PATTERN.matcher(value)
                .matches();
    }
}
