package com.restaurantpos.pos_backend.controller;

import com.restaurantpos.pos_backend.dto.RegisterDeviceTokenRequest;
import com.restaurantpos.pos_backend.entity.DeviceToken;
import com.restaurantpos.pos_backend.repository.DeviceTokenRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/device-tokens")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenRepository deviceTokenRepository;

    @PostMapping
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterDeviceTokenRequest request) {
        deviceTokenRepository.findByToken(request.getToken())
                .orElseGet(() -> {
                    DeviceToken newToken = new DeviceToken();
                    newToken.setToken(request.getToken());
                    return deviceTokenRepository.save(newToken);
                });
        return ResponseEntity.ok().build();
    }
}