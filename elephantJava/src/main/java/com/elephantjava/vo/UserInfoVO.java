package com.elephantjava.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户信息响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVO implements Serializable {

    private String username;
    private String realName;
    private String email;
    private String phone;
    private String department;
    /** 入职时间，格式 yyyy-MM-dd */
    private String joinDate;
}
