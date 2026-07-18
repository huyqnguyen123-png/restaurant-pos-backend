package com.restaurantpos.pos_backend.repository;

import com.restaurantpos.pos_backend.entity.Payment;
import com.restaurantpos.pos_backend.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderId(Long orderId);
    List<Payment> findByMethod(PaymentMethod method);

    // Used when deleting an entire order: must run AFTER deleting the order's
    // items (which may reference these payment rows), so no foreign key from
    // order_items still points at a payment we're about to remove.
    void deleteByOrderId(Long orderId);
}