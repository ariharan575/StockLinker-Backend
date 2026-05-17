package com.backend.StockLinker.AuthService.exception;

import lombok.Getter;
import java.util.List;

@Getter
public class ValidationException extends BaseException {

    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super(ErrorCode.VALIDATION_ERROR, "Validation failed: " + String.join(", ", errors));
        this.errors = errors;
    }

    public ValidationException(String error) {
        super(ErrorCode.VALIDATION_ERROR, error);
        this.errors = List.of(error);
    }
}