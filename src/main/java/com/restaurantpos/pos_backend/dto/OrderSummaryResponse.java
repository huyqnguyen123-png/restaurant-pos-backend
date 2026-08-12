package com.restaurantpos.pos_backend.dto;

import com.restaurantpos.pos_backend.entity.OrderStatus;
import com.restaurantpos.pos_backend.entity.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Lightweight view used for the home screen order list —
// just enough info to render each "order card"
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryResponse {
    private Long id;
    private OrderType orderType;
    private Double tableNumber;
    private String customerName;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}