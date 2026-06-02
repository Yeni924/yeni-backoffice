package com.yeni.backoffice.api.admin.auth.view;

import com.yeni.backoffice.api.common.session.SessionConstants;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminAuthViewController {

    @GetMapping("/login")
    public String login(HttpSession session) {
        if (session.getAttribute(SessionConstants.ADMIN_LOGIN_SESSION) != null) {
            return "redirect:/dashboard";
        }
        return "admin/auth/login";
    }
}
