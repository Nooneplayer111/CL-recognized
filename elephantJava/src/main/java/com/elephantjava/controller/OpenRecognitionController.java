package com.elephantjava.controller;

import com.elephantjava.dto.RecognitionRecordSaveDTO;
import com.elephantjava.result.Result;
import com.elephantjava.service.IRecognitionRecordService;
import com.elephantjava.vo.RecognitionRecordVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 开放识别记录接口（供小程序调用，无需登录）
 */
@RestController
@RequestMapping("/open/recognition-records")
@Slf4j
public class OpenRecognitionController {

    @Autowired
    private IRecognitionRecordService recognitionRecordService;

    /**
     * 保存识别记录
     */
    @PostMapping
    public Result<RecognitionRecordVO> save(@RequestBody RecognitionRecordSaveDTO saveDTO) {
        log.info("保存识别记录: mediaType={}, predictedName={}, operatorName={}",
                saveDTO.getMediaType(), saveDTO.getPredictedName(), saveDTO.getOperatorName());
        RecognitionRecordVO recordVO = recognitionRecordService.saveRecord(saveDTO);
        return Result.success(recordVO);
    }
}
