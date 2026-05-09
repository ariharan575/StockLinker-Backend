package com.backend.StockLinker.AuthService.exception.customExceptions;

import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;

public class OtpVerificationException extends BaseException {
    public OtpVerificationException(String message) {
        super(ErrorCode.OTP_INVALID, message);
    }
}
