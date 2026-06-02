package com.yeni.backoffice.core.admin.navigation.init;

import com.yeni.backoffice.core.admin.auth.AdminRole;
import com.yeni.backoffice.core.admin.navigation.entity.AdminNavigationGroup;
import com.yeni.backoffice.core.admin.navigation.entity.AdminNavigationItem;
import com.yeni.backoffice.core.admin.navigation.repository.AdminNavigationGroupRepository;
import com.yeni.backoffice.core.admin.navigation.repository.AdminNavigationItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class AdminNavigationDataInitializer implements CommandLineRunner {

    private final AdminNavigationGroupRepository navigationGroupRepository;
    private final AdminNavigationItemRepository navigationItemRepository;

    public AdminNavigationDataInitializer(
            AdminNavigationGroupRepository navigationGroupRepository,
            AdminNavigationItemRepository navigationItemRepository) {
        this.navigationGroupRepository = navigationGroupRepository;
        this.navigationItemRepository = navigationItemRepository;
    }

    @Override
    public void run(String... args) {
        if (navigationGroupRepository.count() > 0 || navigationItemRepository.count() > 0) {
            return;
        }

        AdminNavigationGroup portfolioGroup = new AdminNavigationGroup(null, "PORTFOLIO", "PORTFOLIO", 1, true);
        AdminNavigationGroup commerceGroup = new AdminNavigationGroup(null, "COMMERCE_ADMIN", "COMMERCE ADMIN", 2, true);
        AdminNavigationGroup operationGroup = new AdminNavigationGroup(null, "OPERATION", "OPERATION", 3, true);
        navigationGroupRepository.saveAll(Arrays.asList(portfolioGroup, commerceGroup, operationGroup));

        navigationItemRepository.saveAll(Arrays.asList(
                item(portfolioGroup, "Overview", "/dashboard", "OV", 1, AdminRole.USER),
                item(portfolioGroup, "Profile", "/profile", "PF", 2, AdminRole.USER),
                item(portfolioGroup, "Resume", "/resume", "RS", 3, AdminRole.USER),
                item(portfolioGroup, "Career Projects", "/career-projects", "CP", 4, AdminRole.USER),

                item(commerceGroup, "Category Management", "/categories", "CT", 1, AdminRole.ADMIN),
                item(commerceGroup, "Product Management", "/products", "PR", 2, AdminRole.ADMIN),
                item(commerceGroup, "Order Management", "/orders", "OR", 3, AdminRole.ADMIN),
                item(commerceGroup, "Payment Management", "/payments", "PA", 4, AdminRole.ADMIN),
                item(commerceGroup, "Delivery Management", "/deliveries", "DL", 5, AdminRole.ADMIN),
                item(commerceGroup, "Promotion Management", "/promotions", "PM", 6, AdminRole.ADMIN),

                item(operationGroup, "Notification Queue", "/notifications", "NQ", 1, AdminRole.ADMIN),
                item(operationGroup, "API Logs", "/api-logs", "AL", 2, AdminRole.ADMIN),
                item(operationGroup, "System Settings", "/settings", "ST", 3, AdminRole.ADMIN),
                item(operationGroup, "Menu Management", "/admin/navigation", "MN", 4, AdminRole.ADMIN)
        ));
    }

    private AdminNavigationItem item(
            AdminNavigationGroup group,
            String itemName,
            String itemUrl,
            String icon,
            Integer sortOrder,
            AdminRole requiredRole) {
        return AdminNavigationItem.builder()
                .navigationGroup(group)
                .itemName(itemName)
                .itemUrl(itemUrl)
                .icon(icon)
                .depth(1)
                .sortOrder(sortOrder)
                .useYn(true)
                .displayYn(true)
                .requiredRole(requiredRole)
                .build();
    }
}
