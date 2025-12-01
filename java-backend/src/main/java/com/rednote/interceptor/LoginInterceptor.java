package com.rednote.interceptor;

import com.rednote.common.UserContext;
import com.rednote.utils.JwtUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 放行 OPTIONS 请求 (CORS 预检请求)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 获取 Header 中的 Token
        // 约定：前端 Header 传 "Authorization: Bearer <token>"
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            response.setStatus(401); // 401 未授权
            return false;
        }

        String token = header.substring(7); // 去掉 "Bearer " 前缀

        // 3. 校验 Token
        try {
            if (JwtUtils.validateToken(token)) {
                // 4. 解析成功，将 userId 存入 ThreadLocal
                Long userId = JwtUtils.getUserId(token);
                UserContext.setUserId(userId);
                return true; // 放行
            }
        } catch (Exception e) {
            // 解析失败
        }

        response.setStatus(401);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 5. 请求结束，清理 ThreadLocal，防止内存泄漏
        UserContext.clear();
    }
}