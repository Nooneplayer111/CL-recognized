package com.elephantjava.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 修改密码请求参数
 */
@Data
public class ChangePasswordDTO implements Serializable {

    /** 旧密码 */
    private String oldPassword;

    /** 新密码 */
    private String newPassword;

    /** 确认新密码 */
    private String confirmPassword;
}
