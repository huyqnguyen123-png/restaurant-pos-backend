package com.restaurantpos.pos_backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

// Generic key-value store for small pieces of app-wide shared configuration
// (currently just the History password hash) — avoids a dedicated table for
// every single setting.
@Entity
@Table(name = "app_settings")
@Data
@NoArgsConstructor
public class AppSetting {

    @Id
    private String key;

    @jakarta.persistence.Column(length = 500)
    private String value;
}