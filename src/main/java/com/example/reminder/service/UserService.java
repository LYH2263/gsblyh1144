package com.example.reminder.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.reminder.entity.SysUser;
import com.example.reminder.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private SysUserMapper userMapper;

    public SysUser login(String username, String password) {
        SysUser user = userMapper.selectOne(
                new QueryWrapper<SysUser>()
                        .eq("username", username)
                        .eq("password", password)
                        .eq("status", 1));
        return user;
    }

    public SysUser getUserById(Long id) {
        return userMapper.selectById(id);
    }
}
