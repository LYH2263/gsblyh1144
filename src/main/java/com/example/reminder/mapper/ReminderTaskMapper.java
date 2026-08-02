package com.example.reminder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.reminder.entity.ReminderTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReminderTaskMapper extends BaseMapper<ReminderTask> {
}
