package com.example.reminder.exception;

public class ForbiddenException extends RuntimeException {

    public ForbiddenException() {
        super("无权操作该资源");
    }
}
