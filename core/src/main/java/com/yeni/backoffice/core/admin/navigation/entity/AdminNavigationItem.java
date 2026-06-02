package com.yeni.backoffice.core.admin.navigation.entity;

import com.yeni.backoffice.core.admin.auth.AdminRole;
import com.yeni.backoffice.core.common.entity.BaseSoftDeleteEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "admin_navigation_item")
public class AdminNavigationItem extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "navigation_group_id", nullable = false)
    private AdminNavigationGroup navigationGroup;

    @Column
    private Long parentNavigationItemId;

    @Column(nullable = false)
    private String itemName;

    @Column(nullable = false)
    private String itemUrl;

    @Column
    private String icon;

    @Column(nullable = false)
    private Integer depth;

    @Column(nullable = false)
    private Integer sortOrder;

    @Builder.Default
    @Column(nullable = false)
    private Boolean useYn = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean displayYn = true;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminRole requiredRole = AdminRole.USER;

    public void update(
            AdminNavigationGroup navigationGroup,
            Long parentNavigationItemId,
            String itemName,
            String itemUrl,
            String icon,
            Integer depth,
            Integer sortOrder,
            Boolean useYn,
            Boolean displayYn,
            AdminRole requiredRole) {
        this.navigationGroup = navigationGroup;
        this.parentNavigationItemId = parentNavigationItemId;
        this.itemName = itemName;
        this.itemUrl = itemUrl;
        this.icon = icon;
        this.depth = depth;
        this.sortOrder = sortOrder;
        this.useYn = useYn;
        this.displayYn = displayYn;
        this.requiredRole = requiredRole;
    }

    public void updateSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void updateDisplayYn(Boolean displayYn) {
        this.displayYn = displayYn;
    }

    public void updateUseYn(Boolean useYn) {
        this.useYn = useYn;
    }
}

