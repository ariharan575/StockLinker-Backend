package com.backend.StockLinker.Exception.customExceptions;

import com.backend.StockLinker.Exception.BaseException;
import com.backend.StockLinker.Exception.ErrorCode;

public class ConflictException extends BaseException {

    public ConflictException(String message) {
        super(ErrorCode.RESOURCE_ALREADY_EXISTS, message);
    }
}
