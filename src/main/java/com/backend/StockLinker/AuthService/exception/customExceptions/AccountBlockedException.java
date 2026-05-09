package com.backend.StockLinker.AuthService.exception.customExceptions;

import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;

public class AccountBlockedException extends BaseException {
    public AccountBlockedException() {
        super(ErrorCode.ACCOUNT_BLOCKED);
    }
}
