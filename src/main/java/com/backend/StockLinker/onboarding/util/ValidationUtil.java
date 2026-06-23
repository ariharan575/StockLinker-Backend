package com.backend.StockLinker.onboarding.util;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Utility class for reusable validation logic.
 */
public final class ValidationUtil {

    private ValidationUtil() {
    }

    private static final String EMAIL_REGEX =
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    private static final String MOBILE_REGEX =
            "^[6-9][0-9]{9}$";

    private static final String PINCODE_REGEX =
            "^[1-9][0-9]{5}$";

    /**
     * Validate email format.
     *
     * @param email email value
     * @return validation status
     */
    public static boolean isValidEmail(
            final String email
    ) {

        if (!StringUtils.hasText(email)) {
            return false;
        }

        return Pattern.compile(EMAIL_REGEX)
                .matcher(email)
                .matches();
    }

    /**
     * Validate Indian mobile number.
     *
     * @param mobile mobile value
     * @return validation status
     */
    public static boolean isValidMobile(
            final String mobile
    ) {

        if (!StringUtils.hasText(mobile)) {
            return false;
        }

        return Pattern.compile(MOBILE_REGEX)
                .matcher(mobile)
                .matches();
    }

    /**
     * Validate Indian pincode.
     *
     * @param pincode pincode value
     * @return validation status
     */
    public static boolean isValidPincode(
            final String pincode
    ) {

        if (!StringUtils.hasText(pincode)) {
            return false;
        }

        return Pattern.compile(PINCODE_REGEX)
                .matcher(pincode)
                .matches();
    }
}