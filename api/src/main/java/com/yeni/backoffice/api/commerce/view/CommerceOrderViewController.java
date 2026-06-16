package com.yeni.backoffice.api.commerce.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/commerce")
public class CommerceOrderViewController {

    @GetMapping("/orders")
    public String orders(Model model) {
        model.addAttribute("title", "주문 관리");
        model.addAttribute("description", "쇼핑몰 주문 생성부터 결제 승인, 매출 원장 반영까지 이어지는 흐름을 확인합니다.");
        return "commerce/orders";
    }
}
