package com.restaurantpos.pos_backend.service;

import com.restaurantpos.pos_backend.dto.*;
import com.restaurantpos.pos_backend.entity.*;
import com.restaurantpos.pos_backend.exception.BadRequestException;
import com.restaurantpos.pos_backend.exception.ResourceNotFoundException;
import com.restaurantpos.pos_backend.repository.MenuItemRepository;
import com.restaurantpos.pos_backend.repository.OrderDividerRepository;
import com.restaurantpos.pos_backend.repository.OrderItemRepository;
import com.restaurantpos.pos_backend.repository.OrderRepository;
import com.restaurantpos.pos_backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderDividerRepository orderDividerRepository;
    private final PaymentRepository paymentRepository;
    private final PushNotificationService pushNotificationService;

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

        if (saved.getOrderType() == OrderType.TAKEAWAY) {
            try {
                pushNotificationService.sendToAllDevices(
                        "New Pickup Order",
                        "Order #" + saved.getId() + " from " + saved.getCustomerName()
                );
            } catch (Exception e) {
                log.warn("Failed to send push notification for order {}: {}", saved.getId(), e.getMessage());
            }
        }

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

    // Always creates a brand-new line, never merges into an existing one —
    // even if the same menu item was just added. This lets staff add a
    // divider between identical items ordered by different people, and
    // keeps each person's order visually distinct. Duplicate lines are
    // merged back together only when printing the receipt (ReceiptModal),
    // never in the working order itself.
    private void addExistingMenuItem(Order order, AddOrderItemRequest request) {
        MenuItem menuItem = menuItemRepository.findById(request.getMenuItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Menu item not found with id: " + request.getMenuItemId()));

        OrderItem newItem = new OrderItem();
        newItem.setOrder(order);
        newItem.setMenuItem(menuItem);
        newItem.setItemName(menuItem.getName());
        newItem.setUnitPrice(menuItem.getPrice());
        newItem.setQuantity(request.getQuantity());
        newItem.setNotes(normalizeNotes(request.getNotes()));
        newItem.setPosition(nextPosition(order));
        order.getItems().add(newItem);
        orderItemRepository.save(newItem);
    }

    private void addCustomItem(Order order, AddOrderItemRequest request) {
        OrderItem newItem = new OrderItem();
        newItem.setOrder(order);
        newItem.setMenuItem(null);
        newItem.setItemName(request.getCustomName().trim());
        newItem.setUnitPrice(request.getCustomPrice());
        newItem.setQuantity(request.getQuantity());
        newItem.setNotes(normalizeNotes(request.getNotes()));
        newItem.setPosition(nextPosition(order));
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

    public OrderResponse updateItemNotes(Long orderId, Long orderItemId, String notes) {
        Order order = findOrderById(orderId);
        ensureOrderIsOpen(order);

        OrderItem item = findOrderItemInOrder(order, orderItemId);
        if (item.getPayment() != null) {
            throw new BadRequestException("Cannot change notes of an item that has already been paid");
        }
        item.setNotes(normalizeNotes(notes));
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

    // Appends a divider at the end of the current timeline — matches the
    // natural workflow of "person 1 finishes ordering, tap divider, person
    // 2 starts ordering" since new items are always appended at the end too.
    public OrderResponse addDivider(Long orderId) {
        Order order = findOrderById(orderId);
        ensureOrderIsOpen(order);

        OrderDivider divider = new OrderDivider();
        divider.setOrder(order);
        divider.setPosition(nextPosition(order));
        order.getDividers().add(divider);
        orderDividerRepository.save(divider);

        return toOrderResponse(order);
    }

    public OrderResponse removeDivider(Long orderId, Long dividerId) {
        Order order = findOrderById(orderId);
        ensureOrderIsOpen(order);

        OrderDivider divider = order.getDividers().stream()
                .filter(d -> d.getId().equals(dividerId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Divider not found with id: " + dividerId + " in order " + order.getId()));

        order.getDividers().remove(divider);
        orderDividerRepository.delete(divider);

        return toOrderResponse(order);
    }

    public void deleteOrder(Long orderId) {
        Order order = findOrderById(orderId);
        orderDividerRepository.deleteByOrderId(order.getId());
        orderItemRepository.deleteByOrderId(order.getId());
        paymentRepository.deleteByOrderId(order.getId());
        orderRepository.delete(order);
    }

    // --- helpers ---

    private String normalizeNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        return notes.trim();
    }

    // Next slot in the shared item/divider position sequence for this
    // order — always one past whatever is currently the highest position,
    // so new items and dividers are always appended at the very end.
    private int nextPosition(Order order) {
        int maxItemPos = order.getItems().stream()
                .map(OrderItem::getPosition)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(-1);
        int maxDividerPos = order.getDividers().stream()
                .mapToInt(OrderDivider::getPosition)
                .max()
                .orElse(-1);
        return Math.max(maxItemPos, maxDividerPos) + 1;
    }

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
                isCustom,
                item.getNotes()
        );
    }

    // Merges items and dividers into a single ordered list for display.
    // Items created before this feature shipped have no position yet — they
    // fall back to a very low sort key so they keep appearing in their
    // original (id) order, ahead of anything added after the update.
    private List<OrderLineResponse> buildTimeline(Order order) {
        record Entry(int sortKey, long tieBreakId, OrderLineResponse response) {
        }

        List<Entry> entries = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            int sortKey = item.getPosition() != null ? item.getPosition() : -1_000_000;
            entries.add(new Entry(sortKey, item.getId(),
                    new OrderLineResponse("ITEM", toItemResponse(item), null)));
        }

        for (OrderDivider divider : order.getDividers()) {
            entries.add(new Entry(divider.getPosition(), divider.getId(),
                    new OrderLineResponse("DIVIDER", null, divider.getId())));
        }

        return entries.stream()
                .sorted(Comparator.comparingInt(Entry::sortKey).thenComparingLong(Entry::tieBreakId))
                .map(Entry::response)
                .toList();
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
                calculateTotal(order),
                buildTimeline(order)
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