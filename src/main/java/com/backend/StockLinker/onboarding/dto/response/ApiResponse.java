package com.backend.StockLinker.onboarding.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Generic API response wrapper.
 *
 * @param <T> response payload type
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;

    private final String message;

    private final T data;

    private final LocalDateTime timestamp;
}