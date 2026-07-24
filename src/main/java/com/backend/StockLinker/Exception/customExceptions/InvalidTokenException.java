package com.backend.StockLinker.Exception.customExceptions;

import com.backend.StockLinker.Exception.BaseException;
import com.backend.StockLinker.Exception.ErrorCode;

public class InvalidTokenException extends BaseException {

    public InvalidTokenException(String message) {
        super(ErrorCode.INVALID_TOKEN, message);
    }
}