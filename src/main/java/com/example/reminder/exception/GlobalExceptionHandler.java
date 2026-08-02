package com.example.reminder.exception;

import com.example.reminder.util.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：将越权异常统一转换为 code=403 的响应，
 * 避免各 Controller 方法各自复制角色判断与错误码。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public Result<?> handleAccessDenied(AccessDeniedException e) {
        return Result.error(403, "无权操作该资源");
    }
}
