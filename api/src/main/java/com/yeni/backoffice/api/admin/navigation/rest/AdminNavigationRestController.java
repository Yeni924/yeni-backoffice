package com.yeni.backoffice.api.admin.navigation.rest;

import com.yeni.backoffice.core.admin.navigation.dto.AdminNavigationListItemDto;
import com.yeni.backoffice.core.admin.navigation.dto.AdminNavigationSaveRequest;
import com.yeni.backoffice.core.admin.navigation.dto.AdminNavigationStatusUpdateRequest;
import com.yeni.backoffice.core.admin.navigation.dto.AdminNavigationUpdateRequest;
import com.yeni.backoffice.core.admin.navigation.service.AdminNavigationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/navigation")
public class AdminNavigationRestController {

    private final AdminNavigationService navigationService;

    public AdminNavigationRestController(AdminNavigationService navigationService) {
        this.navigationService = navigationService;
    }

    @GetMapping
    public List<AdminNavigationListItemDto> list() {
        return navigationService.getNavigationListForAdmin();
    }

    @GetMapping("/{id}")
    public AdminNavigationListItemDto get(@PathVariable Long id) {
        return navigationService.getNavigationItemForAdmin(id);
    }

    @PostMapping
    public ResponseEntity<Void> save(@RequestBody AdminNavigationSaveRequest request) {
        navigationService.saveNavigation(request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable Long id,
            @RequestBody AdminNavigationUpdateRequest request) {
        AdminNavigationUpdateRequest updateRequest = new AdminNavigationUpdateRequest(
                id,
                request.getNavigationGroupId(),
                request.getParentNavigationItemId(),
                request.getItemName(),
                request.getItemUrl(),
                request.getIcon(),
                request.getDepth(),
                request.getSortOrder(),
                request.getUseYn(),
                request.getDisplayYn(),
                request.getRequiredRole()
        );
        navigationService.updateNavigation(updateRequest);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/use-yn")
    public ResponseEntity<Void> updateUseYn(
            @PathVariable Long id,
            @RequestBody AdminNavigationStatusUpdateRequest request) {
        navigationService.updateUseYn(id, request.getEnabled());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/display-yn")
    public ResponseEntity<Void> updateDisplayYn(
            @PathVariable Long id,
            @RequestBody AdminNavigationStatusUpdateRequest request) {
        navigationService.updateDisplayYn(id, request.getEnabled());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/sort-order")
    public ResponseEntity<Void> updateSortOrder(
            @PathVariable Long id,
            @RequestBody AdminNavigationUpdateRequest request) {
        navigationService.updateSortOrder(id, request.getSortOrder());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        navigationService.softDeleteNavigation(id);
        return ResponseEntity.ok().build();
    }
}
