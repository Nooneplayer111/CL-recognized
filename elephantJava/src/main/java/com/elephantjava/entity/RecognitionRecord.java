package com.elephantjava.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 识别记录实体
 */
@Data
@TableName("recognition_record")
public class RecognitionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("media_type")
    private String mediaType;

    @TableField("source")
    private String source;

    @TableField("source_type")
    private String sourceType;

    @TableField("operator_name")
    private String operatorName;

    @TableField("user_id")
    private Long userId;

    @TableField("media_url")
    private String mediaUrl;

    @TableField("processed_video_url")
    private String processedVideoUrl;

    @TableField("predicted_name")
    private String predictedName;

    @TableField("confidence")
    private BigDecimal confidence;

    /** JSON 字符串：图片各类别概率 */
    @TableField("all_probs")
    private String allProbs;

    @TableField("elephant_count")
    private Integer elephantCount;

    @TableField("frame_count")
    private Integer frameCount;

    /** JSON 字符串：视频识别大象列表 */
    @TableField("elephants")
    private String elephants;

    @TableField("individual_id")
    private Long individualId;

    @TableField("status")
    private String status;

    @TableField("error_message")
    private String errorMessage;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
