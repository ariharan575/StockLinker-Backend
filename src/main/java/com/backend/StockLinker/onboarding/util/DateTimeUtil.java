package com.backend.StockLinker.onboarding.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for date-time operations.
 */
public final class DateTimeUtil {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
            );

    private DateTimeUtil() {
    }

    /**
     * Get current local date-time.
     *
     * @return current date-time
     */
    public static LocalDateTime now() {

        return LocalDateTime.now();
    }

    /**
     * Format local date-time.
     *
     * @param localDateTime date-time
     * @return formatted date-time
     */
    public static String format(
            final LocalDateTime localDateTime
    ) {

        return DATE_TIME_FORMATTER.format(
                localDateTime
        );
    }
}