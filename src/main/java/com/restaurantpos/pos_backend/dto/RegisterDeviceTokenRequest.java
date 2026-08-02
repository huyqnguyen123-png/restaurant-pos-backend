package com.restaurantpos.pos_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterDeviceTokenRequest {

    @NotBlank(message = "token must not be empty")
    private String token;
}