package com.restaurantpos.pos_backend.repository;

import com.restaurantpos.pos_backend.entity.Payment;
import com.restaurantpos.pos_backend.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderId(Long orderId);
    List<Payment> findByMethod(PaymentMethod method); // used for filtering payment history by CASH/CARD
}