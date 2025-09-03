package com.novara_demo.admin.model.exception;

public class FailedLoginException extends RuntimeException{
    public FailedLoginException(String message) {
        super(message);
    }
}
