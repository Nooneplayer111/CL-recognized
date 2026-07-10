package com.elephantjava.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.elephantjava.dto.ChangePasswordDTO;
import com.elephantjava.dto.UserInfoDTO;
import com.elephantjava.dto.UserLoginDTO;
import com.elephantjava.entity.User;
import com.elephantjava.vo.UserInfoVO;

public interface IUserService extends IService<User> {

    /**
     * 用户登录
     */
    User login(UserLoginDTO userLoginDTO);

    /**
     * 获取当前登录用户信息
     */
    UserInfoVO getCurrentUserInfo();

    /**
     * 更新当前登录用户信息
     */
    void updateCurrentUserInfo(UserInfoDTO userInfoDTO);

    /**
     * 修改当前登录用户密码
     */
    void changePassword(ChangePasswordDTO changePasswordDTO);
}
