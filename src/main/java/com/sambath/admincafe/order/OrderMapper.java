package com.sambath.admincafe.order;

import com.sambath.admincafe.order.dto.OrderItemResponse;
import com.sambath.admincafe.order.dto.OrderResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class OrderMapper {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    public OrderResponse toResponse(Order o) {
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
