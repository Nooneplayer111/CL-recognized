package com.elephantjava.controller;

import com.elephantjava.result.Result;
import com.elephantjava.service.VisitorRecognitionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminInsightController {

    private final VisitorRecognitionService recognitionService;

    public AdminInsightController(VisitorRecognitionService recognitionService) {
        this.recognitionService = recognitionService;
    }

    @GetMapping("/recognition-events")
    public Result<List<Map<String, Object>>> events() {
        return Result.success(recognitionService.adminEvents());
    }

    @GetMapping("/review-tasks")
    public Result<List<Map<String, Object>>> reviews() {
        return Result.success(recognitionService.reviewQueue());
    }

    @PostMapping("/review-tasks/{id}/conclude")
    public Result<Map<String, Object>> conclude(@PathVariable String id, @RequestBody Map<String, Object> request) {
        Object individualId = request.get("individualId");
        Long parsed = individualId == null || String.valueOf(individualId).isBlank()
                ? null : Long.valueOf(String.valueOf(individualId));
        return Result.success(recognitionService.conclude(id, String.valueOf(request.get("decision")), parsed));
    }

    @GetMapping("/audit-events")
    public Result<List<Map<String, Object>>> audits() {
        return Result.success(recognitionService.auditEvents());
    }
}
