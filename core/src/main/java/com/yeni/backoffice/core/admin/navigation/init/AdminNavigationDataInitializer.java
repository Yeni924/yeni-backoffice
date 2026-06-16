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

        AdminNavigationGroup portfolioGroup = new AdminNavigationGroup(null, "PORTFOLIO", "포트폴리오", 1, true);
        AdminNavigationGroup commerceGroup = new AdminNavigationGroup(null, "COMMERCE_ADMIN", "커머스 관리", 2, true);
        AdminNavigationGroup operationGroup = new AdminNavigationGroup(null, "OPERATION", "운영 관리", 3, true);
        navigationGroupRepository.saveAll(Arrays.asList(portfolioGroup, commerceGroup, operationGroup));

        navigationItemRepository.saveAll(Arrays.asList(
                item(portfolioGroup, "대시보드", "/dashboard", "홈", 1, AdminRole.USER),
                item(portfolioGroup, "PG 운영", "/admin/payment-operations", "PG", 2, AdminRole.USER),
                item(portfolioGroup, "주문 관리", "/admin/commerce/orders", "주", 3, AdminRole.USER),
                item(portfolioGroup, "매출 원장", "/admin/payment-operations/sales-ledger", "매", 4, AdminRole.USER),
                item(portfolioGroup, "정산 관리", "/admin/payment-operations/settlements", "정", 5, AdminRole.USER),
                item(portfolioGroup, "DB 명세", "/admin/database-spec", "DB", 6, AdminRole.USER),
                item(operationGroup, "메뉴 관리", "/admin/navigation", "메", 1, AdminRole.ADMIN)
        ));
    }

    private void normalizePortfolioMenus() {
        Set<String> visibleUrls = Set.of(
                "/dashboard",
                "/admin/payment-operations",
                "/admin/commerce/orders",
                "/admin/payment-operations/sales-ledger",
                "/admin/payment-operations/settlements",
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
                        new AdminNavigationGroup(null, "PORTFOLIO", "포트폴리오", 1, true)
                ));
        AdminNavigationGroup operationGroup = navigationGroupRepository.findByGroupCode("OPERATION")
                .orElseGet(() -> navigationGroupRepository.save(
                        new AdminNavigationGroup(null, "OPERATION", "운영 관리", 3, true)
                ));

        ensureMenu(portfolioGroup, "대시보드", "/dashboard", "홈", 1, AdminRole.USER);
        ensureMenu(portfolioGroup, "PG 운영", "/admin/payment-operations", "PG", 2, AdminRole.USER);
        ensureMenu(portfolioGroup, "주문 관리", "/admin/commerce/orders", "주", 3, AdminRole.USER);
        ensureMenu(portfolioGroup, "매출 원장", "/admin/payment-operations/sales-ledger", "매", 4, AdminRole.USER);
        ensureMenu(portfolioGroup, "정산 관리", "/admin/payment-operations/settlements", "정", 5, AdminRole.USER);
        ensureMenu(portfolioGroup, "DB 명세", "/admin/database-spec", "DB", 6, AdminRole.USER);
        ensureMenu(operationGroup, "메뉴 관리", "/admin/navigation", "메", 1, AdminRole.ADMIN);
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
