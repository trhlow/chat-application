package com.chatrealtime.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApplicationException {
    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
