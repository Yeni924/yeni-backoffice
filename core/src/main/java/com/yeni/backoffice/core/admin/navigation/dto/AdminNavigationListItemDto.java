package com.yeni.backoffice.core.admin.navigation.dto;

import com.yeni.backoffice.core.admin.auth.AdminRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminNavigationListItemDto {
    private Long id;
    private Long groupId;
    private String groupCode;
    private String groupName;
    private Long parentNavigationItemId;
    private String icon;
    private String itemName;
    private String itemUrl;
    private Integer depth;
    private Integer sortOrder;
    private Boolean useYn;
    private Boolean displayYn;
    private AdminRole requiredRole;
}
