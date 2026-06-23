package com.backend.StockLinker.onboarding.exception;

import com.backend.StockLinker.onboarding.response.ApiErrorResponse;
import com.backend.StockLinker.onboarding.response.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for onboarding module.
 */
@RestControllerAdvice
public class OnboadingGlobalExceptionHandler {

    /**
     * Handle validation errors.
     *
     * @param exception validation exception
     * @return error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            final MethodArgumentNotValidException exception
    ) {

        Map<String, String> validationErrors =
                new HashMap<>();

        exception.getBindingResult()
                .getAllErrors()
                .forEach(error -> {

                    String fieldName =
                            ((FieldError) error).getField();

                    String errorMessage =
                            error.getDefaultMessage();

                    validationErrors.put(
                            fieldName,
                            errorMessage
                    );
                });

        ApiErrorResponse response =
                ApiErrorResponse.builder()
                        .success(false)
                        .errorCode(
                                ErrorCode.VALIDATION_ERROR
                                        .name()
                        )
                        .message(
                                "Validation failed"
                        )
                        .errors(validationErrors)
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle onboarding validation exceptions.
     *
     * @param exception validation exception
     * @return error response
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse>
    handleBusinessValidationException(
            final ValidationException exception
    ) {

        ApiErrorResponse response =
                ApiErrorResponse.builder()
                        .success(false)
                        .errorCode(
                                ErrorCode.VALIDATION_ERROR
                                        .name()
                        )
                        .message(exception.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle invalid onboarding step exceptions.
     *
     * @param exception invalid onboarding step exception
     * @return error response
     */
    @ExceptionHandler(
            InvalidOnboardingStepException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleInvalidStepException(
            final InvalidOnboardingStepException exception
    ) {

        ApiErrorResponse response =
                ApiErrorResponse.builder()
                        .success(false)
                        .errorCode(
                                ErrorCode.INVALID_STEP
                                        .name()
                        )
                        .message(exception.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle duplicate onboarding completion exceptions.
     *
     * @param exception duplicate completion exception
     * @return error response
     */
    @ExceptionHandler(
            DuplicateOnboardingCompletionException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleDuplicateCompletionException(
            final DuplicateOnboardingCompletionException
                    exception
    ) {

        ApiErrorResponse response =
                ApiErrorResponse.builder()
                        .success(false)
                        .errorCode(
                                ErrorCode.ONBOARDING_ALREADY_COMPLETED
                                        .name()
                        )
                        .message(exception.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    /**
     * Handle resource not found exceptions.
     *
     * @param exception resource not found exception
     * @return error response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse>
    handleResourceNotFoundException(
            final ResourceNotFoundException exception
    ) {

        ApiErrorResponse response =
                ApiErrorResponse.builder()
                        .success(false)
                        .errorCode(
                                ErrorCode.RESOURCE_NOT_FOUND
                                        .name()
                        )
                        .message(exception.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    /**
     * Handle role access exceptions.
     *
     * @param exception invalid role exception
     * @return error response
     */
    @ExceptionHandler(
            InvalidRoleSetupException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleRoleValidationException(
            final InvalidRoleSetupException exception
    ) {

        ApiErrorResponse response =
                ApiErrorResponse.builder()
                        .success(false)
                        .errorCode(
                                ErrorCode.INVALID_ROLE_ACCESS
                                        .name()
                        )
                        .message(exception.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }

    /**
     * Handle unauthorized access exceptions.
     *
     * @param exception unauthorized access exception
     * @return error response
     */
    @ExceptionHandler(
            UnauthorizedOnboardingAccessException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleUnauthorizedException(
            final UnauthorizedOnboardingAccessException
                    exception
    ) {

        ApiErrorResponse response =
                ApiErrorResponse.builder()
                        .success(false)
                        .errorCode(
                                ErrorCode.UNAUTHORIZED_ACCESS
                                        .name()
                        )
                        .message(exception.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }

    /**
     * Handle draft recovery exceptions.
     *
     * @param exception draft recovery exception
     * @return error response
     */
    @ExceptionHandler(DraftRecoveryException.class)
    public ResponseEntity<ApiErrorResponse>
    handleDraftRecoveryException(
            final DraftRecoveryException exception
    ) {

        ApiErrorResponse response =
                ApiErrorResponse.builder()
                        .success(false)
                        .errorCode(
                                ErrorCode.DRAFT_RECOVERY_FAILED
                                        .name()
                        )
                        .message(exception.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    /**
     * Handle constraint violation exceptions.
     *
     * @param exception constraint violation exception
     * @return error response
     */
    @ExceptionHandler(
            ConstraintViolationException.class
    )
    public ResponseEntity<ApiErrorResponse>
    handleConstraintViolationException(
            final ConstraintViolationException exception
    ) {

        ApiErrorResponse response =
                ApiErrorResponse.builder()
                        .success(false)
                        .errorCode(
                                ErrorCode.CONSTRAINT_VIOLATION
                                        .name()
                        )
                        .message(exception.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle unexpected exceptions.
     *
     * @param exception generic exception
     * @return error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse>
    handleGenericException(
            final Exception exception
    ) {

        ApiErrorResponse response =
                ApiErrorResponse.builder()
                        .success(false)
                        .errorCode(
                                ErrorCode.INTERNAL_SERVER_ERROR
                                        .name()
                        )
                        .message(
                                "Unexpected internal server error"
                        )
                        .timestamp(LocalDateTime.now())
                        .build();

        return ResponseEntity
                .status(
                        HttpStatus.INTERNAL_SERVER_ERROR
                )
                .body(response);
    }
}