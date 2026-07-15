package com.backend.StockLinker.AuthService.exception.customExceptions;

import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;

public class InvalidTokenException extends BaseException {

    public InvalidTokenException(String message) {
        super(ErrorCode.INVALID_TOKEN, message);
    }
}