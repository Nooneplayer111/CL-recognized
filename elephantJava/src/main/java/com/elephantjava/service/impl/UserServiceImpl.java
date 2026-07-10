package com.elephantjava.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elephantjava.Exception.AccountNotFoundException;
import com.elephantjava.Exception.PasswordErrorException;
import com.elephantjava.contex.BaseContext;
import com.elephantjava.dto.ChangePasswordDTO;
import com.elephantjava.dto.UserInfoDTO;
import com.elephantjava.dto.UserLoginDTO;
import com.elephantjava.entity.User;
import com.elephantjava.mapper.UserMapper;
import com.elephantjava.service.IUserService;
import com.elephantjava.vo.UserInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private UserMapper userMapper;

    /**
     * 登录功能
     */
    @Override
    public User login(UserLoginDTO userLoginDTO) {
        String username = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();

        // 1、根据用户名查询数据库中的数据
        User user = this.getOne(new QueryWrapper<User>().eq("username", username));

        // 2、处理各种异常情况（用户名不存在、密码不对）
        if (user == null) {
            throw new AccountNotFoundException("账号不存在");
        }

        // 密码比对：对前端传过来的明文密码进行 MD5 加密后比对
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(user.getPassword())) {
            throw new PasswordErrorException("密码错误");
        }

        return user;
    }

    /**
     * 获取当前登录用户信息
     */
    @Override
    public UserInfoVO getCurrentUserInfo() {
        User user = getCurrentUserOrThrow();
        return convertToUserInfoVO(user);
    }

    /**
     * 更新当前登录用户信息
     */
    @Override
    public void updateCurrentUserInfo(UserInfoDTO userInfoDTO) {
        User user = getCurrentUserOrThrow();

        if (StringUtils.hasText(userInfoDTO.getRealName())) {
            user.setRealName(userInfoDTO.getRealName());
        }
        if (StringUtils.hasText(userInfoDTO.getEmail())) {
            user.setEmail(userInfoDTO.getEmail());
        }
        if (StringUtils.hasText(userInfoDTO.getPhone())) {
            user.setPhone(userInfoDTO.getPhone());
        }
        if (StringUtils.hasText(userInfoDTO.getDepartment())) {
            user.setDepartment(userInfoDTO.getDepartment());
        }
        if (StringUtils.hasText(userInfoDTO.getJoinDate())) {
            user.setJoinDate(LocalDate.parse(userInfoDTO.getJoinDate(), DATE_FORMATTER));
        }

        this.updateById(user);
    }

    /**
     * 修改当前登录用户密码
     */
    @Override
    public void changePassword(ChangePasswordDTO changePasswordDTO) {
        if (!StringUtils.hasText(changePasswordDTO.getOldPassword())) {
            throw new IllegalArgumentException("旧密码不能为空");
        }
        if (!StringUtils.hasText(changePasswordDTO.getNewPassword())) {
            throw new IllegalArgumentException("新密码不能为空");
        }
        if (changePasswordDTO.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("新密码长度不能少于6位");
        }
        if (!changePasswordDTO.getNewPassword().equals(changePasswordDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("两次输入的新密码不一致");
        }

        User user = getCurrentUserOrThrow();

        String oldPasswordMd5 = DigestUtils.md5DigestAsHex(changePasswordDTO.getOldPassword().getBytes());
        if (!oldPasswordMd5.equals(user.getPassword())) {
            throw new PasswordErrorException("旧密码错误");
        }

        user.setPassword(DigestUtils.md5DigestAsHex(changePasswordDTO.getNewPassword().getBytes()));
        this.updateById(user);
    }

    /**
     * 获取当前登录用户，未登录时抛异常
     */
    private User getCurrentUserOrThrow() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new AccountNotFoundException("用户未登录");
        }
        User user = this.getById(userId);
        if (user == null) {
            throw new AccountNotFoundException("用户不存在");
        }
        return user;
    }

    /**
     * 用户实体转用户信息 VO
     */
    private UserInfoVO convertToUserInfoVO(User user) {
        String joinDate = user.getJoinDate() == null ? null : user.getJoinDate().format(DATE_FORMATTER);
        return UserInfoVO.builder()
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .department(user.getDepartment())
                .joinDate(joinDate)
                .build();
    }
}
