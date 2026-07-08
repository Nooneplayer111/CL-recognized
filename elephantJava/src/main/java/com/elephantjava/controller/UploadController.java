package com.elephantjava.controller;

import com.elephantjava.properties.UploadProperties;
import com.elephantjava.vo.UploadResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传接口
 */
@RestController
@RequestMapping("/upload")
@Slf4j
public class UploadController {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    @Autowired
    private UploadProperties uploadProperties;

    /**
     * 上传图片文件，返回可访问的 URL
     */
    @PostMapping
    public UploadResult upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return UploadResult.builder().code(0).message("上传文件不能为空").build();
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return UploadResult.builder().code(0).message("仅支持上传图片文件").build();
        }

        // 创建保存目录
        Path uploadDir = Paths.get(uploadProperties.getPath()).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);

        // 生成唯一文件名，保留原扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String fileName = UUID.randomUUID().toString().replace("-", "");
        if (StringUtils.hasText(extension)) {
            fileName = fileName + "." + extension.toLowerCase();
        }

        Path targetPath = uploadDir.resolve(fileName);
        file.transferTo(targetPath.toFile());

        // 构建完整访问地址，供前端直接展示
        String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(uploadProperties.getAccessPath())
                .path("/")
                .path(fileName)
                .toUriString();

        log.info("文件上传成功: {}", fileUrl);
        return UploadResult.builder()
                .code(1)
                .message("上传成功")
                .url(fileUrl)
                .build();
    }
}
