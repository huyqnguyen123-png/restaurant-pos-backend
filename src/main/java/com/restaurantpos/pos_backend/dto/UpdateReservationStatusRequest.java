package com.restaurantpos.pos_backend.dto;

import com.restaurantpos.pos_backend.entity.ReservationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateReservationStatusRequest {
    @NotNull(message = "status must not be null")
    private ReservationStatus status;
}