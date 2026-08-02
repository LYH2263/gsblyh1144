package com.example.reminder.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.reminder.entity.ReminderLog;
import com.example.reminder.mapper.ReminderLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReminderLogService {

    @Autowired
    private ReminderLogMapper logMapper;

    /**
     * 分页查询日志（按任务创建人隔离：ADMIN 可看全部，USER 仅能看自己创建任务的日志）
     */
    public IPage<ReminderLog> getLogPage(int pageNum, int pageSize, Long taskId, String keyword,
                                         String currentUser, String role) {
        Page<ReminderLog> page = new Page<>(pageNum, pageSize);
        QueryWrapper<ReminderLog> wrapper = new QueryWrapper<>();

        // 非管理员只能查看自己创建任务的发送日志
        if (!"ADMIN".equals(role)) {
            wrapper.apply("task_id IN (SELECT id FROM reminder_task WHERE create_user = {0})", currentUser);
        }

        if (taskId != null) {
            wrapper.eq("task_id", taskId);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("task_name", keyword)
                    .or().like("receiver", keyword)
                    .or().like("send_content", keyword));
        }

        wrapper.orderByDesc("send_time");
        return logMapper.selectPage(page, wrapper);
    }
}
