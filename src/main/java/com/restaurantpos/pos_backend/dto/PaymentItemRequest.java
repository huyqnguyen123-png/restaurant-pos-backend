package com.restaurantpos.pos_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// One line within a split-bill payment request: which order item, and how much of its
// quantity this payment covers (may be less than the item's full current quantity).
@Data
public class PaymentItemRequest {

    @NotNull(message = "orderItemId must not be null")
    private Long orderItemId;

    @Min(value = 1, message = "quantity must be at least 1")
    private int quantity;
}