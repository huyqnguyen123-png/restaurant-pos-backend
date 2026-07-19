package com.restaurantpos.pos_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SetHistoryPasswordRequest {
    // Null/omitted only when there's no password set yet (first-time setup).
    private String oldPassword;

    @NotBlank(message = "newPassword must not be empty")
    private String newPassword;
}