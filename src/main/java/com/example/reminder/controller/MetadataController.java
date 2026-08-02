package com.example.reminder.controller;

import com.example.reminder.service.MetadataService;
import com.example.reminder.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

    @Autowired
    private MetadataService metadataService;

    @GetMapping("/tables")
    public Result<List<Map<String, String>>> getTables() {
        return Result.success(metadataService.getAllTables());
    }

    @GetMapping("/columns/{tableName}")
    public Result<?> getColumns(@PathVariable String tableName) {
        try {
            return Result.success(metadataService.getTableColumns(tableName));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/preview/{tableName}")
    public Result<?> previewData(@PathVariable String tableName) {
        try {
            return Result.success(metadataService.previewTableData(tableName));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
