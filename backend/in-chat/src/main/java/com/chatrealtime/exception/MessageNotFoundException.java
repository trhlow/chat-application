package com.chatrealtime.exception;

import org.springframework.http.HttpStatus;

public class MessageNotFoundException extends ApplicationException {
    public MessageNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}

