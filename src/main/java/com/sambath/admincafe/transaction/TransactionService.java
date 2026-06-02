package com.sambath.admincafe.transaction;

import com.sambath.admincafe.order.Order;
import com.sambath.admincafe.order.OrderItem;
import com.sambath.admincafe.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<TransactionResponse> findAll() {
        return transactionRepository.findAll().stream().map(this::toResponse).toList();
    }

    public TransactionResponse createFromOrder(Order order) {
        Transaction tx = new Transaction();
        tx.setOrderId(order.getId());
        tx.setCustomerName(resolveCustomerName(order));
        tx.setDescription(buildDescription(order.getItems()));
        tx.setItemsCount(order.getItems().stream().mapToInt(OrderItem::getQuantity).sum());
        tx.setAmount(order.getTotal());
        tx.setStatus(TransactionStatus.COMPLETED);
        return toResponse(transactionRepository.save(tx));
    }

    private static String resolveCustomerName(Order order) {
        if (isNotBlank(order.getTableNumber())) return order.getTableNumber();
        if (isNotBlank(order.getCustomerName())) return order.getCustomerName();
        return "Walk-in";
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    public TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                "BW-" + t.getId(),
                String.valueOf(t.getOrderId()),
                t.getCustomerName(),
                t.getDescription(),
                relative(t.getCreatedAt()),
                t.getItemsCount(),
                t.getAmount(),
                t.getStatus().name()
        );
    }

    private String buildDescription(List<OrderItem> items) {
        if (items.isEmpty()) {
            return "";
        }
        OrderItem first = items.get(0);
        String base = first.getQuantity() + "x " + first.getProductName();
        if (items.size() > 1) {
            base += " +" + (items.size() - 1) + " more";
        }
        return base;
    }

    private String relative(Instant t) {
        long secs = Duration.between(t, Instant.now()).getSeconds();
        if (secs < 60) return "Just now";
        long mins = secs / 60;
        if (mins < 60) return mins + " min ago";
        long hrs = mins / 60;
        if (hrs < 24) return hrs + " hr ago";
        long days = hrs / 24;
        return days + " day ago";
    }
}
