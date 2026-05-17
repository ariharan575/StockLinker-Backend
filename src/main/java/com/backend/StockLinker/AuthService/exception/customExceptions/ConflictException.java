package com.backend.StockLinker.AuthService.exception.customExceptions;

import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;

public class ConflictException extends BaseException {

    public ConflictException() {
        super(ErrorCode.RESOURCE_ALREADY_EXISTS);
    }

    public ConflictException(String message) {
        super(ErrorCode.RESOURCE_ALREADY_EXISTS, message);
    }
}
