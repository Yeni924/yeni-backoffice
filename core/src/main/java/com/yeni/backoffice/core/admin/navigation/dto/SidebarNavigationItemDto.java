package com.yeni.backoffice.core.admin.navigation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SidebarNavigationItemDto {
    private Long id;
    private String itemName;
    private String itemUrl;
    private String icon;
    private Integer sortOrder;
    private boolean active;
}

