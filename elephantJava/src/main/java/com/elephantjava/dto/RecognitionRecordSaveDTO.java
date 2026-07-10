package com.elephantjava.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 保存识别记录请求参数（小程序/Web 调用）
 */
@Data
public class RecognitionRecordSaveDTO implements Serializable {

    /** 媒体类型：image / video */
    private String mediaType;

    /** 来源：miniprogram / web */
    private String source;

    /** 采集方式：camera / album */
    private String sourceType;

    /** 识别用户名称 */
    private String operatorName;

    /** 识别图片 Base64（图片识别时传入） */
    private String imageBase64;

    /** 原始/处理后媒体 URL（可选，视频场景） */
    private String mediaUrl;

    /** 处理后视频 URL */
    private String processedVideoUrl;

    /** 模型判断的主要结果 */
    private String predictedName;

    /** 置信度 0-100 */
    private BigDecimal confidence;

    /** 图片各类别概率 */
    private Map<String, Object> allProbs;

    /** 视频识别大象数量 */
    private Integer elephantCount;

    /** 视频处理帧数 */
    private Integer frameCount;

    /** 视频识别大象列表 */
    private Object elephants;

    /** 状态：success / failed */
    private String status;

    /** 失败原因 */
    private String errorMessage;
}
