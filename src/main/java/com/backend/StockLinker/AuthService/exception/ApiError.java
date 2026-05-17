package com.backend.StockLinker.AuthService.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private final String errorId;
    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private List<String> validationErrors;

    public ApiError(int status, String error, String message, String path) {
        this.errorId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public ApiError(int status, String error, String message, String path, List<String> validationErrors) {
        this(status, error, message, path);
        this.validationErrors = validationErrors;
    }
}