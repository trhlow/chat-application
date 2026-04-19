package com.chatrealtime.exception;

import org.springframework.http.HttpStatus;

public class ExistsUsernameException extends ApplicationException {
    public ExistsUsernameException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}

