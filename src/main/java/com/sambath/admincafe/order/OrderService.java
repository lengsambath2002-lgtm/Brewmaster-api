package com.sambath.admincafe.order;

import com.sambath.admincafe.common.ConflictException;
import com.sambath.admincafe.common.NotFoundException;
import com.sambath.admincafe.order.dto.OrderResponse;
import com.sambath.admincafe.order.dto.PlaceOrderItem;
import com.sambath.admincafe.order.dto.PlaceOrderRequest;
import com.sambath.admincafe.order.dto.UpdateOrderRequest;
import com.sambath.admincafe.order.dto.UpdateStatusResponse;
import com.sambath.admincafe.transaction.TransactionService;
import com.sambath.admincafe.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");
    private static final ZoneId ORDER_ZONE = ZoneId.of("Asia/Phnom_Penh");

    private final OrderRepository orderRepository;
    private final TransactionService transactionService;
    private final OrderMapper orderMapper;

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(orderMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAllGuest() {
        return orderRepository.findAllByGuestOrderByCreatedAtDesc(true).stream()
                .map(orderMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAllPaid() {
        return orderRepository.findAllByPaymentStatusOrderByCreatedAtDesc(PaymentStatus.PAID).stream()
                .map(orderMapper::toResponse).toList();
    }

    public OrderResponse place(PlaceOrderRequest request) {
        return placeInternal(request, false);
    }

    public OrderResponse placeAsGuest(PlaceOrderRequest request) {
        return placeInternal(request, true);
    }

    private OrderResponse placeInternal(PlaceOrderRequest request, boolean guest) {
        Order order = new Order();
        LocalDate today = LocalDate.now(ORDER_ZONE);
        order.setOrderDate(today);
        order.setDailyNumber(orderRepository.findMaxDailyNumberByOrderDate(today) + 1);
        order.setTableNumber(request.tableNumber());
        order.setCustomerName(request.customerName());
        order.setTakeout(request.isTakeout());
        order.setKitchenNote(request.kitchenNote());
        order.setGuest(guest);

        for (PlaceOrderItem item : request.items()) {
            OrderItem oi = new OrderItem();
            oi.setProductName(item.productName());
            oi.setQuantity(item.quantity());
            oi.setSize(item.size());
            oi.setNotes(item.notes() == null ? List.of() : item.notes());
            oi.setPriceOrder(item.priceOrder());
            order.addItem(oi);
        }

        recalcTotals(order);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    public OrderResponse update(Long id, UpdateOrderRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.PICKED_UP) {
            throw new ConflictException("Order is " + order.getStatus().toDisplay() + " and cannot be edited.");
        }

        if (request.tableNumber() != null) order.setTableNumber(request.tableNumber());
        if (request.customerName() != null) order.setCustomerName(request.customerName());
        if (request.isTakeout() != null) order.setTakeout(request.isTakeout());
        if (request.kitchenNote() != null) order.setKitchenNote(request.kitchenNote());

        if (request.items() != null) {
            if (request.items().isEmpty()) {
                throw new IllegalArgumentException("items must not be empty");
            }
            order.getItems().clear();
            for (PlaceOrderItem item : request.items()) {
                OrderItem oi = new OrderItem();
                oi.setProductName(item.productName());
                oi.setQuantity(item.quantity());
                oi.setSize(item.size());
                oi.setNotes(item.notes() == null ? List.of() : item.notes());
                oi.setPriceOrder(item.priceOrder());
                order.addItem(oi);
            }
        }

        recalcTotals(order);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    public void delete(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new NotFoundException("Order not found: " + id);
        }
        orderRepository.deleteById(id);
    }

    public UpdateStatusResponse updateStatus(Long id, String statusDisplay) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
        OrderStatus newStatus = OrderStatus.fromDisplay(statusDisplay);
        order.setStatus(newStatus);
        order.setStatusUpdatedAt(Instant.now());
        Order saved = orderRepository.save(order);

        TransactionResponse transaction = null;
        if (newStatus == OrderStatus.COMPLETED) {
            transaction = transactionService.createFromOrder(saved);
        }
        return new UpdateStatusResponse(orderMapper.toResponse(saved), transaction);
    }

    private void recalcTotals(Order order) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem oi : order.getItems()) {
            subtotal = subtotal.add(oi.getPriceOrder());
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
        order.setSubtotal(subtotal);
        order.setTax(tax);
        order.setTotal(total);
    }
}
