package com.yeni.backoffice.api.common.interceptor;

import com.yeni.backoffice.api.common.session.LoginAdmin;
import com.yeni.backoffice.api.common.session.SessionConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        LoginAdmin loginAdmin = getLoginAdmin(request);
        if (loginAdmin != null && loginAdmin.isAdmin()) {
            return true;
        }

        if (request.getRequestURI().startsWith("/api/admin/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        response.sendRedirect("/admin/login");
        return false;
    }

    private LoginAdmin getLoginAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SessionConstants.ADMIN_LOGIN_SESSION);
        return value instanceof LoginAdmin loginAdmin ? loginAdmin : null;
    }
}
