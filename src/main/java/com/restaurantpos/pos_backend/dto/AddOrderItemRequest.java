package com.restaurantpos.pos_backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

// Exactly one of the two "modes" below must be provided:
// (a) menuItemId  -> add an existing menu item
// (b) customName + customPrice -> add a one-off item not saved to the permanent menu
// The service layer enforces this XOR rule; Bean Validation alone can't express it cleanly.
@Data
public class AddOrderItemRequest {

    private Long menuItemId;

    private String customName;

    @DecimalMin(value = "0.0", inclusive = false, message = "customPrice must be greater than 0")
    private BigDecimal customPrice;

    @Min(value = 1, message = "quantity must be at least 1")
    private int quantity = 1;
}