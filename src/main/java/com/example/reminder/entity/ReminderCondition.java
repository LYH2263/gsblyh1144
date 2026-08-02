package com.example.reminder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("reminder_condition")
public class ReminderCondition {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Integer sortOrder;
    private String fieldName;
    private String operator;
    private String compareType;
    private String compareValue;
    private String logicOperator;
}
