package com.chatrealtime.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends ApplicationException {
    public BusinessException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_CONTENT);
    }
}
