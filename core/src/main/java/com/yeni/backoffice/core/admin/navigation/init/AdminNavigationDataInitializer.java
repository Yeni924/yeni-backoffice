package com.yeni.backoffice.core.admin.navigation.init;

import com.yeni.backoffice.core.admin.auth.AdminRole;
import com.yeni.backoffice.core.admin.navigation.entity.AdminNavigationGroup;
import com.yeni.backoffice.core.admin.navigation.entity.AdminNavigationItem;
import com.yeni.backoffice.core.admin.navigation.repository.AdminNavigationGroupRepository;
import com.yeni.backoffice.core.admin.navigation.repository.AdminNavigationItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;

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
    @Transactional
    public void run(String... args) {
        if (navigationGroupRepository.count() > 0 || navigationItemRepository.count() > 0) {
            normalizePortfolioMenus();
            return;
        }

        AdminNavigationGroup portfolioGroup = new AdminNavigationGroup(null, "PORTFOLIO", "PORTFOLIO", 1, true);
        AdminNavigationGroup commerceGroup = new AdminNavigationGroup(null, "COMMERCE_ADMIN", "COMMERCE ADMIN", 2, true);
        AdminNavigationGroup operationGroup = new AdminNavigationGroup(null, "OPERATION", "OPERATION", 3, true);
        navigationGroupRepository.saveAll(Arrays.asList(portfolioGroup, commerceGroup, operationGroup));

        navigationItemRepository.saveAll(Arrays.asList(
                item(portfolioGroup, "Overview", "/dashboard", "OV", 1, AdminRole.USER),
                item(portfolioGroup, "PG Operations", "/admin/payment-operations", "PG", 2, AdminRole.USER),
                item(portfolioGroup, "DB Specification", "/admin/database-spec", "DB", 3, AdminRole.USER),
                item(operationGroup, "Menu Management", "/admin/navigation", "MN", 1, AdminRole.ADMIN)
        ));
    }

    private void normalizePortfolioMenus() {
        Set<String> visibleUrls = Set.of(
                "/dashboard",
                "/admin/payment-operations",
                "/admin/database-spec",
                "/admin/navigation"
        );

        navigationItemRepository.findAllNotDeleted().forEach(item -> {
            if (!visibleUrls.contains(item.getItemUrl())) {
                item.softDelete();
            }
        });

        AdminNavigationGroup portfolioGroup = navigationGroupRepository.findByGroupCode("PORTFOLIO")
                .orElseGet(() -> navigationGroupRepository.save(
                        new AdminNavigationGroup(null, "PORTFOLIO", "PORTFOLIO", 1, true)
                ));
        AdminNavigationGroup operationGroup = navigationGroupRepository.findByGroupCode("OPERATION")
                .orElseGet(() -> navigationGroupRepository.save(
                        new AdminNavigationGroup(null, "OPERATION", "OPERATION", 3, true)
                ));

        ensureMenu(portfolioGroup, "Overview", "/dashboard", "OV", 1, AdminRole.USER);
        ensureMenu(portfolioGroup, "PG Operations", "/admin/payment-operations", "PG", 2, AdminRole.USER);
        ensureMenu(portfolioGroup, "DB Specification", "/admin/database-spec", "DB", 3, AdminRole.USER);
        ensureMenu(operationGroup, "Menu Management", "/admin/navigation", "MN", 1, AdminRole.ADMIN);
    }

    private void ensureMenu(
            AdminNavigationGroup group,
            String itemName,
            String itemUrl,
            String icon,
            Integer sortOrder,
            AdminRole requiredRole) {
        navigationItemRepository.findByItemUrlAndIsDeletedFalse(itemUrl)
                .ifPresentOrElse(
                        existingItem -> existingItem.update(group, null, itemName, itemUrl, icon, 1, sortOrder, true, true, requiredRole),
                        () -> navigationItemRepository.save(item(group, itemName, itemUrl, icon, sortOrder, requiredRole))
                );
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
