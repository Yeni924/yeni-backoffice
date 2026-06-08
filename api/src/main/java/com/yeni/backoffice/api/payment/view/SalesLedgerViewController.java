package com.yeni.backoffice.api.payment.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SalesLedgerViewController {

    @GetMapping("/admin/sales-ledger")
    public String salesLedger(Model model) {
        model.addAttribute("title", "매출 원장");
        model.addAttribute("description", "확정된 SALE/CANCEL 거래를 누적 기록하는 매출 원장입니다.");
        return "payment/sales-ledger";
    }
}
