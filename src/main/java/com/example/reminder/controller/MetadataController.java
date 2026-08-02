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
    public Result<List<Map<String, String>>> getColumns(@PathVariable String tableName) {
        return Result.success(metadataService.getTableColumns(tableName));
    }

    @GetMapping("/preview/{tableName}")
    public Result<List<Map<String, Object>>> previewData(@PathVariable String tableName) {
        return Result.success(metadataService.previewTableData(tableName));
    }
}
