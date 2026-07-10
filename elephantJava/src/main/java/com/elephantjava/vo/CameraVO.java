package com.elephantjava.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 摄像头信息响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraVO implements Serializable {

    private Long id;
    private String name;
    private String ipAddress;
    private String location;
    private Integer port;
    private String username;
    /** 返回给前端时做脱敏处理 */
    private String password;
    /** 在线状态：online / offline */
    private String status;
    /** 预览画面地址 */
    private String previewUrl;
}
