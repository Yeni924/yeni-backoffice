package com.yeni.backoffice.api.common.advice;

import com.yeni.backoffice.api.common.session.LoginAdmin;
import com.yeni.backoffice.api.common.session.SessionConstants;
import com.yeni.backoffice.core.admin.auth.AdminRole;
import com.yeni.backoffice.core.admin.navigation.dto.SidebarNavigationGroupDto;
import com.yeni.backoffice.core.admin.navigation.service.AdminNavigationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;

@ControllerAdvice
public class GlobalModelAttributeAdvice {

    private final AdminNavigationService navigationService;

    public GlobalModelAttributeAdvice(AdminNavigationService navigationService) {
        this.navigationService = navigationService;
    }

    @ModelAttribute("loginAdmin")
    public LoginAdmin loginAdmin() {
        return getLoginAdmin();
    }

    @ModelAttribute("isLoggedIn")
    public boolean isLoggedIn() {
        return getLoginAdmin() != null;
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin() {
        LoginAdmin loginAdmin = getLoginAdmin();
        return loginAdmin != null && loginAdmin.isAdmin();
    }

    @ModelAttribute("sidebarNavigationGroups")
    public List<SidebarNavigationGroupDto> sidebarNavigationGroups() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null || request.getRequestURI().startsWith("/api/")) {
            return Collections.emptyList();
        }

        LoginAdmin loginAdmin = getLoginAdmin(request);
        AdminRole role = loginAdmin == null ? AdminRole.USER : loginAdmin.getRole();
        return navigationService.getSidebarNavigationGroups(request.getRequestURI(), role);
    }

    private LoginAdmin getLoginAdmin() {
        HttpServletRequest request = getCurrentRequest();
        return request == null ? null : getLoginAdmin(request);
    }

    private LoginAdmin getLoginAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SessionConstants.ADMIN_LOGIN_SESSION);
        return value instanceof LoginAdmin loginAdmin ? loginAdmin : null;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
