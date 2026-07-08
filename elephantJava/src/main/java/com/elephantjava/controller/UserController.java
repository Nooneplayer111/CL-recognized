package com.elephantjava.controller;

import com.elephantjava.constant.JwtClaimsConstant;
import com.elephantjava.dto.UserLoginDTO;
import com.elephantjava.entity.User;
import com.elephantjava.properties.JwtProperties;
import com.elephantjava.result.Result;
import com.elephantjava.service.IUserService;
import com.elephantjava.utils.JwtUtil;
import com.elephantjava.vo.UserLoginVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

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
     * @param userLoginDTO
     * @return
     */
    @PostMapping("/login")
    public Result<UserLoginVo> login(@RequestBody UserLoginDTO userLoginDTO){
        log.info("员工登录: {}", userLoginDTO);
        User user = userService.login(userLoginDTO);

        //登录成功后，生成jwt令牌
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

}
