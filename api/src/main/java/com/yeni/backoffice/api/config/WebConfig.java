package com.yeni.backoffice.api.config;

import com.yeni.backoffice.api.common.interceptor.AdminAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;

    public WebConfig(AdminAuthInterceptor adminAuthInterceptor) {
        this.adminAuthInterceptor = adminAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/admin/**", "/api/admin/**")
                .excludePathPatterns(
                        "/admin/login",
                        "/api/admin/auth/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/h2-console/**"
                );
    }
}
