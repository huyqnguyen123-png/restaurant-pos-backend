package com.restaurantpos.pos_backend.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateOrderItemRequest {

    @Min(value = 1, message = "quantity must be at least 1")
    private int quantity;
}