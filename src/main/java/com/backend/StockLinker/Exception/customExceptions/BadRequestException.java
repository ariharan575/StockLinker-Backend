package com.backend.StockLinker.Exception.customExceptions;

import com.backend.StockLinker.Exception.BaseException;
import com.backend.StockLinker.Exception.ErrorCode;

public class BadRequestException extends BaseException {

    public BadRequestException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }
}
