package com.elephantjava.controller;

import com.elephantjava.result.Result;
import com.elephantjava.service.IRecognitionRecordService;
import com.elephantjava.vo.RecognitionRecordPageResult;
import com.elephantjava.vo.RecognitionRecordVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 识别记录管理接口（Web 管理端）
 */
@RestController
@RequestMapping("/recognition-records")
@Slf4j
public class RecognitionRecordController {

    @Autowired
    private IRecognitionRecordService recognitionRecordService;

    /**
     * 分页查询识别记录
     */
    @GetMapping
    public RecognitionRecordPageResult page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String mediaType) {
        log.info("查询识别记录: page={}, size={}, keyword={}, mediaType={}", page, size, keyword, mediaType);
        return recognitionRecordService.pageQuery(page, size, keyword, mediaType);
    }

    /**
     * 查询识别记录详情
     */
    @GetMapping("/{id}")
    public RecognitionRecordVO detail(@PathVariable Long id) {
        log.info("查询识别记录详情 id={}", id);
        return recognitionRecordService.getDetail(id);
    }

    /**
     * 删除识别记录
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除识别记录 id={}", id);
        recognitionRecordService.deleteRecord(id);
        return Result.success();
    }
}
