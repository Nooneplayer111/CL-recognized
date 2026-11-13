package com.elephantjava.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("media_upload")
public class MediaUpload {

    @TableId(type = IdType.INPUT)
    private String id;

    private String mediaAssetId;

    private String sessionId;

    private String fileName;

    private String contentType;

    private Long sizeBytes;

    private String source;

    private String state;

    private String storagePath;

    private String mediaType;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;
}
