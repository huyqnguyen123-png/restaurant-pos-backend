package com.restaurantpos.pos_backend.dto;

import lombok.Data;

// Only the field matching the order's existing orderType is used
// (tableNumber for DINE_IN, customerName for TAKEAWAY).
@Data
public class UpdateOrderInfoRequest {
    private Integer tableNumber;
    private String customerName;
}