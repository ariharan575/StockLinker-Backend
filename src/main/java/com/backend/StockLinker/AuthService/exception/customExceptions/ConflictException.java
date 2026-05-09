package com.backend.StockLinker.AuthService.exception.customExceptions;

import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;

public class ConflictException extends BaseException {
    public ConflictException(String message) {
        super(ErrorCode.USER_ALREADY_EXISTS, message);
    }
}
