package com.restaurantpos.pos_backend.repository;

import com.restaurantpos.pos_backend.entity.Order;
import com.restaurantpos.pos_backend.entity.OrderStatus;
import com.restaurantpos.pos_backend.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(OrderStatus status);

    List<Order> findAllByOrderByCreatedAtDesc();

    // Orders that have at least one item paid with the given method.
    // Joins Order -> items -> payment since Payment has no direct back-reference to Order's item list.
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i JOIN i.payment p " +
           "WHERE p.method = :method ORDER BY o.createdAt DESC")
    List<Order> findByPaymentMethod(@Param("method") PaymentMethod method);

    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i JOIN i.payment p " +
           "WHERE o.status = :status AND p.method = :method ORDER BY o.createdAt DESC")
    List<Order> findByStatusAndPaymentMethod(
            @Param("status") OrderStatus status,
            @Param("method") PaymentMethod method);
}