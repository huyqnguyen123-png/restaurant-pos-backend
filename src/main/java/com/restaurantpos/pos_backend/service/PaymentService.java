package com.restaurantpos.pos_backend.service;

import com.restaurantpos.pos_backend.dto.CreatePaymentRequest;
import com.restaurantpos.pos_backend.dto.OrderResponse;
import com.restaurantpos.pos_backend.dto.PaymentItemRequest;
import com.restaurantpos.pos_backend.dto.PaymentResponse;
import com.restaurantpos.pos_backend.dto.UpdatePaymentMethodRequest;
import com.restaurantpos.pos_backend.entity.Order;
import com.restaurantpos.pos_backend.entity.OrderItem;
import com.restaurantpos.pos_backend.entity.OrderStatus;
import com.restaurantpos.pos_backend.entity.Payment;
import com.restaurantpos.pos_backend.entity.PaymentMethod;
import com.restaurantpos.pos_backend.exception.BadRequestException;
import com.restaurantpos.pos_backend.exception.ResourceNotFoundException;
import com.restaurantpos.pos_backend.repository.OrderItemRepository;
import com.restaurantpos.pos_backend.repository.OrderRepository;
import com.restaurantpos.pos_backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;

    private record PaymentLine(OrderItem item, int quantity) {}

    private record Reduction(Long itemId, int quantity) {}

    public OrderResponse createPayment(Long orderId, CreatePaymentRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() == OrderStatus.PAID) {
            throw new BadRequestException("This order has already been fully paid");
        }

        List<PaymentLine> lines = buildPaymentLines(order, request.getItems());

        if (lines.isEmpty()) {
            throw new BadRequestException("No unpaid items available to pay for");
        }

        BigDecimal amount = lines.stream()
                .map(l -> l.item().getUnitPrice().multiply(BigDecimal.valueOf(l.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (coversAllUnpaidQuantity(order, lines)) {
            return payWithoutSplitting(order, lines, request.getMethod(), amount);
        }
        return splitPaymentIntoNewOrder(order, lines, request.getMethod(), amount);
    }

    // Lets staff correct a mistaken payment method (e.g. selected Cash instead
    // of Card) after the fact. Only meaningful for orders that have already
    // been paid - a partial/split payment always spins off into its own
    // separate order (see createPayment above), so a PAID order always has
    // exactly one payment to correct here.
    public OrderResponse updatePaymentMethod(Long orderId, UpdatePaymentMethodRequest request) {
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }

        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        if (payments.isEmpty()) {
            throw new BadRequestException("This order has not been paid yet, so there is no payment method to change");
        }

        Payment payment = payments.get(0);
        payment.setMethod(request.getMethod());
        paymentRepository.save(payment);

        return orderService.getOrder(orderId);
    }

    private OrderResponse payWithoutSplitting(Order order, List<PaymentLine> lines, PaymentMethod method, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(method);
        payment.setAmount(amount);
        Payment savedPayment = paymentRepository.save(payment);

        for (PaymentLine line : lines) {
            line.item().setPayment(savedPayment);
        }
        orderItemRepository.saveAll(lines.stream().map(PaymentLine::item).toList());

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        return orderService.getOrder(order.getId());
    }

    private OrderResponse splitPaymentIntoNewOrder(
            Order order, List<PaymentLine> lines, PaymentMethod method, BigDecimal amount) {

        Order splitOrder = new Order();
        splitOrder.setOrderType(order.getOrderType());
        splitOrder.setTableNumber(order.getTableNumber());
        splitOrder.setCustomerName(order.getCustomerName());
        splitOrder.setStatus(OrderStatus.PAID);
        Order savedSplitOrder = orderRepository.save(splitOrder);

        Payment payment = new Payment();
        payment.setOrder(savedSplitOrder);
        payment.setMethod(method);
        payment.setAmount(amount);
        Payment savedPayment = paymentRepository.save(payment);

        List<Long> fullyMovedItemIds = new ArrayList<>();
        List<OrderItem> newPartialCopies = new ArrayList<>();
        List<Reduction> partialReductions = new ArrayList<>();

        for (PaymentLine line : lines) {
            OrderItem item = line.item();
            int payingQty = line.quantity();

            if (payingQty == item.getQuantity()) {
                fullyMovedItemIds.add(item.getId());
            } else {
                OrderItem paidCopy = new OrderItem();
                paidCopy.setOrder(savedSplitOrder);
                paidCopy.setMenuItem(item.getMenuItem());
                paidCopy.setItemName(item.getItemName());
                paidCopy.setUnitPrice(item.getUnitPrice());
                paidCopy.setQuantity(payingQty);
                paidCopy.setPayment(savedPayment);
                newPartialCopies.add(paidCopy);

                partialReductions.add(new Reduction(item.getId(), payingQty));
            }
        }

        if (!fullyMovedItemIds.isEmpty()) {
            orderItemRepository.reassignItemsToNewOrder(savedSplitOrder, savedPayment, fullyMovedItemIds);
        }
        if (!newPartialCopies.isEmpty()) {
            orderItemRepository.saveAll(newPartialCopies);
        }
        for (Reduction r : partialReductions) {
            orderItemRepository.reduceQuantity(r.itemId(), r.quantity());
        }

        return orderService.getOrder(order.getId());
    }

    // --- helpers ---

    private boolean coversAllUnpaidQuantity(Order order, List<PaymentLine> lines) {
        Map<Long, Integer> requestedByItemId = new HashMap<>();
        for (PaymentLine line : lines) {
            requestedByItemId.put(line.item().getId(), line.quantity());
        }

        for (OrderItem item : order.getItems()) {
            if (item.getPayment() != null) {
                continue;
            }
            Integer requestedQty = requestedByItemId.get(item.getId());
            if (requestedQty == null || !requestedQty.equals(item.getQuantity())) {
                return false;
            }
        }
        return true;
    }

    private List<PaymentLine> buildPaymentLines(Order order, List<PaymentItemRequest> requestedItems) {
        if (requestedItems == null || requestedItems.isEmpty()) {
            return order.getItems().stream()
                    .filter(i -> i.getPayment() == null)
                    .map(i -> new PaymentLine(i, i.getQuantity()))
                    .toList();
        }

        Set<Long> seenItemIds = new HashSet<>();
        List<PaymentLine> lines = new ArrayList<>();

        for (PaymentItemRequest req : requestedItems) {
            if (!seenItemIds.add(req.getOrderItemId())) {
                throw new BadRequestException("Duplicate orderItemId in payment request: " + req.getOrderItemId());
            }

            OrderItem item = order.getItems().stream()
                    .filter(i -> i.getId().equals(req.getOrderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Order item not found with id: " + req.getOrderItemId() + " in order " + order.getId()));

            if (item.getPayment() != null) {
                throw new BadRequestException("Item '" + item.getItemName() + "' has already been paid");
            }
            if (req.getQuantity() < 1 || req.getQuantity() > item.getQuantity()) {
                throw new BadRequestException(
                        "Invalid quantity for '" + item.getItemName() + "': must be between 1 and " + item.getQuantity());
            }

            lines.add(new PaymentLine(item, req.getQuantity()));
        }

        return lines;
    }

    public List<PaymentResponse> getPaymentsForOrder(Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order not found with id: " + orderId);
        }
        return paymentRepository.findByOrderId(orderId).stream()
                .map(this::toPaymentResponse)
                .toList();
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        List<String> itemNames = orderItemRepository.findByOrderId(payment.getOrder().getId()).stream()
                .filter(i -> i.getPayment() != null && i.getPayment().getId().equals(payment.getId()))
                .map(OrderItem::getItemName)
                .toList();

        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getMethod(),
                payment.getAmount(),
                payment.getPaidAt(),
                itemNames
        );
    }
}