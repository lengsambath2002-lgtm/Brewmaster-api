package com.sambath.admincafe.auth;

import com.sambath.admincafe.auth.dto.AuthUser;
import com.sambath.admincafe.common.UnauthorizedException;
import com.sambath.admincafe.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String USER_ATTR = "auth.user";
    public static final String TOKEN_ATTR = "auth.token";

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenStore tokenStore;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("Missing Authorization header.");
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        AuthUser user = tokenStore.resolve(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired token."));
        request.setAttribute(USER_ATTR, user);
        request.setAttribute(TOKEN_ATTR, token);
        TenantContext.set(user.tenantId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
