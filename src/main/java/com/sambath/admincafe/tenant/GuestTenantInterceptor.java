package com.sambath.admincafe.tenant;

import com.sambath.admincafe.common.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class GuestTenantInterceptor implements HandlerInterceptor {

    public static final String TENANT_ATTR = "tenant.current";

    private static final String DEFAULT_SLUG = "default";

    private final TenantRepository tenantRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        Tenant tenant = tenantRepository.findBySlug(DEFAULT_SLUG)
                .filter(Tenant::isActive)
                .orElseThrow(() -> new NotFoundException("Default tenant not configured."));
        request.setAttribute(TENANT_ATTR, tenant);
        TenantContext.set(tenant.getId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
