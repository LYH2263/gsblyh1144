package com.example.reminder.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.reminder.entity.ReminderCondition;
import com.example.reminder.entity.ReminderTask;
import com.example.reminder.exception.ForbiddenException;
import com.example.reminder.mapper.ReminderConditionMapper;
import com.example.reminder.mapper.ReminderTaskMapper;
import com.example.reminder.util.CronUtils;
import com.example.reminder.util.SqlSanitizer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    @Autowired
    private ReminderTaskMapper taskMapper;

    @Autowired
    private ReminderConditionMapper conditionMapper;

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 分页查询任务列表
     */
    public IPage<ReminderTask> getTaskPage(int pageNum, int pageSize, String keyword, String currentUser, String role) {
        Page<ReminderTask> page = new Page<>(pageNum, pageSize);
        QueryWrapper<ReminderTask> wrapper = new QueryWrapper<>();

        // 非管理员只能查看自己的任务
        if (!"ADMIN".equals(role)) {
            wrapper.eq("create_user", currentUser);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w.like("task_name", keyword)
                    .or().like("target_table", keyword));
        }

        wrapper.orderByDesc("update_time");
        return taskMapper.selectPage(page, wrapper);
    }

    /**
     * 获取任务详情（含条件列表）
     */
    public Map<String, Object> getTaskDetail(Long taskId, String currentUser, String role) {
        ReminderTask task = taskMapper.selectById(taskId);
        if (task == null)
            return null;
        checkOwnership(task, currentUser, role);

        List<ReminderCondition> conditions = conditionMapper.selectList(
                new QueryWrapper<ReminderCondition>()
                        .eq("task_id", taskId)
                        .orderByAsc("sort_order"));

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("task", task);
        result.put("conditions", conditions);
        return result;
    }

    /**
     * 创建定时任务
     */
    @Transactional
    public ReminderTask createTask(ReminderTask task, String conditionsJson) {
        // 仅允许针对白名单业务表创建提醒任务
        SqlSanitizer.checkAllowedTable(task.getTargetTable());

        // 生成Cron表达式
        String cron = CronUtils.generateCron(
                task.getFrequencyType(), task.getFrequencyDay(),
                task.getFrequencyHour(), task.getFrequencyMinute());
        task.setCronExpression(cron);

        if (task.getStatus() == null) {
            task.setStatus(1);
        }

        taskMapper.insert(task);

        // 保存可视化条件
        if ("VISUAL".equals(task.getConditionMode()) && conditionsJson != null && !conditionsJson.isEmpty()) {
            saveConditions(task.getId(), conditionsJson);
        }

        // 注册调度任务
        if (task.getStatus() == 1) {
            schedulerService.registerTask(task);
        }

        log.info("创建定时任务: {} (ID={}, Cron={})", task.getTaskName(), task.getId(), cron);
        return task;
    }

    /**
     * 更新定时任务
     */
    @Transactional
    public ReminderTask updateTask(ReminderTask task, String conditionsJson, String currentUser, String role) {
        ReminderTask existing = taskMapper.selectById(task.getId());
        if (existing == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        checkOwnership(existing, currentUser, role);
        // 仅允许针对白名单业务表创建提醒任务
        SqlSanitizer.checkAllowedTable(task.getTargetTable());

        // 重新生成Cron表达式
        String cron = CronUtils.generateCron(
                task.getFrequencyType(), task.getFrequencyDay(),
                task.getFrequencyHour(), task.getFrequencyMinute());
        task.setCronExpression(cron);
        // 创建人不可被更新覆盖
        task.setCreateUser(existing.getCreateUser());

        taskMapper.updateById(task);

        // 更新可视化条件：先删后插
        if ("VISUAL".equals(task.getConditionMode())) {
            conditionMapper.delete(
                    new QueryWrapper<ReminderCondition>().eq("task_id", task.getId()));
            if (conditionsJson != null && !conditionsJson.isEmpty()) {
                saveConditions(task.getId(), conditionsJson);
            }
        }

        // 重新注册调度任务
        if (task.getStatus() != null && task.getStatus() == 1) {
            schedulerService.registerTask(task);
        } else {
            schedulerService.cancelTask(task.getId());
        }

        log.info("更新定时任务: {} (ID={}, Cron={})", task.getTaskName(), task.getId(), cron);
        return task;
    }

    /**
     * 删除定时任务
     */
    @Transactional
    public void deleteTask(Long taskId, String currentUser, String role) {
        ReminderTask existing = taskMapper.selectById(taskId);
        if (existing == null) {
            return;
        }
        checkOwnership(existing, currentUser, role);
        schedulerService.cancelTask(taskId);
        conditionMapper.delete(new QueryWrapper<ReminderCondition>().eq("task_id", taskId));
        taskMapper.deleteById(taskId);
        log.info("删除定时任务: ID={}", taskId);
    }

    /**
     * 切换任务状态
     */
    public void toggleTaskStatus(Long taskId, String currentUser, String role) {
        ReminderTask task = taskMapper.selectById(taskId);
        if (task == null)
            return;
        checkOwnership(task, currentUser, role);

        int newStatus = (task.getStatus() != null && task.getStatus() == 1) ? 0 : 1;
        task.setStatus(newStatus);
        taskMapper.updateById(task);

        if (newStatus == 1) {
            schedulerService.registerTask(task);
        } else {
            schedulerService.cancelTask(taskId);
        }

        log.info("切换任务状态: {} (ID={}, 新状态={})", task.getTaskName(), taskId, newStatus == 1 ? "启用" : "停用");
    }

    /**
     * 手动触发执行任务
     */
    public void triggerTask(Long taskId, String currentUser, String role) {
        ReminderTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        checkOwnership(task, currentUser, role);
        schedulerService.triggerTask(taskId);
    }

    /**
     * 归属与角色校验：ADMIN 可操作全部，USER 仅能操作本人创建的任务
     */
    private void checkOwnership(ReminderTask task, String currentUser, String role) {
        if ("ADMIN".equals(role)) {
            return;
        }
        if (task.getCreateUser() == null || !task.getCreateUser().equals(currentUser)) {
            throw new ForbiddenException("无权操作该资源");
        }
    }

    /**
     * 保存可视化条件
     */
    private void saveConditions(Long taskId, String conditionsJson) {
        try {
            List<ReminderCondition> conditions = objectMapper.readValue(
                    conditionsJson, new TypeReference<List<ReminderCondition>>() {
                    });
            for (int i = 0; i < conditions.size(); i++) {
                ReminderCondition cond = conditions.get(i);
                cond.setId(null);
                cond.setTaskId(taskId);
                cond.setSortOrder(i);
                if (cond.getLogicOperator() == null || cond.getLogicOperator().isEmpty()) {
                    cond.setLogicOperator("AND");
                }
                conditionMapper.insert(cond);
            }
        } catch (Exception e) {
            log.error("保存条件失败", e);
            throw new RuntimeException("保存条件失败: " + e.getMessage());
        }
    }
}
