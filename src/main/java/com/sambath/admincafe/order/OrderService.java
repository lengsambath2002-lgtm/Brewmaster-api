package com.sambath.admincafe.order;

import com.sambath.admincafe.common.NotFoundException;
import com.sambath.admincafe.order.dto.OrderItemResponse;
import com.sambath.admincafe.order.dto.OrderResponse;
import com.sambath.admincafe.order.dto.PlaceOrderItem;
import com.sambath.admincafe.order.dto.PlaceOrderRequest;
import com.sambath.admincafe.order.dto.UpdateStatusResponse;
import com.sambath.admincafe.transaction.TransactionService;
import com.sambath.admincafe.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final OrderRepository orderRepository;
    private final TransactionService transactionService;

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    public OrderResponse place(PlaceOrderRequest request) {
        Order order = new Order();
        order.setTableNumber(request.tableNumber());
        order.setCustomerName(request.customerName());
        order.setTakeout(request.isTakeout());
        order.setKitchenNote(request.kitchenNote());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (PlaceOrderItem item : request.items()) {
            OrderItem oi = new OrderItem();
            oi.setProductName(item.productName());
            oi.setQuantity(item.quantity());
            oi.setSize(item.size());
            oi.setNotes(item.notes() == null ? List.of() : item.notes());
            oi.setPriceOrder(item.priceOrder());
            order.addItem(oi);
            subtotal = subtotal.add(item.priceOrder());
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
        order.setSubtotal(subtotal);
        order.setTax(tax);
        order.setTotal(total);

        return toResponse(orderRepository.save(order));
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
        return new UpdateStatusResponse(toResponse(saved), transaction);
    }

    private OrderResponse toResponse(Order o) {
        List<OrderItemResponse> items = o.getItems().stream()
                .map(this::toItemResponse)
                .toList();
        return new OrderResponse(
                String.valueOf(o.getId()),
                o.getTableNumber(),
                o.isTakeout(),
                o.getCustomerName(),
                computeTimeElapsed(o),
                TIMESTAMP_FORMAT.format(o.getCreatedAt()),
                o.getStatus().toDisplay(),
                o.getServer(),
                items,
                o.getSubtotal(),
                o.getTax(),
                o.getTotal(),
                o.getKitchenNote()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem oi) {
        return new OrderItemResponse(
                "oi_" + oi.getId(),
                oi.getProductName(),
                oi.getQuantity(),
                oi.getSize(),
                oi.getNotes(),
                oi.getPriceOrder()
        );
    }

    private String computeTimeElapsed(Order o) {
        if (o.getStatus() == OrderStatus.COMPLETED) {
            return "Completed " + relative(o.getStatusUpdatedAt());
        }
        long secs = Duration.between(o.getCreatedAt(), Instant.now()).getSeconds();
        if (secs < 30) {
            return "Just Placed";
        }
        return relative(o.getCreatedAt());
    }

    private String relative(Instant t) {
        long secs = Duration.between(t, Instant.now()).getSeconds();
        if (secs < 60) return "just now";
        long mins = secs / 60;
        if (mins < 60) return mins + " min ago";
        long hrs = mins / 60;
        if (hrs < 24) return hrs + " hr ago";
        long days = hrs / 24;
        return days + " day ago";
    }
}
