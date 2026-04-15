package com.chatrealtime.exception;

import org.springframework.http.HttpStatus;

public class FriendshipNotFoundException extends ApplicationException {
    public FriendshipNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
