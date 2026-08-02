package com.example.reminder.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.reminder.entity.ReminderCondition;
import com.example.reminder.entity.ReminderTask;
import com.example.reminder.entity.SysUser;
import com.example.reminder.exception.ForbiddenException;
import com.example.reminder.service.ConditionService;
import com.example.reminder.service.TaskService;
import com.example.reminder.util.Result;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/task")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private TaskService taskService;

    @Autowired
    private ConditionService conditionService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 分页查询任务列表
     */
    @GetMapping("/page")
    public Result<IPage<ReminderTask>> getTaskPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("currentUser");
        IPage<ReminderTask> page = taskService.getTaskPage(
                pageNum, pageSize, keyword, user.getUsername(), user.getRole());
        return Result.success(page);
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{id}")
    public Result<?> getTaskDetail(@PathVariable Long id, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("currentUser");
        Map<String, Object> detail = taskService.getTaskDetail(id, user.getUsername(), user.getRole());
        if (detail == null) {
            return Result.error("任务不存在");
        }
        return Result.success(detail);
    }

    /**
     * 创建任务
     */
    @PostMapping
    public Result<?> createTask(@RequestBody Map<String, Object> params, HttpSession session) {
        try {
            SysUser user = (SysUser) session.getAttribute("currentUser");

            ReminderTask task = new ReminderTask();
            task.setTaskName((String) params.get("taskName"));
            task.setTargetTable((String) params.get("targetTable"));
            task.setFrequencyType((String) params.get("frequencyType"));
            task.setFrequencyDay(
                    params.get("frequencyDay") != null ? Integer.parseInt(params.get("frequencyDay").toString())
                            : null);
            task.setFrequencyHour(Integer.parseInt(params.get("frequencyHour").toString()));
            task.setFrequencyMinute(Integer.parseInt(params.get("frequencyMinute").toString()));
            task.setConditionMode((String) params.get("conditionMode"));
            task.setConditionSql((String) params.get("conditionSql"));
            task.setMessageTemplate((String) params.get("messageTemplate"));
            task.setReceiverField((String) params.get("receiverField"));
            task.setDingtalkWebhook((String) params.get("dingtalkWebhook"));
            task.setStatus(params.get("status") != null ? Integer.parseInt(params.get("status").toString()) : 1);
            task.setCreateUser(user.getUsername());

            String conditionsJson = null;
            if (params.get("conditions") != null) {
                conditionsJson = objectMapper.writeValueAsString(params.get("conditions"));
            }

            ReminderTask created = taskService.createTask(task, conditionsJson);
            return Result.success("创建成功", created);
        } catch (Exception e) {
            log.error("创建任务失败", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }

    /**
     * 更新任务
     */
    @PutMapping("/{id}")
    public Result<?> updateTask(@PathVariable Long id, @RequestBody Map<String, Object> params, HttpSession session) {
        try {
            SysUser user = (SysUser) session.getAttribute("currentUser");

            ReminderTask task = new ReminderTask();
            task.setId(id);
            task.setTaskName((String) params.get("taskName"));
            task.setTargetTable((String) params.get("targetTable"));
            task.setFrequencyType((String) params.get("frequencyType"));
            task.setFrequencyDay(
                    params.get("frequencyDay") != null ? Integer.parseInt(params.get("frequencyDay").toString())
                            : null);
            task.setFrequencyHour(Integer.parseInt(params.get("frequencyHour").toString()));
            task.setFrequencyMinute(Integer.parseInt(params.get("frequencyMinute").toString()));
            task.setConditionMode((String) params.get("conditionMode"));
            task.setConditionSql((String) params.get("conditionSql"));
            task.setMessageTemplate((String) params.get("messageTemplate"));
            task.setReceiverField((String) params.get("receiverField"));
            task.setDingtalkWebhook((String) params.get("dingtalkWebhook"));
            task.setStatus(params.get("status") != null ? Integer.parseInt(params.get("status").toString()) : 1);

            String conditionsJson = null;
            if (params.get("conditions") != null) {
                conditionsJson = objectMapper.writeValueAsString(params.get("conditions"));
            }

            ReminderTask updated = taskService.updateTask(task, conditionsJson, user.getUsername(), user.getRole());
            return Result.success("更新成功", updated);
        } catch (ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新任务失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteTask(@PathVariable Long id, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("currentUser");
        taskService.deleteTask(id, user.getUsername(), user.getRole());
        return Result.success("删除成功");
    }

    /**
     * 切换任务状态
     */
    @PostMapping("/{id}/toggle")
    public Result<?> toggleStatus(@PathVariable Long id, HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("currentUser");
        taskService.toggleTaskStatus(id, user.getUsername(), user.getRole());
        return Result.success("状态切换成功");
    }

    /**
     * 手动触发执行任务
     */
    @PostMapping("/{id}/trigger")
    public Result<?> triggerTask(@PathVariable Long id, HttpSession session) {
        try {
            SysUser user = (SysUser) session.getAttribute("currentUser");
            taskService.triggerTask(id, user.getUsername(), user.getRole());
            return Result.success("任务已触发执行");
        } catch (ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            return Result.error("触发失败: " + e.getMessage());
        }
    }

    /**
     * 预览查询SQL
     */
    @PostMapping("/preview-sql")
    public Result<?> previewSql(@RequestBody Map<String, Object> params) {
        try {
            String tableName = (String) params.get("targetTable");
            String conditionMode = (String) params.get("conditionMode");
            String whereClause;

            if ("EDITOR".equals(conditionMode)) {
                whereClause = (String) params.get("conditionSql");
            } else {
                String condJson = objectMapper.writeValueAsString(params.get("conditions"));
                List<ReminderCondition> conditions = objectMapper.readValue(
                        condJson, new TypeReference<List<ReminderCondition>>() {
                        });
                whereClause = conditionService.buildWhereClause(conditions);
            }

            String sql = conditionService.buildPreviewSql(tableName, whereClause);
            List<Map<String, Object>> data = conditionService.executeQuery(tableName, whereClause);

            Map<String, Object> result = new java.util.HashMap<>();
            result.put("sql", sql);
            result.put("data", data);
            result.put("total", data.size());
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            return Result.error("预览失败: " + e.getMessage());
        }
    }
}
