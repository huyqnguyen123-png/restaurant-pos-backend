package com.restaurantpos.pos_backend.controller;

import com.restaurantpos.pos_backend.dto.CreatePaymentRequest;
import com.restaurantpos.pos_backend.dto.OrderResponse;
import com.restaurantpos.pos_backend.dto.PaymentResponse;
import com.restaurantpos.pos_backend.dto.UpdatePaymentMethodRequest;
import com.restaurantpos.pos_backend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders/{orderId}/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<OrderResponse> createPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPayment(orderId, request));
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getPayments(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentsForOrder(orderId));
    }

    // PUT /api/orders/{orderId}/payments/method -> correct a mistaken payment method (CASH <-> CARD)
    @PutMapping("/method")
    public ResponseEntity<OrderResponse> updatePaymentMethod(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdatePaymentMethodRequest request) {
        return ResponseEntity.ok(paymentService.updatePaymentMethod(orderId, request));
    }
}