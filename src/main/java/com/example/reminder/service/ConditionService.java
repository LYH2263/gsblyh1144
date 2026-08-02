package com.example.reminder.service;

import com.example.reminder.entity.ReminderCondition;
import com.example.reminder.util.SqlSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ConditionService {

    private static final Logger log = LoggerFactory.getLogger(ConditionService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 从可视化条件列表构建WHERE子句
     */
    public String buildWhereClause(List<ReminderCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return "1=1";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < conditions.size(); i++) {
            ReminderCondition cond = conditions.get(i);
            if (i > 0) {
                String logic = cond.getLogicOperator();
                if (logic == null || logic.isEmpty()) {
                    logic = "AND";
                }
                sb.append(" ").append(logic).append(" ");
            }
            sb.append(buildSingleCondition(cond));
        }
        return sb.toString();
    }

    private String buildSingleCondition(ReminderCondition cond) {
        String field = cond.getFieldName();
        if (!SqlSanitizer.isValidFieldName(field)) {
            throw new IllegalArgumentException("非法字段名: " + field);
        }
        String escapedField = "`" + field + "`";

        String operator = cond.getOperator();
        String value = cond.getCompareValue();
        String compareType = cond.getCompareType();

        // 如果比较类型是字段
        if ("FIELD".equals(compareType)) {
            if (!SqlSanitizer.isValidFieldName(value)) {
                throw new IllegalArgumentException("非法比较字段名: " + value);
            }
            String escapedValue = "`" + value + "`";
            return escapedField + " " + mapOperator(operator) + " " + escapedValue;
        }

        // 固定值比较
        String escapedValue = SqlSanitizer.escapeValue(value);
        switch (operator) {
            case "CONTAINS":
                return escapedField + " LIKE '%" + escapedValue + "%'";
            case "NOT_CONTAINS":
                return escapedField + " NOT LIKE '%" + escapedValue + "%'";
            case "STARTS_WITH":
                return escapedField + " LIKE '" + escapedValue + "%'";
            default:
                return escapedField + " " + mapOperator(operator) + " '" + escapedValue + "'";
        }
    }

    private String mapOperator(String operator) {
        switch (operator) {
            case "GT":
                return ">";
            case "GTE":
                return ">=";
            case "LT":
                return "<";
            case "LTE":
                return "<=";
            case "EQ":
                return "=";
            case "NEQ":
                return "!=";
            default:
                return "=";
        }
    }

    /**
     * 执行查询，返回匹配的数据行
     */
    public List<Map<String, Object>> executeQuery(String tableName, String whereClause) {
        SqlSanitizer.assertBusinessTable(tableName);

        if (!SqlSanitizer.isValidFieldName(tableName)) {
            throw new IllegalArgumentException("非法表名: " + tableName);
        }
        if (whereClause != null && !whereClause.trim().isEmpty() && !"1=1".equals(whereClause.trim())) {
            if (!SqlSanitizer.isSafe(whereClause)) {
                throw new IllegalArgumentException("WHERE语句包含不安全的关键字");
            }
        }

        String sql = "SELECT * FROM `" + tableName + "`";
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql += " WHERE " + whereClause;
        }
        sql += " LIMIT 1000";

        log.info("执行查询: {}", sql);
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 预览查询SQL（不实际执行）
     */
    public String buildPreviewSql(String tableName, String whereClause) {
        String sql = "SELECT * FROM `" + tableName + "`";
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sql += " WHERE " + whereClause;
        }
        return sql + " LIMIT 100";
    }
}
