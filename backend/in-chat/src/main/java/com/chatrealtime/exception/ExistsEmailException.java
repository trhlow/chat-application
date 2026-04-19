package com.chatrealtime.exception;

import org.springframework.http.HttpStatus;

public class ExistsEmailException extends ApplicationException {
    public ExistsEmailException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}


