package com.sambath.admincafe.tenant;

import com.sambath.admincafe.common.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class GuestTenantInterceptor implements HandlerInterceptor {

    public static final String TENANT_ATTR = "tenant.current";

    private static final Pattern SLUG_PATTERN = Pattern.compile("^/api/guest/t/([^/]+)(/.*)?$");

    private final TenantRepository tenantRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        Matcher m = SLUG_PATTERN.matcher(path);
        if (!m.matches()) {
            throw new NotFoundException("Tenant not specified in path.");
        }
        String slug = m.group(1);
        Tenant tenant = tenantRepository.findBySlug(slug)
                .filter(Tenant::isActive)
                .orElseThrow(() -> new NotFoundException("Unknown tenant: " + slug));
        request.setAttribute(TENANT_ATTR, tenant);
        TenantContext.set(tenant.getId());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
