package com.example.reminder.exception;

public class NonBusinessTableException extends RuntimeException {
    public NonBusinessTableException(String message) {
        super(message);
    }
}
