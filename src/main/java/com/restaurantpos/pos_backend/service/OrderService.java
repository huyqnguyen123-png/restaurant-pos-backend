package com.restaurantpos.pos_backend.service;

import com.restaurantpos.pos_backend.dto.*;
import com.restaurantpos.pos_backend.entity.*;
import com.restaurantpos.pos_backend.exception.BadRequestException;
import com.restaurantpos.pos_backend.exception.ResourceNotFoundException;
import com.restaurantpos.pos_backend.repository.MenuItemRepository;
import com.restaurantpos.pos_backend.repository.OrderItemRepository;
import com.restaurantpos.pos_backend.repository.OrderRepository;
import com.restaurantpos.pos_backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;

    public OrderResponse createOrder(CreateOrderRequest request) {
        validateOrderRequest(request);

        Order order = new Order();
        order.setOrderType(request.getOrderType());
        order.setStatus(OrderStatus.OPEN);

        if (request.getOrderType() == OrderType.DINE_IN) {
            order.setTableNumber(request.getTableNumber());
        } else {
            order.setCustomerName(request.getCustomerName());
        }

        Order saved = orderRepository.save(order);
        return toOrderResponse(saved);
    }

    public List<OrderSummaryResponse> getOrderSummaries(OrderStatus status, PaymentMethod paymentMethod) {
        List<Order> orders;

        if (status != null && paymentMethod != null) {
            orders = orderRepository.findByStatusAndPaymentMethod(status, paymentMethod);
        } else if (status != null) {
            orders = orderRepository.findByStatus(status);
        } else if (paymentMethod != null) {
            orders = orderRepository.findByPaymentMethod(paymentMethod);
        } else {
            orders = orderRepository.findAllByOrderByCreatedAtDesc();
        }

        return orders.stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    public OrderResponse getOrder(Long orderId) {
        Order order = findOrderById(orderId);
        return toOrderResponse(order);
    }

    // Renames a table number (dine-in) or customer name (takeaway). Allowed
    // regardless of order status (OPEN or PAID) - fixing a typo shouldn't be
    // blocked just because the bill was already settled.
    public OrderResponse updateOrderInfo(Long orderId, UpdateOrderInfoRequest request) {
        Order order = findOrderById(orderId);

        if (order.getOrderType() == OrderType.DINE_IN) {
            if (request.getTableNumber() == null || request.getTableNumber() <= 0) {
                throw new BadRequestException("tableNumber is required for dine-in orders");
            }
            order.setTableNumber(request.getTableNumber());
        } else {
            if (request.getCustomerName() == null || request.getCustomerName().isBlank()) {
                throw new BadRequestException("customerName is required for takeaway orders");
            }
            order.setCustomerName(request.getCustomerName().trim());
        }

        Order saved = orderRepository.save(order);
        return toOrderResponse(saved);
    }

    public OrderResponse addItem(Long orderId, AddOrderItemRequest request) {
        Order order = findOrderById(orderId);
        ensureOrderIsOpen(order);

        boolean hasMenuItemId = request.getMenuItemId() != null;
        boolean hasCustomFields = request.getCustomName() != null
                && !request.getCustomName().isBlank()
                && request.getCustomPrice() != null;

        if (hasMenuItemId == hasCustomFields) {
            throw new BadRequestException(
                    "Provide either menuItemId, or both customName and customPrice (not both, not neither)");
        }

        if (hasMenuItemId) {
            addExistingMenuItem(order, request);
        } else {
            addCustomItem(order, request);
        }

        return toOrderResponse(order);
    }

    private void addExistingMenuItem(Order order, AddOrderItemRequest request) {
        MenuItem menuItem = menuItemRepository.findById(request.getMenuItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Menu item not found with id: " + request.getMenuItemId()));

        OrderItem existing = order.getItems().stream()
                .filter(i -> i.getMenuItem() != null
                        && i.getMenuItem().getId().equals(menuItem.getId())
                        && i.getPayment() == null)
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
            orderItemRepository.save(existing);
        } else {
            OrderItem newItem = new OrderItem();
            newItem.setOrder(order);
            newItem.setMenuItem(menuItem);
            newItem.setItemName(menuItem.getName());
            newItem.setUnitPrice(menuItem.getPrice());
            newItem.setQuantity(request.getQuantity());
            order.getItems().add(newItem);
            orderItemRepository.save(newItem);
        }
    }

    private void addCustomItem(Order order, AddOrderItemRequest request) {
        OrderItem newItem = new OrderItem();
        newItem.setOrder(order);
        newItem.setMenuItem(null);
        newItem.setItemName(request.getCustomName().trim());
        newItem.setUnitPrice(request.getCustomPrice());
        newItem.setQuantity(request.getQuantity());
        order.getItems().add(newItem);
        orderItemRepository.save(newItem);
    }

    public OrderResponse updateItemQuantity(Long orderId, Long orderItemId, UpdateOrderItemRequest request) {
        Order order = findOrderById(orderId);
        ensureOrderIsOpen(order);

        OrderItem item = findOrderItemInOrder(order, orderItemId);
        if (item.getPayment() != null) {
            throw new BadRequestException("Cannot change quantity of an item that has already been paid");
        }
        item.setQuantity(request.getQuantity());
        orderItemRepository.save(item);

        return toOrderResponse(order);
    }

    public OrderResponse removeItem(Long orderId, Long orderItemId) {
        Order order = findOrderById(orderId);
        ensureOrderIsOpen(order);

        OrderItem item = findOrderItemInOrder(order, orderItemId);
        if (item.getPayment() != null) {
            throw new BadRequestException("Cannot remove an item that has already been paid");
        }
        order.getItems().remove(item);
        orderItemRepository.delete(item);

        return toOrderResponse(order);
    }

    // Deletes an entire order (works for both OPEN and PAID orders). Deletion
    // order matters: items first (they may reference a payment row), then
    // payments (now nothing references them), then the order itself.
    public void deleteOrder(Long orderId) {
        Order order = findOrderById(orderId);
        orderItemRepository.deleteByOrderId(order.getId());
        paymentRepository.deleteByOrderId(order.getId());
        orderRepository.delete(order);
    }

    // --- helpers ---

    private void validateOrderRequest(CreateOrderRequest request) {
        if (request.getOrderType() == OrderType.DINE_IN
                && (request.getTableNumber() == null || request.getTableNumber() <= 0)) {
            throw new BadRequestException("tableNumber is required for dine-in orders");
        }
        if (request.getOrderType() == OrderType.TAKEAWAY
                && (request.getCustomerName() == null || request.getCustomerName().isBlank())) {
            throw new BadRequestException("customerName is required for takeaway orders");
        }
    }

    private void ensureOrderIsOpen(Order order) {
        if (order.getStatus() != OrderStatus.OPEN) {
            throw new BadRequestException("Cannot modify an order that is already paid");
        }
    }

    private Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private OrderItem findOrderItemInOrder(Order order, Long orderItemId) {
        return order.getItems().stream()
                .filter(i -> i.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order item not found with id: " + orderItemId + " in order " + order.getId()));
    }

    private BigDecimal calculateTotal(Order order) {
        return order.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        PaymentMethod method = (item.getPayment() != null) ? item.getPayment().getMethod() : null;
        Long menuItemId = (item.getMenuItem() != null) ? item.getMenuItem().getId() : null;
        boolean isCustom = item.getMenuItem() == null;

        return new OrderItemResponse(
                item.getId(),
                menuItemId,
                item.getItemName(),
                item.getUnitPrice(),
                item.getQuantity(),
                subtotal,
                item.getPayment() != null,
                method,
                isCustom
        );
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderType(),
                order.getTableNumber(),
                order.getCustomerName(),
                order.getStatus(),
                order.getCreatedAt(),
                items,
                calculateTotal(order)
        );
    }

    private OrderSummaryResponse toSummaryResponse(Order order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderType(),
                order.getTableNumber(),
                order.getCustomerName(),
                order.getStatus(),
                calculateTotal(order),
                order.getCreatedAt()
        );
    }
}