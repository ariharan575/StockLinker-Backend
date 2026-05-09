package com.backend.StockLinker.AuthService.exception.customExceptions;

import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;

public class OAuthException extends BaseException {
    public OAuthException() {
        super(ErrorCode.OAUTH_FAILED);
    }
}
