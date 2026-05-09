package com.backend.StockLinker.AuthService.exception;


import java.util.List;

public class ValidationException extends BaseException {

    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super(ErrorCode.VALIDATION_ERROR);
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
