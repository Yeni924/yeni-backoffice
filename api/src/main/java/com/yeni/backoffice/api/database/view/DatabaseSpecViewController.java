package com.yeni.backoffice.api.database.view;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/database-spec")
public class DatabaseSpecViewController {

    private final DatabaseSpecService databaseSpecService;

    @GetMapping
    public String page(Model model) {
        model.addAttribute("title", "DB 명세");
        model.addAttribute("description", "결제, 취소, 매출 원장, 외부전송, 알림톡, 복구 작업, 정산 테이블의 역할과 컬럼 구조를 확인합니다.");
        model.addAttribute("tableSpecs", databaseSpecService.getTableSpecs());
        return "database/spec";
    }
}
