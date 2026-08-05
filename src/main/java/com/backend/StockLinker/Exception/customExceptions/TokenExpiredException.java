package com.backend.StockLinker.Exception.customExceptions;

import com.backend.StockLinker.Exception.BaseException;
import com.backend.StockLinker.Exception.ErrorCode;

public class TokenExpiredException extends BaseException {

    public TokenExpiredException(String message) {
        super(ErrorCode.TOKEN_EXPIRED, message);
    }
}