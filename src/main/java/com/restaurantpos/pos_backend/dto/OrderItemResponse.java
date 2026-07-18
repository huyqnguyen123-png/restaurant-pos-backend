package com.restaurantpos.pos_backend.dto;

import com.restaurantpos.pos_backend.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private Long id;
    private Long menuItemId; // null for custom, one-off items
    private String itemName;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal subtotal;
    private boolean paid;
    private PaymentMethod paymentMethod;
    private boolean custom; // true if typed in manually rather than picked from the menu
}