package com.yeni.backoffice.core.admin.navigation.dto;

import com.yeni.backoffice.core.admin.auth.AdminRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminNavigationUpdateRequest {
    private Long id;
    private Long navigationGroupId;
    private Long parentNavigationItemId;
    private String itemName;
    private String itemUrl;
    private String icon;
    private Integer depth;
    private Integer sortOrder;
    private Boolean useYn;
    private Boolean displayYn;
    private AdminRole requiredRole;
}
