package com.example.reminder.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.reminder.entity.ReminderLog;
import com.example.reminder.entity.SysUser;
import com.example.reminder.service.ReminderLogService;
import com.example.reminder.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/log")
public class LogController {

    @Autowired
    private ReminderLogService logService;

    @GetMapping("/page")
    public Result<IPage<ReminderLog>> getLogPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "15") int pageSize,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String keyword,
            HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("currentUser");
        return Result.success(logService.getLogPage(
                pageNum, pageSize, taskId, keyword, user.getUsername(), user.getRole()));
    }
}
