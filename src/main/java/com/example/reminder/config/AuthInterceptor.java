package com.example.reminder.config;

import com.example.reminder.entity.SysUser;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String uri = request.getRequestURI();

        // 放行登录接口和静态资源
        if (uri.equals("/api/user/login") || !uri.startsWith("/api/")) {
            return true;
        }

        // OPTIONS预检请求放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        SysUser user = (SysUser) request.getSession().getAttribute("currentUser");
        if (user == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter writer = response.getWriter();
            writer.write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
            writer.flush();
            return false;
        }
        return true;
    }
}
