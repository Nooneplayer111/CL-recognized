package com.elephantjava.controller;

import com.elephantjava.constant.JwtClaimsConstant;
import com.elephantjava.dto.ChangePasswordDTO;
import com.elephantjava.dto.UserInfoDTO;
import com.elephantjava.dto.UserLoginDTO;
import com.elephantjava.entity.User;
import com.elephantjava.properties.JwtProperties;
import com.elephantjava.result.Result;
import com.elephantjava.service.IUserService;
import com.elephantjava.utils.JwtUtil;
import com.elephantjava.vo.UserInfoVO;
import com.elephantjava.vo.UserLoginVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户相关接口
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private IUserService userService;

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录功能
     */
    @PostMapping("/login")
    public Result<UserLoginVo> login(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("员工登录: {}", userLoginDTO);
        User user = userService.login(userLoginDTO);

        // 登录成功后，生成 JWT 令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        UserLoginVo userLoginVo = UserLoginVo.builder()
                .userName(user.getUsername())
                .token(token)
                .build();

        return Result.success(userLoginVo);
    }

    /**
     * 获取当前登录用户信息
     * 前端 Profile.vue 期望直接返回用户对象，不使用 Result 包装
     */
    @GetMapping("/info")
    public UserInfoVO getUserInfo() {
        log.info("获取当前用户信息");
        return userService.getCurrentUserInfo();
    }

    /**
     * 更新当前登录用户信息
     */
    @PutMapping("/info")
    public Result<Void> updateUserInfo(@RequestBody UserInfoDTO userInfoDTO) {
        log.info("更新用户信息: {}", userInfoDTO);
        userService.updateCurrentUserInfo(userInfoDTO);
        return Result.success();
    }

    /**
     * 修改当前登录用户密码
     */
    @PostMapping("/changePassword")
    public Result<Void> changePassword(@RequestBody ChangePasswordDTO changePasswordDTO) {
        log.info("修改用户密码");
        userService.changePassword(changePasswordDTO);
        return Result.success();
    }
}
