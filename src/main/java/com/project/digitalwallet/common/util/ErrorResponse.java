package com.project.digitalwallet.common.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ErrorResponse {
    public static ResponseEntity<ResponseWrapper<String>> buildErrorResponse(String message, HttpStatus status){
        ResponseWrapper<String> responseWrapper = new ResponseWrapper<>(null,message,status.value(),false);
        return ResponseEntity.status(status).body(responseWrapper);
    }

    public static <T> ResponseEntity<ResponseWrapper<T>> buildErrorResponse(T data,String message,HttpStatus status){
        ResponseWrapper<T> responseWrapper=new ResponseWrapper<>(data,message,status.value(),false);
        return ResponseEntity.status(status).body(responseWrapper);
    }
}