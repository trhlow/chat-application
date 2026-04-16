package com.chatrealtime.exception;

import org.springframework.http.HttpStatus;

public class FriendRequestNotFoundException extends ApplicationException {
    public FriendRequestNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
