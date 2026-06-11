package com.yeni.backoffice.api.common.advice;

import com.yeni.backoffice.core.admin.auth.AdminRole;
import com.yeni.backoffice.core.admin.navigation.dto.SidebarNavigationGroupDto;
import com.yeni.backoffice.core.admin.navigation.service.AdminNavigationService;
import jakarta.servlet.http.HttpServletRequest;
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

    @ModelAttribute("sidebarNavigationGroups")
    public List<SidebarNavigationGroupDto> sidebarNavigationGroups() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null || request.getRequestURI().startsWith("/api/")) {
            return Collections.emptyList();
        }

        return navigationService.getSidebarNavigationGroups(request.getRequestURI(), AdminRole.ADMIN);
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }
}
