package com.restaurantpos.pos_backend.dto;

import com.restaurantpos.pos_backend.entity.OrderType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {

    @NotNull(message = "orderType must not be null")
    private OrderType orderType; // DINE_IN or TAKEAWAY

    // Required when orderType = DINE_IN, ignored otherwise
    private Double tableNumber;

    // Required when orderType = TAKEAWAY, ignored otherwise
    private String customerName;
}