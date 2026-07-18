package com.restaurantpos.pos_backend.dto;

import com.restaurantpos.pos_backend.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreatePaymentRequest {

    @NotNull(message = "method must not be null")
    private PaymentMethod method;

    // Optional: which items (and how much of each item's quantity) to pay for.
    // Used for split-bill payments, including partial quantities (e.g. paying for
    // 2 out of 3 units of the same item). If null or empty, this payment covers
    // everything currently unpaid in the order, at full quantity.
    @Valid
    private List<PaymentItemRequest> items;
}