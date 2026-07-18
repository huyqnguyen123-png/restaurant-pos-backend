package com.restaurantpos.pos_backend.controller;

import com.restaurantpos.pos_backend.dto.*;
import com.restaurantpos.pos_backend.entity.OrderStatus;
import com.restaurantpos.pos_backend.entity.PaymentMethod;
import com.restaurantpos.pos_backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<OrderSummaryResponse>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod) {
        return ResponseEntity.ok(orderService.getOrderSummaries(status, paymentMethod));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    // PUT /api/orders/{id} -> rename the table number or customer name
    @PutMapping("/{id}")
    public ResponseEntity<OrderResponse> updateOrderInfo(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderInfoRequest request) {
        return ResponseEntity.ok(orderService.updateOrderInfo(id, request));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<OrderResponse> addItem(
            @PathVariable Long id,
            @Valid @RequestBody AddOrderItemRequest request) {
        return ResponseEntity.ok(orderService.addItem(id, request));
    }

    @PutMapping("/{id}/items/{itemId}")
    public ResponseEntity<OrderResponse> updateItemQuantity(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateOrderItemRequest request) {
        return ResponseEntity.ok(orderService.updateItemQuantity(id, itemId, request));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<OrderResponse> removeItem(
            @PathVariable Long id,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(orderService.removeItem(id, itemId));
    }

    // DELETE /api/orders/{id} -> permanently delete an entire order (any status)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}