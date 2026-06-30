package com.sambath.admincafe.superadmin;

import com.sambath.admincafe.auth.AuthInterceptor;
import com.sambath.admincafe.auth.AuthService;
import com.sambath.admincafe.auth.dto.AuthUser;
import com.sambath.admincafe.common.ForbiddenException;
import com.sambath.admincafe.common.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SuperAdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        AuthUser user = (AuthUser) request.getAttribute(AuthInterceptor.USER_ATTR);
        if (user == null) {
            throw new UnauthorizedException("Missing authenticated user.");
        }
        if (!AuthService.ROLE_SUPER_ADMIN.equals(user.role())) {
            throw new ForbiddenException("SuperAdmin role required.");
        }
        return true;
    }
}
