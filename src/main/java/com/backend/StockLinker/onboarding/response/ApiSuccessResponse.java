package com.backend.StockLinker.onboarding.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Standard API success response wrapper.
 *
 * @param <T> response data type
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiSuccessResponse<T> {

    private final Boolean success;

    private final String message;

    private final T data;

    private final LocalDateTime timestamp;
}