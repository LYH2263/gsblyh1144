package com.example.reminder.exception;

/**
 * 越权访问异常。归属/角色校验统一在 Service 层抛出，
 * 由 GlobalExceptionHandler 统一转换为 code=403、message=无权操作该资源。
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException() {
        super("无权操作该资源");
    }
}
