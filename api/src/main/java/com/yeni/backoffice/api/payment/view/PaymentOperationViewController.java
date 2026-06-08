package com.yeni.backoffice.api.payment.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/payment-operations")
public class PaymentOperationViewController {

    @GetMapping
    public String page(Model model) {
        model.addAttribute("title", "PG Operations");
        model.addAttribute("description", "Payment gateway adapter, sales operation and settlement mock console.");
        return "payment/operations";
    }

    @GetMapping("/sales-ledger")
    public String salesLedger(Model model) {
        model.addAttribute("title", "Sales Ledger");
        model.addAttribute("description", "SALE/CANCEL sales ledger generated from payment and cancel results.");
        return "payment/sales-ledger";
    }

    @GetMapping("/settlements")
    public String settlements(Model model) {
        model.addAttribute("title", "Settlement Management");
        model.addAttribute("description", "Settlement statements generated from the sales ledger.");
        return "payment/settlements";
    }
}
