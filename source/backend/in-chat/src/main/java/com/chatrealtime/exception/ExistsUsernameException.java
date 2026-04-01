package com.chatrealtime.exception;

public class ExistsUsernameException extends ApplicationException{
    public ExistsUsernameException(String message){
        super(message, HttpStatus.ExistsUsername);
    }
}