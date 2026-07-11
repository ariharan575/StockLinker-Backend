package com.backend.StockLinker.AuthService.exception;

import com.backend.StockLinker.AuthService.exception.customExceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

        log.error("BaseException occurred: {} - {}", code.name(), ex.getMessage());

        ApiError error = new ApiError(
                code.getStatus().value(),
                code.name(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, code.getStatus());
    }

    // =========================================================
    // ✅ BAD REQUEST EXCEPTION
    // =========================================================
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request
    ) {
        log.warn("Bad request: {}", ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.badRequest().body(error);
    }

    // =========================================================
    // ✅ CONFLICT EXCEPTION
    // =========================================================
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(
            ConflictException ex,
            HttpServletRequest request
    ) {
        log.warn("Conflict: {}", ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.CONFLICT.value(),
                "CONFLICT",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // =========================================================
    // ✅ FORBIDDEN EXCEPTION
    // =========================================================
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(
            ForbiddenException ex,
            HttpServletRequest request
    ) {
        log.warn("Forbidden access: {}", ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.FORBIDDEN.value(),
                "FORBIDDEN",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // =========================================================
    // ✅ INVALID TOKEN EXCEPTION
    // =========================================================
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiError> handleInvalidToken(
            InvalidTokenException ex,
            HttpServletRequest request
    ) {
        log.warn("Invalid token: {}", ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                "INVALID_TOKEN",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // =========================================================
    // ✅ RESOURCE NOT FOUND EXCEPTION
    // =========================================================
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Resource not found: {}", ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // =========================================================
    // ✅ TOKEN EXPIRED EXCEPTION
    // =========================================================
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ApiError> handleTokenExpired(
            TokenExpiredException ex,
            HttpServletRequest request
    ) {
        log.warn("Token expired: {}", ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                "TOKEN_EXPIRED",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // =========================================================
    // ✅ UNAUTHORIZED EXCEPTION
    // =========================================================
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(
            UnauthorizedException ex,
            HttpServletRequest request
    ) {
        log.warn("Unauthorized: {}", ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
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
    // ✅ SPRING SECURITY ACCESS DENIED
    // =========================================================
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        log.warn("Access denied: {}", ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_DENIED",
                "You don't have permission to access this resource",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // =========================================================
    // ✅ VALIDATION EXCEPTION
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

        log.warn("Validation failed: {}", errors);

        ApiError error = new ApiError(
                400,
                "VALIDATION_ERROR",
                "Validation failed",
                request.getRequestURI(),
                errors
        );

        return ResponseEntity.badRequest().body(error);
    }

    // =========================================================
    // ✅ RUNTIME EXCEPTION (FALLBACK)
    // =========================================================
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntime(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        log.error("Runtime error occurred: {}", ex.getMessage(), ex);

        ApiError error = new ApiError(
                500,
                "INTERNAL_ERROR",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(500).body(error);
    }

    // =========================================================
    // ✅ GLOBAL EXCEPTION (FALLBACK)
    // =========================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGlobal(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error occurred", ex);

        ApiError error = new ApiError(
                500,
                "INTERNAL_ERROR",
                "An unexpected error occurred: " + ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(500).body(error);
    }
}