package com.backend.StockLinker.onboarding.validation.validator;

import com.backend.StockLinker.onboarding.validation.annotation.ValidMobileNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Validator for Indian mobile numbers.
 */
public class MobileNumberValidator
        implements ConstraintValidator<ValidMobileNumber, String> {

    private static final String MOBILE_REGEX =
            "^[6-9][0-9]{9}$";

    private static final Pattern MOBILE_PATTERN =
            Pattern.compile(MOBILE_REGEX);

    @Override
    public boolean isValid(
            final String value,
            final ConstraintValidatorContext context
    ) {

        if (!StringUtils.hasText(value)) {
            return false;
        }

        return MOBILE_PATTERN.matcher(value).matches();
    }
}
