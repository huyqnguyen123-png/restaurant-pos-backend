package com.restaurantpos.pos_backend.repository;

import com.restaurantpos.pos_backend.entity.Reservation;
import com.restaurantpos.pos_backend.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByStatus(ReservationStatus status);
    List<Reservation> findAllByOrderByReservationDateAscReservationTimeAsc();
}