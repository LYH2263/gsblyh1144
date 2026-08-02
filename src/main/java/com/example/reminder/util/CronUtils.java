package com.example.reminder.util;

public class CronUtils {

    /**
     * 根据频率类型、天、小时、分钟生成Cron表达式
     * Spring Task Cron格式: 秒 分 时 日 月 周
     */
    public static String generateCron(String frequencyType, Integer day, int hour, int minute) {
        switch (frequencyType) {
            case "DAILY":
                // 每天 hour:minute 执行
                return String.format("0 %d %d * * ?", minute, hour);
            case "WEEKLY":
                // 每周第day天 hour:minute 执行 (1=周一...7=周日)
                // Spring cron: 1=MON, 7=SUN 也可以用 MON-SUN
                String weekDay = convertToWeekDay(day != null ? day : 1);
                return String.format("0 %d %d ? * %s", minute, hour, weekDay);
            case "MONTHLY":
                // 每月第day天 hour:minute 执行
                int monthDay = (day != null && day >= 1 && day <= 31) ? day : 1;
                return String.format("0 %d %d %d * ?", minute, hour, monthDay);
            default:
                return String.format("0 %d %d * * ?", minute, hour);
        }
    }

    private static String convertToWeekDay(int day) {
        switch (day) {
            case 1:
                return "MON";
            case 2:
                return "TUE";
            case 3:
                return "WED";
            case 4:
                return "THU";
            case 5:
                return "FRI";
            case 6:
                return "SAT";
            case 7:
                return "SUN";
            default:
                return "MON";
        }
    }

    /**
     * 获取Cron表达式的中文描述
     */
    public static String getDescription(String frequencyType, Integer day, int hour, int minute) {
        String time = String.format("%02d:%02d", hour, minute);
        switch (frequencyType) {
            case "DAILY":
                return "每天 " + time;
            case "WEEKLY":
                return "每周" + getWeekDayName(day) + " " + time;
            case "MONTHLY":
                return "每月" + day + "日 " + time;
            default:
                return time;
        }
    }

    private static String getWeekDayName(Integer day) {
        if (day == null)
            return "一";
        switch (day) {
            case 1:
                return "一";
            case 2:
                return "二";
            case 3:
                return "三";
            case 4:
                return "四";
            case 5:
                return "五";
            case 6:
                return "六";
            case 7:
                return "日";
            default:
                return "一";
        }
    }
}
