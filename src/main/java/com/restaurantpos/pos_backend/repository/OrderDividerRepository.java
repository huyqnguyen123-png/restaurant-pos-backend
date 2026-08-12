package com.restaurantpos.pos_backend.repository;

import com.restaurantpos.pos_backend.entity.OrderDivider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderDividerRepository extends JpaRepository<OrderDivider, Long> {

    @Modifying
    @Query("DELETE FROM OrderDivider d WHERE d.order.id = :orderId")
    void deleteByOrderId(@Param("orderId") Long orderId);
}