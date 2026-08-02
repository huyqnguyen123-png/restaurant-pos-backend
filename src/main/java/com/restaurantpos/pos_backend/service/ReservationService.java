package com.restaurantpos.pos_backend.service;

import com.restaurantpos.pos_backend.dto.CreateReservationRequest;
import com.restaurantpos.pos_backend.dto.ReservationResponse;
import com.restaurantpos.pos_backend.dto.UpdateReservationStatusRequest;
import com.restaurantpos.pos_backend.entity.Reservation;
import com.restaurantpos.pos_backend.entity.ReservationStatus;
import com.restaurantpos.pos_backend.exception.ResourceNotFoundException;
import com.restaurantpos.pos_backend.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final PushNotificationService pushNotificationService;

    public ReservationResponse create(CreateReservationRequest request) {
        Reservation reservation = new Reservation();
        reservation.setCustomerName(request.getCustomerName());
        reservation.setEmail(request.getEmail());
        reservation.setPhone(request.getPhone());
        reservation.setReservationDate(request.getReservationDate());
        reservation.setReservationTime(request.getReservationTime());
        reservation.setPartySize(request.getPartySize());
        reservation.setNotes(request.getNotes());
        Reservation saved = reservationRepository.save(reservation);

        // Never let a notification failure roll back the reservation itself.
        try {
            pushNotificationService.sendToAllDevices(
                    "New Reservation",
                    saved.getCustomerName() + " — " + saved.getPartySize() + " guests on "
                            + saved.getReservationDate() + " at " + saved.getReservationTime()
            );
        } catch (Exception e) {
            log.warn("Failed to send push notification for reservation {}: {}", saved.getId(), e.getMessage());
        }

        return toResponse(saved);
    }

    public List<ReservationResponse> getAll(ReservationStatus status) {
        List<Reservation> reservations = (status != null)
                ? reservationRepository.findByStatus(status)
                : reservationRepository.findAllByOrderByReservationDateAscReservationTimeAsc();

        return reservations.stream().map(this::toResponse).toList();
    }

    public ReservationResponse updateStatus(Long id, UpdateReservationStatusRequest request) {
        Reservation reservation = findById(id);
        reservation.setStatus(request.getStatus());
        Reservation saved = reservationRepository.save(reservation);
        return toResponse(saved);
    }

    public void delete(Long id) {
        Reservation reservation = findById(id);
        reservationRepository.delete(reservation);
    }

    private Reservation findById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));
    }

    private ReservationResponse toResponse(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getCustomerName(),
                r.getEmail(),
                r.getPhone(),
                r.getReservationDate(),
                r.getReservationTime(),
                r.getPartySize(),
                r.getNotes(),
                r.getStatus(),
                r.getCreatedAt()
        );
    }
}