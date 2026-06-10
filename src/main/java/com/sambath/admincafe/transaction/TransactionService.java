package com.sambath.admincafe.transaction;

import com.sambath.admincafe.common.ConflictException;
import com.sambath.admincafe.common.NotFoundException;
import com.sambath.admincafe.order.Order;
import com.sambath.admincafe.order.OrderItem;
import com.sambath.admincafe.order.OrderMapper;
import com.sambath.admincafe.order.OrderRepository;
import com.sambath.admincafe.order.PaymentStatus;
import com.sambath.admincafe.transaction.dto.RefundRequest;
import com.sambath.admincafe.transaction.dto.RefundResponse;
import com.sambath.admincafe.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Transactional(readOnly = true)
    public List<TransactionResponse> findAll() {
        return transactionRepository.findAll().stream().map(this::toResponse).toList();
    }

    public RefundResponse refund(String publicId, RefundRequest request) {
        Long txId = parsePublicId(publicId);
        Transaction original = transactionRepository.findById(txId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + publicId));
        if (original.getStatus() == TransactionStatus.REFUNDED) {
            throw new ConflictException("Transaction already refunded.");
        }

        BigDecimal amount = request == null || request.amount() == null
                ? original.getAmount()
                : request.amount();
        String reason = request == null ? null : request.reason();

        Transaction refund = new Transaction();
        refund.setOrderId(original.getOrderId());
        refund.setCustomerName(original.getCustomerName());
        refund.setDescription(reason == null || reason.isBlank()
                ? "Refund — " + original.getDescription()
                : "Refund — " + reason);
        refund.setItemsCount(original.getItemsCount());
        refund.setAmount(amount.negate());
        refund.setStatus(TransactionStatus.REFUNDED);
        Transaction savedRefund = transactionRepository.save(refund);

        original.setStatus(TransactionStatus.REFUNDED);
        transactionRepository.save(original);

        Order order = orderRepository.findById(original.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found: " + original.getOrderId()));
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            orderRepository.save(order);
        }

        return new RefundResponse(toResponse(savedRefund), orderMapper.toResponse(order));
    }

    private static Long parsePublicId(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            throw new IllegalArgumentException("transaction id is required");
        }
        String raw = publicId.startsWith("BW-") ? publicId.substring(3) : publicId;
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid transaction id: " + publicId);
        }
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

    public TransactionResponse refundForCancelledOrder(Order order) {
        BigDecimal amount = order.getTotal() == null ? BigDecimal.ZERO : order.getTotal().negate();
        Transaction refund = new Transaction();
        refund.setOrderId(order.getId());
        refund.setCustomerName(resolveCustomerName(order));
        refund.setDescription("Refund — order cancelled");
        refund.setItemsCount(order.getItems().stream().mapToInt(OrderItem::getQuantity).sum());
        refund.setAmount(amount);
        refund.setStatus(TransactionStatus.REFUNDED);
        return toResponse(transactionRepository.save(refund));
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
