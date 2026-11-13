package com.elephantjava.controller;

import com.elephantjava.result.Result;
import com.elephantjava.service.VisitorRecognitionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1")
public class VisitorRecognitionController {

    private final VisitorRecognitionService recognitionService;

    public VisitorRecognitionController(VisitorRecognitionService recognitionService) {
        this.recognitionService = recognitionService;
    }

    @PostMapping("/visitor-sessions")
    public Result<Map<String, Object>> session() {
        return Result.success(recognitionService.createSession());
    }

    @PostMapping("/media-upload-sessions")
    public Result<Map<String, Object>> createUpload(@RequestBody Map<String, Object> request) {
        return Result.success(recognitionService.createUpload(request));
    }

    @PutMapping("/media-upload-sessions/{id}/content")
    public Result<Map<String, Object>> content(@PathVariable String id, HttpServletRequest request) throws Exception {
        return Result.success(recognitionService.saveContent(id, request.getInputStream().readAllBytes()));
    }

    @PostMapping("/media-upload-sessions/{id}/complete")
    public Result<Map<String, Object>> complete(@PathVariable String id) {
        return Result.success(recognitionService.completeUpload(id));
    }

    @PostMapping("/recognition-tasks")
    public Result<Map<String, Object>> createTask(@RequestBody Map<String, Object> request,
                                                   @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return Result.success(recognitionService.createTask(String.valueOf(request.get("mediaAssetId")), idempotencyKey));
    }

    @GetMapping("/recognition-tasks/{id}")
    public Result<Map<String, Object>> task(@PathVariable String id) {
        return Result.success(recognitionService.getTask(id));
    }

    @PutMapping("/recognition-tasks/{id}/feedback")
    public Result<Map<String, Object>> feedback(@PathVariable String id, @RequestBody Map<String, Object> request) {
        return Result.success(recognitionService.feedback(id, String.valueOf(request.get("verdict"))));
    }

    @GetMapping("/me/recognition-history")
    public Result<Map<String, Object>> history() {
        return Result.success(recognitionService.history());
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/me/recognition-history/{id}")
    public Result<Map<String, Object>> delete(@PathVariable String id) {
        return Result.success(recognitionService.deleteHistory(id));
    }

    @PostMapping("/privacy-requests")
    public Result<Map<String, Object>> privacy() {
        return Result.success(recognitionService.privacyRequest());
    }

    @GetMapping("/public/elephants")
    public Result<java.util.List<Map<String, Object>>> elephants() {
        return Result.success(recognitionService.publicElephants());
    }

    @GetMapping("/public/elephants/{id}")
    public Result<Map<String, Object>> elephant(@PathVariable String id) {
        return Result.success(recognitionService.publicElephant(id));
    }
}
