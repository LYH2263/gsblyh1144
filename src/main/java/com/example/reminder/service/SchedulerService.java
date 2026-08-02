package com.example.reminder.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.reminder.dto.DingTalkSendResult;
import com.example.reminder.entity.ReminderCondition;
import com.example.reminder.entity.ReminderLog;
import com.example.reminder.entity.ReminderTask;
import com.example.reminder.mapper.ReminderConditionMapper;
import com.example.reminder.mapper.ReminderLogMapper;
import com.example.reminder.mapper.ReminderTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private ReminderTaskMapper taskMapper;

    @Autowired
    private ReminderConditionMapper conditionMapper;

    @Autowired
    private ReminderLogMapper logMapper;

    @Autowired
    private ConditionService conditionService;

    @Autowired
    private DingTalkService dingTalkService;

    /**
     * 应用启动后加载所有已启用的定时任务
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("======= 初始化定时任务调度 =======");
        List<ReminderTask> tasks = taskMapper.selectList(
                new QueryWrapper<ReminderTask>().eq("status", 1));
        for (ReminderTask task : tasks) {
            registerTask(task);
        }
        log.info("======= 已加载 {} 个定时任务 =======", tasks.size());
    }

    /**
     * 注册一个定时任务
     */
    public void registerTask(ReminderTask task) {
        cancelTask(task.getId());

        if (task.getStatus() == null || task.getStatus() != 1) {
            return;
        }

        String cron = task.getCronExpression();
        if (cron == null || cron.isEmpty()) {
            log.warn("任务[{}]的Cron表达式为空，跳过注册", task.getTaskName());
            return;
        }

        try {
            CronTrigger trigger = new CronTrigger(cron);
            final Long taskId = task.getId();
            ScheduledFuture<?> future = taskScheduler.schedule(
                    () -> executeTask(taskId), trigger);
            scheduledTasks.put(task.getId(), future);
            log.info("注册定时任务: {} (ID={}, Cron={})", task.getTaskName(), task.getId(), cron);
        } catch (Exception e) {
            log.error("注册定时任务失败: {} (ID={})", task.getTaskName(), task.getId(), e);
        }
    }

    /**
     * 取消一个定时任务
     */
    public void cancelTask(Long taskId) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
            log.info("取消定时任务: ID={}", taskId);
        }
    }

    /**
     * 手动触发执行一个任务（用于测试）
     */
    public void triggerTask(Long taskId) {
        executeTask(taskId);
    }

    /**
     * 获取已注册的任务数量
     */
    public int getRegisteredTaskCount() {
        return scheduledTasks.size();
    }

    /**
     * 检查任务是否已注册
     */
    public boolean isTaskRegistered(Long taskId) {
        return scheduledTasks.containsKey(taskId);
    }

    /**
     * 执行定时任务
     */
    private void executeTask(Long taskId) {
        log.info("===== 开始执行定时任务: ID={} =====", taskId);

        // 从DB重新加载最新的任务配置
        ReminderTask task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("任务不存在: ID={}", taskId);
            cancelTask(taskId);
            return;
        }
        if (task.getStatus() != 1) {
            log.warn("任务已停用: {} (ID={})", task.getTaskName(), taskId);
            cancelTask(taskId);
            return;
        }

        try {
            // 构建WHERE子句
            String whereClause;
            if ("EDITOR".equals(task.getConditionMode())) {
                whereClause = task.getConditionSql();
            } else {
                List<ReminderCondition> conditions = conditionMapper.selectList(
                        new QueryWrapper<ReminderCondition>()
                                .eq("task_id", taskId)
                                .orderByAsc("sort_order"));
                whereClause = conditionService.buildWhereClause(conditions);
            }

            // 执行查询
            List<Map<String, Object>> rows = conditionService.executeQuery(
                    task.getTargetTable(), whereClause);

            log.info("任务[{}]查询到{}条匹配数据", task.getTaskName(), rows.size());

            if (rows.isEmpty()) {
                // 无匹配数据也记录日志
                ReminderLog logEntry = new ReminderLog();
                logEntry.setTaskId(taskId);
                logEntry.setTaskName(task.getTaskName());
                logEntry.setSendTime(LocalDateTime.now());
                logEntry.setSendContent("无匹配数据");
                logEntry.setSendStatus(1);
                logMapper.insert(logEntry);
                return;
            }

            // 逐行发送提醒
            for (Map<String, Object> row : rows) {
                String message = buildMessage(task.getMessageTemplate(), row);
                String receiver = getFieldValue(row, task.getReceiverField());

                boolean success = false;
                String errorMsg = null;

                try {
                    if (task.getDingtalkWebhook() != null && !task.getDingtalkWebhook().trim().isEmpty()) {
                        DingTalkSendResult result = dingTalkService.sendMessage(task.getDingtalkWebhook(), message);
                        success = result.isSuccess();
                        if (!success) {
                            errorMsg = result.getErrmsg();
                        }
                    } else {
                        // 未配置webhook，仅记录日志
                        success = true;
                        log.info("任务[{}]无钉钉Webhook配置，仅记录: {} -> {}", task.getTaskName(), receiver, message);
                    }
                } catch (Exception e) {
                    errorMsg = e.getMessage();
                    log.error("发送提醒消息失败", e);
                }

                // 记录发送日志
                ReminderLog logEntry = new ReminderLog();
                logEntry.setTaskId(taskId);
                logEntry.setTaskName(task.getTaskName());
                logEntry.setSendTime(LocalDateTime.now());
                logEntry.setReceiver(receiver);
                logEntry.setSendContent(message);
                logEntry.setSendStatus(success ? 1 : 0);
                logEntry.setErrorMsg(errorMsg);
                logMapper.insert(logEntry);
            }

        } catch (Exception e) {
            log.error("执行定时任务异常: {} (ID={})", task.getTaskName(), taskId, e);
            // 记录异常日志
            ReminderLog logEntry = new ReminderLog();
            logEntry.setTaskId(taskId);
            logEntry.setTaskName(task.getTaskName());
            logEntry.setSendTime(LocalDateTime.now());
            logEntry.setSendStatus(0);
            logEntry.setErrorMsg("任务执行异常: " + e.getMessage());
            logMapper.insert(logEntry);
        }

        log.info("===== 定时任务执行完成: ID={} =====", taskId);
    }

    /**
     * 构建消息内容，替换模板中的${字段名}占位符
     */
    private String buildMessage(String template, Map<String, Object> row) {
        if (template == null)
            return "";

        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String value = getFieldValue(row, fieldName);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 从数据行中获取字段值（大小写不敏感匹配）
     */
    private String getFieldValue(Map<String, Object> row, String fieldName) {
        if (row == null || fieldName == null)
            return "";

        // 精确匹配
        Object val = row.get(fieldName);
        if (val != null)
            return val.toString();

        // 大小写不敏感匹配
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(fieldName)) {
                return entry.getValue() != null ? entry.getValue().toString() : "";
            }
        }
        return "";
    }
}
