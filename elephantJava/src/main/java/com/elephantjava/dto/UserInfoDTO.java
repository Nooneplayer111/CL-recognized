package com.elephantjava.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户信息更新请求参数
 */
@Data
public class UserInfoDTO implements Serializable {

    /** 用户名（不可修改，仅用于标识） */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 部门 */
    private String department;

    /** 入职时间（yyyy-MM-dd） */
    private String joinDate;
}
