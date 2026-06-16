package com.yeni.backoffice.api.commerce.rest;

import com.yeni.backoffice.core.commerce.dto.CommerceOrderDtos.CommerceOrderCreateRequest;
import com.yeni.backoffice.core.commerce.dto.CommerceOrderDtos.CommerceOrderResponse;
import com.yeni.backoffice.core.commerce.dto.CommerceOrderDtos.CommerceOrderSummaryResponse;
import com.yeni.backoffice.core.commerce.service.CommerceOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/api/commerce/orders")
public class CommerceOrderRestController {

    private final CommerceOrderService orderService;

    public CommerceOrderRestController(CommerceOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<CommerceOrderResponse>> orders() {
        return ResponseEntity.ok(orderService.getOrders());
    }

    @GetMapping("/summary")
    public ResponseEntity<CommerceOrderSummaryResponse> summary() {
        return ResponseEntity.ok(orderService.getSummary());
    }

    @PostMapping
    public ResponseEntity<CommerceOrderResponse> create(@Valid @RequestBody CommerceOrderCreateRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @PostMapping("/mock")
    public ResponseEntity<CommerceOrderResponse> createMock() {
        return ResponseEntity.ok(orderService.createMockOrder());
    }

    @PostMapping("/{orderId}/pay")
    public ResponseEntity<CommerceOrderResponse> pay(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.approvePayment(orderId));
    }
}
