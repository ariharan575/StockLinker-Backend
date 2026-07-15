package com.backend.StockLinker.AuthService.exception.customExceptions;

import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;

public class TokenExpiredException extends BaseException {

    public TokenExpiredException(String message) {
        super(ErrorCode.TOKEN_EXPIRED, message);
    }
}