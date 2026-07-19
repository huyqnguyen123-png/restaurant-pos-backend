package com.restaurantpos.pos_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyPasswordRequest {
    @NotBlank(message = "password must not be empty")
    private String password;
}