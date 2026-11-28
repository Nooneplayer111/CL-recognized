package com.elephantjava.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("recognition_task")
public class RecognitionTask {

    @TableId(type = IdType.INPUT)
    private String id;

    private String sessionId;

    private String mediaAssetId;

    private String idempotencyKey;

    private String mediaType;

    private String taskState;

    private String resultKind;

    private String reviewState;

    private String predictedName;

    private BigDecimal confidence;

    private Long speciesId;

    private Long individualId;

    private Long recordId;

    private String qualityReason;

    private String errorMessage;

    private String feedbackVerdict;

    private Integer visible;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
