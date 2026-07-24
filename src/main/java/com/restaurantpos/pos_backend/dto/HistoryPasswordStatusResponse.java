package com.restaurantpos.pos_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoryPasswordStatusResponse {
    @JsonProperty("isSet")
    private boolean isSet;
}