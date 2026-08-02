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
     * 分页查询日志：ADMIN 查看全部，USER 只能查看本人创建任务的发送日志
     */
    public IPage<ReminderLog> getLogPage(int pageNum, int pageSize, Long taskId, String keyword,
            String currentUser, String role) {
        Page<ReminderLog> page = new Page<>(pageNum, pageSize);
        QueryWrapper<ReminderLog> wrapper = new QueryWrapper<>();

        // 非管理员只能查看自己任务的发送日志（reminder_log 经 task_id 关联任务创建人）
        if (!"ADMIN".equals(role)) {
            List<Long> ownedTaskIds = taskMapper.selectList(
                    new QueryWrapper<ReminderTask>()
                            .select("id")
                            .eq("create_user", currentUser))
                    .stream().map(ReminderTask::getId).collect(Collectors.toList());
            if (ownedTaskIds.isEmpty()) {
                return page;
            }
            wrapper.in("task_id", ownedTaskIds);
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
