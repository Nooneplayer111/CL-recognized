package com.elephantjava.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 本地文件上传配置
 */
@Component
@ConfigurationProperties(prefix = "elephant.upload")
@Data
public class UploadProperties {

    /** 文件保存目录（相对项目运行目录） */
    private String path = "uploads";

    /** 对外访问路径前缀 */
    private String accessPath = "/uploads";
}
