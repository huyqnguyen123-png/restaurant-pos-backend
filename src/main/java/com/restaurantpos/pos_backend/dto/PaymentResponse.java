package com.restaurantpos.pos_backend.dto;

import com.restaurantpos.pos_backend.entity.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private PaymentMethod method;
    private BigDecimal amount;
    private LocalDateTime paidAt;
    private List<String> itemNames; // which items this specific payment covered (useful for split-bill display)
}