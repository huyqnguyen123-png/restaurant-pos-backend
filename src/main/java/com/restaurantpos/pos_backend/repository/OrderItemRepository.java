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

    // Bulk-reassigns whole order item rows to a brand-new order and links them to a
    // payment, all in a single UPDATE statement. We bypass the in-memory parent/child
    // collection here: Order.items uses orphanRemoval=true, and directly manipulating
    // that collection to "move" a child to a different parent is a known Hibernate
    // pitfall that can trigger an unintended DELETE instead of the UPDATE we want.
    // clearAutomatically=true wipes the persistence context afterward so subsequent
    // reads in the same transaction see the fresh, moved state.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE OrderItem oi SET oi.order = :newOrder, oi.payment = :payment WHERE oi.id IN :itemIds")
    void reassignItemsToNewOrder(
            @Param("newOrder") Order newOrder,
            @Param("payment") Payment payment,
            @Param("itemIds") List<Long> itemIds);

    // Shrinks the quantity left behind on the original row after a partial-quantity
    // split payment (e.g. quantity 3, pay for 1 -> this reduces it down to 2).
    @Modifying(clearAutomatically = true)
    @Query("UPDATE OrderItem oi SET oi.quantity = oi.quantity - :qty WHERE oi.id = :itemId")
    void reduceQuantity(@Param("itemId") Long itemId, @Param("qty") int qty);
}