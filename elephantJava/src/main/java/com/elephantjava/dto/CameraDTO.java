package com.elephantjava.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 摄像头新增/修改请求参数
 */
@Data
public class CameraDTO implements Serializable {

    /** 摄像头名称 */
    private String name;

    /** IP地址 */
    private String ipAddress;

    /** 安装位置 */
    private String location;

    /** 端口号，默认8080 */
    private Integer port;

    /** 登录用户名 */
    private String username;

    /** 登录密码 */
    private String password;
}
