package com.backend.StockLinker.onboarding.validation.validator;

import com.backend.StockLinker.onboarding.validation.annotation.ValidPincode;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Validator for Indian pincode validation.
 */
public class PincodeValidator
        implements ConstraintValidator<ValidPincode, String> {

    private static final String PINCODE_REGEX =
            "^[1-9][0-9]{5}$";

    private static final Pattern PINCODE_PATTERN =
            Pattern.compile(PINCODE_REGEX);

    @Override
    public boolean isValid(
            final String value,
            final ConstraintValidatorContext context
    ) {

        if (!StringUtils.hasText(value)) {
            return false;
        }

        return PINCODE_PATTERN.matcher(value).matches();
    }
}
