package com.demo.courseplatform.security;

import com.demo.courseplatform.common.ForbiddenException;
import com.demo.courseplatform.common.UnauthorizedException;
import com.demo.courseplatform.domain.entity.PersistenceModels.UserEntity;
import com.demo.courseplatform.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final UserMapper userMapper;

    public AuthInterceptor(TokenService tokenService, UserMapper userMapper) {
        this.tokenService = tokenService;
        this.userMapper = userMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Let browser CORS preflight requests pass through without token validation.
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("missing token");
        }
        String token = authorization.substring(7);
        Long userId = tokenService.resolveUserId(token)
            .orElseThrow(() -> new UnauthorizedException("invalid token"));
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            throw new UnauthorizedException("invalid token");
        }

        enforceRole(user, request.getRequestURI());
        enforceFirstLoginReset(user, request.getRequestURI());

        request.setAttribute("currentUserId", userId);
        request.setAttribute("currentUserRole", user.role);
        request.setAttribute("currentToken", token);
        return true;
    }

    private void enforceRole(UserEntity user, String requestUri) {
        if (requestUri.startsWith("/api/v1/admin/") && !"ADMIN".equals(user.role)) {
            throw new ForbiddenException("无权访问管理员接口");
        }
        if (requestUri.startsWith("/api/v1/teacher/")
            && !("TEACHER".equals(user.role) || "ADMIN".equals(user.role))) {
            throw new ForbiddenException("无权访问教师接口");
        }
        if (requestUri.startsWith("/api/v1/student/") && !"STUDENT".equals(user.role)) {
            throw new ForbiddenException("无权访问学生接口");
        }
    }

    private void enforceFirstLoginReset(UserEntity user, String requestUri) {
        if (!user.firstLoginResetRequired) {
            return;
        }
        if (requestUri.startsWith("/api/v1/auth/change-password")
            || requestUri.startsWith("/api/v1/auth/logout")
            || requestUri.startsWith("/api/v1/auth/me")) {
            return;
        }
        throw new ForbiddenException("首次登录请先修改密码");
    }
}
