package com.restaurantpos.pos_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Stores the FCM (Firebase Cloud Messaging) token for each device that has
// the POS app installed, so the backend knows where to send push
// notifications when a new order or reservation arrives from the website.
@Entity
@Table(name = "device_tokens")
@Data
@NoArgsConstructor
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique — re-registering the same token (e.g. app restart) updates the
    // existing row instead of creating a duplicate.
    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}