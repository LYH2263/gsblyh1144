package com.example.reminder.exception;

import com.example.reminder.util.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ForbiddenException.class)
    public Result<?> handleForbidden(ForbiddenException e) {
        return Result.error(403, e.getMessage());
    }

    @ExceptionHandler(NonBusinessTableException.class)
    public Result<?> handleNonBusinessTable(NonBusinessTableException e) {
        return Result.error(400, e.getMessage());
    }
}
