package com.backend.StockLinker.onboarding.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard API error response wrapper.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private final Boolean success;

    private final String errorCode;

    private final String message;

    private final Map<String, String> errors;

    private final LocalDateTime timestamp;
}
