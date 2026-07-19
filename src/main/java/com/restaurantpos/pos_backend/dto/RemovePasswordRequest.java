package com.restaurantpos.pos_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RemovePasswordRequest {
    @NotBlank(message = "password must not be empty")
    private String password;
}