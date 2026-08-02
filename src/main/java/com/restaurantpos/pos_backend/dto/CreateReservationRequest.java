package com.restaurantpos.pos_backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreateReservationRequest {

    @NotBlank(message = "customerName must not be empty")
    private String customerName;

    private String email;

    @NotBlank(message = "phone must not be empty")
    private String phone;

    @NotNull(message = "reservationDate must not be null")
    private LocalDate reservationDate;

    @NotNull(message = "reservationTime must not be null")
    private LocalTime reservationTime;

    @Min(value = 1, message = "partySize must be at least 1")
    private int partySize;

    private String notes;
}