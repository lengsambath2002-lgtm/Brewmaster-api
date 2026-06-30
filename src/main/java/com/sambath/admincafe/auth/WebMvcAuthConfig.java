package com.sambath.admincafe.auth;

import com.sambath.admincafe.superadmin.SuperAdminInterceptor;
import com.sambath.admincafe.tenant.GuestTenantInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcAuthConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final GuestTenantInterceptor guestTenantInterceptor;
    private final SuperAdminInterceptor superAdminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/login",
                        "/api/health",
                        "/api/guest/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                );

        registry.addInterceptor(superAdminInterceptor)
                .addPathPatterns("/api/superadmin/**");

        registry.addInterceptor(guestTenantInterceptor)
                .addPathPatterns("/api/guest/**");
    }
}
