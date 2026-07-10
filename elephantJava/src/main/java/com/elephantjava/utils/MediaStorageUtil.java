package com.elephantjava.utils;

import com.elephantjava.properties.UploadProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

/**
 * 识别媒体文件存储工具
 */
@Component
public class MediaStorageUtil {

    @Autowired
    private UploadProperties uploadProperties;

    /**
     * 将 Base64 图片保存到本地并返回访问 URL
     */
    public String saveBase64Image(String base64Data) throws Exception {
        if (!StringUtils.hasText(base64Data)) {
            return null;
        }

        String cleanBase64 = base64Data;
        String extension = "jpg";
        if (base64Data.contains(",")) {
            String prefix = base64Data.substring(0, base64Data.indexOf(','));
            cleanBase64 = base64Data.substring(base64Data.indexOf(',') + 1);
            if (prefix.contains("png")) {
                extension = "png";
            } else if (prefix.contains("webp")) {
                extension = "webp";
            }
        }

        byte[] bytes = Base64.getDecoder().decode(cleanBase64);
        Path recognitionDir = Paths.get(uploadProperties.getPath(), "recognition")
                .toAbsolutePath().normalize();
        Files.createDirectories(recognitionDir);

        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetPath = recognitionDir.resolve(fileName);
        Files.write(targetPath, bytes);

        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(uploadProperties.getAccessPath())
                .path("/recognition/")
                .path(fileName)
                .toUriString();
    }
}
