package com.yeni.backoffice.api.admin.auth.rest;

import com.yeni.backoffice.api.common.session.LoginAdmin;
import com.yeni.backoffice.api.common.session.SessionConstants;
import com.yeni.backoffice.core.admin.auth.dto.AdminLoginRequest;
import com.yeni.backoffice.core.admin.auth.dto.AdminLoginResult;
import com.yeni.backoffice.core.admin.auth.service.AdminAuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthRestController {

    private final AdminAuthService adminAuthService;

    public AdminAuthRestController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody AdminLoginRequest request,
            HttpSession session) {
        AdminLoginResult result = adminAuthService.login(request.getLoginId(), request.getPassword());
        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", result.getMessage()));
        }

        LoginAdmin loginAdmin = LoginAdmin.from(result.getLoginAdmin());
        session.setAttribute(SessionConstants.ADMIN_LOGIN_SESSION, loginAdmin);
        return ResponseEntity.ok(Map.of("success", true, "loginAdmin", loginAdmin));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.invalidate();
        return Map.of("success", true);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Object loginAdmin = session.getAttribute(SessionConstants.ADMIN_LOGIN_SESSION);
        if (loginAdmin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Not logged in."));
        }
        return ResponseEntity.ok(loginAdmin);
    }
}
