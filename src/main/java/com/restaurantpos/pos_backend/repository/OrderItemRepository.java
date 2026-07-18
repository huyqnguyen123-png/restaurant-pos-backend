package com.restaurantpos.pos_backend.repository;

import com.restaurantpos.pos_backend.entity.Order;
import com.restaurantpos.pos_backend.entity.OrderItem;
import com.restaurantpos.pos_backend.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE OrderItem oi SET oi.order = :newOrder, oi.payment = :payment WHERE oi.id IN :itemIds")
    void reassignItemsToNewOrder(
            @Param("newOrder") Order newOrder,
            @Param("payment") Payment payment,
            @Param("itemIds") List<Long> itemIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE OrderItem oi SET oi.quantity = oi.quantity - :qty WHERE oi.id = :itemId")
    void reduceQuantity(@Param("itemId") Long itemId, @Param("qty") int qty);

    // Used when deleting an entire order: removes all its line items first,
    // before the order's payments (which the items may reference) can be
    // safely removed.
    void deleteByOrderId(Long orderId);
}