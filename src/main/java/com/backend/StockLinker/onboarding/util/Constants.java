package com.backend.StockLinker.onboarding.util;

/**
 * Application constants for onboarding module.
 */
public final class Constants {

    private Constants() {
    }

    public static final String API_BASE_PATH =
            "/api/onboarding";

    public static final String SUCCESS_STATUS =
            "SUCCESS";

    public static final String FAILURE_STATUS =
            "FAILURE";

    public static final Integer MAX_COMPLETION_PERCENTAGE =
            100;

    public static final Integer MIN_COMPLETION_PERCENTAGE =
            0;

    public static final String DEFAULT_COUNTRY =
            "India";

    public static final String SYSTEM_USER =
            "SYSTEM";

    public static final String AUTHORIZATION_HEADER =
            "Authorization";

    public static final String BEARER_PREFIX =
            "Bearer ";

    public static final String ROLE_WHOLESALER =
            "WHOLESALER";

    public static final String ROLE_SHOPKEEPER =
            "SHOPKEEPER";
}