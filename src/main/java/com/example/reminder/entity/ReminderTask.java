package com.example.reminder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("reminder_task")
public class ReminderTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskName;
    private String targetTable;
    private String frequencyType;
    private Integer frequencyDay;
    private Integer frequencyHour;
    private Integer frequencyMinute;
    private String cronExpression;
    private String conditionMode;
    private String conditionJson;
    private String conditionSql;
    private String messageTemplate;
    private String receiverField;
    private String dingtalkWebhook;
    private Integer status;
    private String createUser;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
