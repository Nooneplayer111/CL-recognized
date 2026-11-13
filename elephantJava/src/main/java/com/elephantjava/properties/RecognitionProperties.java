package com.elephantjava.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "elephant.recognition")
@Data
public class RecognitionProperties {

    /** 现有图片识别服务。小程序不直连，由 Java 在任务里调用。 */
    private String predictUrl = "https://recognize.liangzhihuamei.xyz/predict";

    /** 视频识别仍走原服务。小程序当前不展示入口。 */
    private String predictVideoUrl = "https://recognize.liangzhihuamei.xyz/predict-video";

    private String publicBaseUrl = "http://localhost:8082";

    private BigDecimal knownThreshold = new BigDecimal("75");

    private BigDecimal reviewThreshold = new BigDecimal("45");
}
