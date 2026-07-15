package com.backend.StockLinker.AuthService.exception;

import com.backend.StockLinker.AuthService.exception.customExceptions.*;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // =========================================================
    // ✅ CUSTOM BASE EXCEPTION HANDLER
    // =========================================================
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiError> handleBaseException(
            BaseException ex,
            HttpServletRequest request
    ) {
        ErrorCode code = ex.getErrorCode();

        log.error("BaseException occurred: {} - {} - Path: {}",
                code.name(), ex.getMessage(), request.getRequestURI());

        ApiError error = new ApiError(
                code.getStatus().value(),
                code.name(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, code.getStatus());
    }

    // =========================================================
    // ✅ VALIDATION EXCEPTION (MethodArgumentNotValid)
    // =========================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.toList());

        // Log with details
        log.warn("Validation failed for request to {}: {}", request.getRequestURI(), errors);

        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Validation failed: " + String.join("; ", errors),
                request.getRequestURI(),
                errors
        );

        return ResponseEntity.badRequest().body(error);
    }

    // =========================================================
    // ✅ CONSTRAINT VIOLATION EXCEPTION
    // =========================================================
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.toList());

        log.warn("Constraint violation: {}", errors);

        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "CONSTRAINT_VIOLATION",
                "Validation failed: " + String.join("; ", errors),
                request.getRequestURI(),
                errors
        );

        return ResponseEntity.badRequest().body(error);
    }

    // =========================================================
    // ✅ HTTP MESSAGE NOT READABLE (Malformed JSON)
    // =========================================================
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        String message = "Malformed JSON request body";

        if (ex.getCause() instanceof InvalidFormatException) {
            InvalidFormatException ife = (InvalidFormatException) ex.getCause();
            message = String.format("Invalid format for field '%s': expected %s",
                    ife.getTargetType().getSimpleName());
        }

        log.warn("Malformed request: {}", message);

        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "MALFORMED_REQUEST",
                message,
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(error);
    }

    // =========================================================
    // ✅ MISSING REQUEST COOKIE
    // =========================================================
    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ApiError> handleMissingCookie(
            MissingRequestCookieException ex,
            HttpServletRequest request
    ) {
        log.warn("Missing required cookie: {}", ex.getCookieName());

        ApiError error = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                "MISSING_COOKIE",
                "Required cookie '" + ex.getCookieName() + "' is missing",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // =========================================================
    // ✅ METHOD ARGUMENT TYPE MISMATCH
    // =========================================================
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String message = ex.getRequiredType() != null
                ? ex.getRequiredType().getSimpleName()
                : "unknown";

        log.warn("Type mismatch: {}", message);

        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "TYPE_MISMATCH",
                message,
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(error);
    }

    // =========================================================
    // ✅ DATA INTEGRITY VIOLATION
    // =========================================================
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        String message = "Database integrity violation";
        Throwable cause = ex.getMostSpecificCause();

        if (cause instanceof SQLException) {
            SQLException sqlEx = (SQLException) cause;
            if (sqlEx.getMessage().contains("Duplicate entry")) {
                message = "Duplicate entry detected - record already exists";
            } else if (sqlEx.getMessage().contains("foreign key constraint")) {
                message = "Referenced record not found";
            }
        }

        log.warn("Data integrity violation: {}", message, ex);

        ApiError error = new ApiError(
                HttpStatus.CONFLICT.value(),
                "DATA_INTEGRITY_VIOLATION",
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // =========================================================
    // ✅ DATA ACCESS EXCEPTION
    // =========================================================
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> handleDataAccess(
            DataAccessException ex,
            HttpServletRequest request
    ) {
        log.error("Data access error: {}", ex.getMessage(), ex);

        ApiError error = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "DATA_ACCESS_ERROR",
                "Database access error occurred. Please try again later.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // =========================================================
    // ✅ AUTHENTICATION EXCEPTION
    // =========================================================
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        log.warn("Authentication failed: {}", ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                "AUTHENTICATION_FAILED",
                "Authentication failed: " + ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // =========================================================
    // ✅ ACCESS DENIED EXCEPTION
    // =========================================================
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        log.warn("Access denied to {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_DENIED",
                "You don't have permission to access this resource",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // =========================================================
    // ✅ ILLEGAL ARGUMENT EXCEPTION
    // =========================================================
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        log.warn("Illegal argument: {}", ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(error);
    }

    // =========================================================
    // ✅ MAX UPLOAD SIZE EXCEEDED
    // =========================================================
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        log.warn("File upload size exceeded: {}", ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "PAYLOAD_TOO_LARGE",
                "File size exceeds the maximum allowed limit",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    // =========================================================
    // ✅ GLOBAL EXCEPTION (FALLBACK)
    // =========================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGlobal(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error occurred: {} - Path: {}", ex.getMessage(), request.getRequestURI(), ex);

        // Production: Don't expose stack traces
        String message = "An unexpected error occurred. Please try again later.";
        if (request.getAttribute("debug") != null) {
            message = ex.getMessage();
        }

        ApiError error = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}