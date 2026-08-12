package com.restaurantpos.pos_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// A single row in the order's visual timeline — either a real order item,
// or a divider marking a split between people. Only one of `item` /
// `dividerId` is populated, matching `type`.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderLineResponse {
    private String type; // "ITEM" or "DIVIDER"
    private OrderItemResponse item;
    private Long dividerId;
}