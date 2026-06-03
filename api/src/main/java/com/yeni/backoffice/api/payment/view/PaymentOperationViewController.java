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
}
