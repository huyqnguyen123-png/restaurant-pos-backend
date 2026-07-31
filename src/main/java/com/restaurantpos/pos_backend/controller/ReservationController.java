package com.restaurantpos.pos_backend.controller;

import com.restaurantpos.pos_backend.dto.CreateReservationRequest;
import com.restaurantpos.pos_backend.dto.ReservationResponse;
import com.restaurantpos.pos_backend.dto.UpdateReservationStatusRequest;
import com.restaurantpos.pos_backend.entity.ReservationStatus;
import com.restaurantpos.pos_backend.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    // Public — called by the website backend when a customer books a table.
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse created = reservationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Called by the POS app's Reservations tab.
    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getAll(
            @RequestParam(required = false) ReservationStatus status) {
        return ResponseEntity.ok(reservationService.getAll(status));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ReservationResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateReservationStatusRequest request) {
        return ResponseEntity.ok(reservationService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}