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
     * 分页查询日志
     */
    public IPage<ReminderLog> getLogPage(int pageNum, int pageSize, Long taskId, String keyword) {
        Page<ReminderLog> page = new Page<>(pageNum, pageSize);
        QueryWrapper<ReminderLog> wrapper = new QueryWrapper<>();

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
