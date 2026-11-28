package com.elephantjava.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "elephant.recognition")
@Data
public class RecognitionProperties {

    /** 图片识别。小程序不直连，由 Java 以 multipart 字段 image 上传 JPEG。 */
    private String predictUrl = "http://120.196.88.140:9998/api/v1/infer";

    /** 识别服务请求头 X-Api-Key。 */
    private String apiKey = "elephant-demo-2026";

    /** 视频识别仍走原服务。小程序当前不展示入口。 */
    private String predictVideoUrl = "https://recognize.liangzhihuamei.xyz/predict-video";

    private String publicBaseUrl = "http://localhost:8082";

    private BigDecimal knownThreshold = new BigDecimal("75");

    private BigDecimal reviewThreshold = new BigDecimal("45");
}
