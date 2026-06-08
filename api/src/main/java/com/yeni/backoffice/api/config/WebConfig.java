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
                        "/admin/payment-operations",
                        "/admin/payment-operations/sales-ledger",
                        "/admin/payment-operations/settlements",
                        "/admin/sales-ledger",
                        "/admin/database-spec",
                        "/admin/api/sales-ledger/**",
                        "/admin/api/recovery/tasks/**",
                        "/api/payment-bridge/**",
                        "/api/admin/auth/**",
                        "/api/admin/sales/**",
                        "/api/admin/external-send-requests/**",
                        "/api/admin/alimtalk-queues/**",
                        "/api/admin/payment-recovery-tasks/**",
                        "/api/admin/pg-fee-policies/**",
                        "/api/admin/payment-statistics/**",
                        "/api/admin/settlements/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/h2-console/**"
                );
    }
}
