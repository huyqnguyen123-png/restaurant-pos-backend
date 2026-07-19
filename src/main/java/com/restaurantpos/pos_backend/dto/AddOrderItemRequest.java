package com.restaurantpos.pos_backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddOrderItemRequest {

    private Long menuItemId;

    private String customName;

    @DecimalMin(value = "0.0", inclusive = false, message = "customPrice must be greater than 0")
    private BigDecimal customPrice;

    @Min(value = 1, message = "quantity must be at least 1")
    private int quantity = 1;

    private String notes;
}