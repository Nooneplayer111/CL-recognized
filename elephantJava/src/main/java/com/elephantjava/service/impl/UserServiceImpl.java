package com.elephantjava.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.elephantjava.Exception.AccountNotFoundException;
import com.elephantjava.Exception.PasswordErrorException;
import com.elephantjava.dto.UserLoginDTO;
import com.elephantjava.entity.User;
import com.elephantjava.mapper.UserMapper;
import com.elephantjava.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;
    /**
     * 登录功能
     * @param userLoginDTO
     * @return
     */
    @Override
    public User login(UserLoginDTO userLoginDTO) {
        String username = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        User user = this.getOne(new QueryWrapper<User>().eq("username", username));

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (user == null) {
            //账号不存在
            throw new AccountNotFoundException("账号不存在");
        }

        //密码比对
        //对前端传过来的明文密码进行md5加密处理
        //调用DigestUtils的md5DigestAsHex方法对密码进行md5加密(要把原密码转成Bytes类型的数组)
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(user.getPassword())) {
            //密码错误
            throw new PasswordErrorException("密码错误");
        }


        //3、返回实体对象
        return user;

    }
}
