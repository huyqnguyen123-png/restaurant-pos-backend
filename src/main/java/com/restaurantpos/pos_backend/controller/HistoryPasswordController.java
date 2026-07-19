package com.restaurantpos.pos_backend.controller;

import com.restaurantpos.pos_backend.dto.*;
import com.restaurantpos.pos_backend.service.HistoryPasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings/history-password")
@RequiredArgsConstructor
public class HistoryPasswordController {

    private final HistoryPasswordService historyPasswordService;

    @GetMapping
    public ResponseEntity<HistoryPasswordStatusResponse> getStatus() {
        return ResponseEntity.ok(new HistoryPasswordStatusResponse(historyPasswordService.isPasswordSet()));
    }

    @PostMapping("/verify")
    public ResponseEntity<VerifyPasswordResponse> verify(@Valid @RequestBody VerifyPasswordRequest request) {
        return ResponseEntity.ok(new VerifyPasswordResponse(historyPasswordService.verify(request.getPassword())));
    }

    @PutMapping
    public ResponseEntity<Void> setPassword(@Valid @RequestBody SetHistoryPasswordRequest request) {
        historyPasswordService.setPassword(request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> removePassword(@Valid @RequestBody RemovePasswordRequest request) {
        historyPasswordService.removePassword(request.getPassword());
        return ResponseEntity.noContent().build();
    }
}