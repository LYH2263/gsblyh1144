package com.example.reminder.controller;

import com.example.reminder.entity.SysUser;
import com.example.reminder.service.UserService;
import com.example.reminder.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> params, HttpSession session) {
        String username = params.get("username");
        String password = params.get("password");

        if (username == null || password == null) {
            return Result.error("用户名和密码不能为空");
        }

        SysUser user = userService.login(username, password);
        if (user == null) {
            return Result.error("用户名或密码错误");
        }

        // 不返回密码
        user.setPassword(null);
        session.setAttribute("currentUser", user);

        return Result.success("登录成功", user);
    }

    @PostMapping("/logout")
    public Result<?> logout(HttpSession session) {
        session.invalidate();
        return Result.success("登出成功");
    }

    @GetMapping("/current")
    public Result<?> getCurrentUser(HttpSession session) {
        SysUser user = (SysUser) session.getAttribute("currentUser");
        if (user == null) {
            return Result.error(401, "未登录");
        }
        return Result.success(user);
    }
}
