package com.example.reminder;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.example.reminder.mapper")
public class ReminderApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReminderApplication.class, args);
    }
}
