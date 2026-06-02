package com.yeni.backoffice.core.admin.navigation.repository;

import com.yeni.backoffice.core.admin.navigation.entity.AdminNavigationGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminNavigationGroupRepository extends JpaRepository<AdminNavigationGroup, Long> {
    List<AdminNavigationGroup> findByUseYnTrueOrderBySortOrderAscIdAsc();
    boolean existsByGroupCode(String groupCode);
}

