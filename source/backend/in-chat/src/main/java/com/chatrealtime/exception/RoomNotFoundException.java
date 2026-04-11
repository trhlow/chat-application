package com.chatrealtime.exception;

import org.springframework.http.HttpStatus;

public class RoomNotFoundException extends ApplicationException {
    public RoomNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}

