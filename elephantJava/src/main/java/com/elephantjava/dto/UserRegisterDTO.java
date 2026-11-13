package com.elephantjava.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterDTO implements Serializable {

    private String username;

    private String realName;

    private String password;

    private String confirmPassword;
}
