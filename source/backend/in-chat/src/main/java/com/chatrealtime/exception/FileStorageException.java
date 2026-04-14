package com.chatrealtime.exception;

import org.springframework.http.HttpStatus;

public class FileStorageException extends ApplicationException {
    public FileStorageException(String message) {
        super(message, HttpStatus.BAD_GATEWAY);
    }
}
