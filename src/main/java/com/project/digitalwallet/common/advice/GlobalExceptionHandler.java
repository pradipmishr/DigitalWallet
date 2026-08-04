package com.project.digitalwallet.common.advice;

import com.project.digitalwallet.common.exception.ResourceNotFoundException;
import com.project.digitalwallet.common.util.ErrorResponse;
import com.project.digitalwallet.common.util.ResponseWrapper;
import com.twilio.exception.ApiException;
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
    @ExceptionHandler({IllegalStateException.class})
    public ResponseEntity<ResponseWrapper<String>> handleIllegalStateException(IllegalStateException illegalStateException){
        return ErrorResponse.buildErrorResponse(illegalStateException.getMessage(), HttpStatus.BAD_REQUEST);
    }
    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<ResponseWrapper<String>> handleIllegalArgumentException(IllegalArgumentException illegalArgumentException){
        return ErrorResponse.buildErrorResponse(illegalArgumentException.getMessage(),HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({ApiException.class})
    public ResponseEntity<ResponseWrapper<String>> handleApiException(ApiException apiException){
        return ErrorResponse.buildErrorResponse(apiException.getMessage(),HttpStatus.BAD_REQUEST);
    }

}
