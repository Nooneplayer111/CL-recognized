package com.elephantjava.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 识别记录响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionRecordVO implements Serializable {

    private Long id;
    private String mediaType;
    private String source;
    private String sourceType;
    private String operatorName;
    private String mediaUrl;
    private String processedVideoUrl;
    private String predictedName;
    private BigDecimal confidence;
    private String allProbs;
    private Integer elephantCount;
    private Integer frameCount;
    private String elephants;
    private Long individualId;
    private String individualName;
    private String status;
    private String errorMessage;
    /** 识别时间 yyyy-MM-dd HH:mm:ss */
    private String createTime;
}
