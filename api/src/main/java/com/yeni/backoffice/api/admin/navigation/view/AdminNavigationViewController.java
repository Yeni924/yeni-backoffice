package com.yeni.backoffice.api.admin.navigation.view;

import com.yeni.backoffice.core.admin.navigation.service.AdminNavigationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/navigation")
public class AdminNavigationViewController {

    private final AdminNavigationService navigationService;

    public AdminNavigationViewController(AdminNavigationService navigationService) {
        this.navigationService = navigationService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("navigationItems", navigationService.getNavigationListForAdmin());
        model.addAttribute("navigationGroups", navigationService.getNavigationGroupOptions());
        model.addAttribute("title", "Menu Management");
        model.addAttribute("description", "Create, edit, and display sidebar navigation items.");
        return "admin/navigation/list";
    }
}
