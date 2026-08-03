package com.project.digitalwallet.common.advice;

import com.project.digitalwallet.common.exception.ResourceNotFoundException;
import com.project.digitalwallet.common.util.ErrorResponse;
import com.project.digitalwallet.common.util.ResponseWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({ResourceNotFoundException.class})
    public ResponseEntity<ResponseWrapper<String>> handleResourceNotFoundException(ResourceNotFoundException resourceNotFoundException) {
        return ErrorResponse.buildErrorResponse(resourceNotFoundException.getMessage(), HttpStatus.NOT_FOUND);
    }
}
