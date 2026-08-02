package com.example.reminder.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.reminder.util.SqlSanitizer;

import java.util.*;

@Service
public class MetadataService {

    private static final Logger log = LoggerFactory.getLogger(MetadataService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 获取数据库中所有表名和注释。
     * 仅返回业务表白名单内的表，内部表（sys_user、reminder_*）不予暴露。
     */
    public List<Map<String, String>> getAllTables() {
        List<Map<String, String>> tables = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT TABLE_NAME, TABLE_COMMENT FROM INFORMATION_SCHEMA.TABLES " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE' " +
                            "ORDER BY TABLE_NAME");
            for (Map<String, Object> row : rows) {
                String tableName = String.valueOf(row.get("TABLE_NAME"));
                // 仅暴露业务表白名单内的表
                if (!SqlSanitizer.isBusinessTable(tableName)) {
                    continue;
                }
                Map<String, String> table = new HashMap<>();
                table.put("tableName", tableName);
                table.put("tableComment", String.valueOf(row.getOrDefault("TABLE_COMMENT", "")));
                tables.add(table);
            }
        } catch (Exception e) {
            log.error("获取表列表失败", e);
        }
        return tables;
    }

    /**
     * 获取指定表的所有字段信息。
     * 直查非业务表一律拒绝。
     */
    public List<Map<String, String>> getTableColumns(String tableName) {
        if (!SqlSanitizer.isBusinessTable(tableName)) {
            throw new IllegalArgumentException("禁止访问非业务表");
        }
        List<Map<String, String>> columns = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT COLUMN_NAME, COLUMN_COMMENT, DATA_TYPE, COLUMN_TYPE " +
                            "FROM INFORMATION_SCHEMA.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? " +
                            "ORDER BY ORDINAL_POSITION",
                    tableName);
            for (Map<String, Object> row : rows) {
                Map<String, String> column = new HashMap<>();
                column.put("columnName", String.valueOf(row.get("COLUMN_NAME")));
                column.put("columnComment", String.valueOf(row.getOrDefault("COLUMN_COMMENT", "")));
                column.put("dataType", String.valueOf(row.get("DATA_TYPE")));
                column.put("columnType", String.valueOf(row.get("COLUMN_TYPE")));
                columns.add(column);
            }
        } catch (Exception e) {
            log.error("获取表字段失败: {}", tableName, e);
        }
        return columns;
    }

    /**
     * 预览表数据（前10条）。
     * 直查非业务表一律拒绝。
     */
    public List<Map<String, Object>> previewTableData(String tableName) {
        if (!SqlSanitizer.isBusinessTable(tableName)) {
            throw new IllegalArgumentException("禁止访问非业务表");
        }
        try {
            return jdbcTemplate.queryForList("SELECT * FROM `" + tableName + "` LIMIT 10");
        } catch (Exception e) {
            log.error("预览表数据失败: {}", tableName, e);
            return Collections.emptyList();
        }
    }
}
