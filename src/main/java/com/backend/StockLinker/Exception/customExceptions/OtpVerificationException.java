package com.backend.StockLinker.Exception.customExceptions;

import com.backend.StockLinker.Exception.BaseException;
import com.backend.StockLinker.Exception.ErrorCode;

public class OtpVerificationException extends BaseException {
    public OtpVerificationException(String message) {
        super(ErrorCode.OTP_INVALID, message);
    }
}
