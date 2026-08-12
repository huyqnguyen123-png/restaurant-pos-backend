package com.restaurantpos.pos_backend.dto;

import com.restaurantpos.pos_backend.entity.OrderStatus;
import com.restaurantpos.pos_backend.entity.OrderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private OrderType orderType;
    private Double tableNumber;
    private String customerName;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
    private BigDecimal totalAmount;
    private List<OrderLineResponse> timeline;
}