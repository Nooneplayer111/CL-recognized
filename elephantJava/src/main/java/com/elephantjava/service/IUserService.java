package com.elephantjava.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.elephantjava.dto.UserLoginDTO;
import com.elephantjava.entity.User;

public interface IUserService extends IService<User> {
    User login(UserLoginDTO userLoginDTO);
}
