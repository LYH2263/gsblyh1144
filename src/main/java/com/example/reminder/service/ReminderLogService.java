package com.example.reminder.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.reminder.entity.ReminderLog;
import com.example.reminder.entity.ReminderTask;
import com.example.reminder.mapper.ReminderLogMapper;
import com.example.reminder.mapper.ReminderTaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReminderLogService {

    @Autowired
    private ReminderLogMapper logMapper;

    @Autowired
    private ReminderTaskMapper taskMapper;

    /**
     * 分页查询日志。
     * 发送日志按任务创建人隔离：ADMIN 可见全部；USER 仅可见自己创建的任务所产生的日志。
     * reminder_log 表无 create_user 列，故通过 reminder_task.create_user 反查任务ID集合再过滤。
     */
    public IPage<ReminderLog> getLogPage(int pageNum, int pageSize, Long taskId, String keyword,
                                         String currentUser, String role) {
        Page<ReminderLog> page = new Page<>(pageNum, pageSize);
        QueryWrapper<ReminderLog> wrapper = new QueryWrapper<>();

        // 非管理员只能查看自己创建任务的日志
        if (!"ADMIN".equals(role)) {
            List<ReminderTask> ownTasks = taskMapper.selectList(
                    new QueryWrapper<ReminderTask>().eq("create_user", currentUser).select("id"));
            List<Long> ownTaskIds = ownTasks.stream()
                    .map(ReminderTask::getId)
                    .collect(Collectors.toList());
            if (ownTaskIds.isEmpty()) {
                // 无自有任务，直接返回空结果
                return page;
            }
            wrapper.in("task_id", ownTaskIds);
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
