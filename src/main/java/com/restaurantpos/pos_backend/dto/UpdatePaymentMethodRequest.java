package com.restaurantpos.pos_backend.dto;

import com.restaurantpos.pos_backend.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePaymentMethodRequest {
    @NotNull(message = "method must not be null")
    private PaymentMethod method;
}