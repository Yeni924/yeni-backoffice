package com.yeni.backoffice.core.admin.navigation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SidebarNavigationGroupDto {
    private String groupCode;
    private String groupName;
    private Integer sortOrder;
    private List<SidebarNavigationItemDto> items;
}

