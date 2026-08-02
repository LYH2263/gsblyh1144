package com.example.reminder.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("reminder_log")
public class ReminderLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String taskName;
    private LocalDateTime sendTime;
    private String receiver;
    private String sendContent;
    private Integer sendStatus;
    private String errorMsg;
}
