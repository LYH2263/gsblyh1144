package com.example.reminder.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SqlSanitizer {

    private static final Set<String> BLOCKED_KEYWORDS = new HashSet<>(Arrays.asList(
            "DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "CREATE", "TRUNCATE",
            "EXEC", "EXECUTE", "GRANT", "REVOKE", "INTO OUTFILE", "INTO DUMPFILE",
            "LOAD_FILE", "BENCHMARK", "SLEEP"));

    /**
     * 业务表白名单：元数据列表与直查（columns/preview/条件查询）只允许这三张表，
     * 其余（sys_user、reminder_task、reminder_condition、reminder_log 等内部表）一律拒绝。
     */
    public static final Set<String> ALLOWED_TABLES = new HashSet<>(Arrays.asList(
            "demo_employee", "demo_order", "demo_project"));

    /**
     * 判断表名是否为允许访问的业务表（大小写不敏感）。
     */
    public static boolean isBusinessTable(String tableName) {
        if (tableName == null) {
            return false;
        }
        return ALLOWED_TABLES.contains(tableName.trim().toLowerCase());
    }

    /**
     * 检查WHERE语句是否安全
     */
    public static boolean isSafe(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        String upper = sql.toUpperCase().trim();

        // 检查危险关键字
        for (String keyword : BLOCKED_KEYWORDS) {
            // 使用单词边界检查，避免误判
            if (upper.contains(keyword)) {
                return false;
            }
        }

        // 禁止注释
        if (upper.contains("--") || upper.contains("/*") || upper.contains("*/")) {
            return false;
        }

        // 禁止分号（防止多语句注入）
        if (upper.contains(";")) {
            return false;
        }

        return true;
    }

    /**
     * 转义SQL值中的特殊字符
     */
    public static String escapeValue(String value) {
        if (value == null)
            return "";
        return value.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"");
    }

    /**
     * 验证字段名是否合法（只允许字母、数字、下划线、点号）
     */
    public static boolean isValidFieldName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty())
            return false;
        return fieldName.matches("^[a-zA-Z_][a-zA-Z0-9_.]*$");
    }
}
