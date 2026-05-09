package com.backend.StockLinker.AuthService.exception.customExceptions;

import com.backend.StockLinker.AuthService.exception.BaseException;
import com.backend.StockLinker.AuthService.exception.ErrorCode;

public class ForbiddenException extends BaseException {
  public ForbiddenException(String message) {
    super(ErrorCode.FORBIDDEN, message);
  }
}
