package com.project.digitalwallet.common.advice;

import com.project.digitalwallet.common.exception.ExpiredJwtException;
import com.project.digitalwallet.common.exception.ResourceNotFoundException;
import com.project.digitalwallet.common.util.ErrorResponse;
import com.project.digitalwallet.common.util.ResponseWrapper;
import com.twilio.exception.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseWrapper<String>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ErrorResponse.buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ResponseWrapper<String>> handleIllegalStateException(IllegalStateException ex) {
        return ErrorResponse.buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseWrapper<String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ErrorResponse.buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ResponseWrapper<String>> handleApiException(ApiException ex) {
        return ErrorResponse.buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseWrapper<String>> handleValidationException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ErrorResponse.buildErrorResponse(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseWrapper<String>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return ErrorResponse.buildErrorResponse("Malformed JSON request body or invalid field format", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResponseWrapper<String>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String error = String.format("Parameter '%s' should be of type '%s'",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return ErrorResponse.buildErrorResponse(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResponseWrapper<String>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        return ErrorResponse.buildErrorResponse("Database constraint violation: duplicate record or invalid reference", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ResponseWrapper<String>> handleBadCredentialsException(BadCredentialsException ex) {
        return ErrorResponse.buildErrorResponse("Invalid username or password", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseWrapper<String>> handleAccessDeniedException(AccessDeniedException ex) {
        return ErrorResponse.buildErrorResponse("You do not have permission to perform this action", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ResponseWrapper<String>> handleJwtExpiredException(ExpiredJwtException ex) {
        return ErrorResponse.buildErrorResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseWrapper<String>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {

        return ErrorResponse.buildErrorResponse("Http method not supported", HttpStatus.BAD_REQUEST);

    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ResponseWrapper<String>> handleGlobalException(Exception ex) {
//        return ErrorResponse.buildErrorResponse("An unexpected error occurred. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR);
//    }
}