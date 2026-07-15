package com.backend.StockLinker.AuthService.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Generic
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation failed"),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Unauthorized"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied"),

    // Token
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token expired"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid token"),
    REFRESH_TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "RefreshToken Revoked"),

    // OTP
    OTP_INVALID(HttpStatus.BAD_REQUEST, "Invalid OTP"),
    OTP_EXPIRED(HttpStatus.BAD_REQUEST, "OTP expired"),

    // OAuth
    OAUTH_FAILED(HttpStatus.UNAUTHORIZED, "OAuth authentication failed"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "User already exists"),
    ACCOUNT_BLOCKED(HttpStatus.FORBIDDEN, "Account is blocked"),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "Account is locked due to multiple failed attempts"),

    // Role & Permission
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Role not found"),

    PERMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Permission not found"),
    INSUFFICIENT_PERMISSIONS(HttpStatus.FORBIDDEN, "Insufficient permissions"),
    ROLE_ALREADY_ASSIGNED(HttpStatus.CONFLICT, "Role already assigned to user"),
    INVALID_ROLE_OPERATION(HttpStatus.BAD_REQUEST, "Invalid role operation"),

    // Device
    DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "Device not found"),
    DEVICE_MISMATCH(HttpStatus.FORBIDDEN, "Device mismatch"),

    // Resource
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    RESOURCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Resource already exists"),

    // Rate limiting
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Too many requests");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}