package com.sambath.admincafe.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByGuestOrderByCreatedAtDesc(boolean guest);

    List<Order> findAllByOrderByCreatedAtDesc();

    List<Order> findAllByPaymentStatusOrderByCreatedAtDesc(PaymentStatus paymentStatus);


    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :start AND o.createdAt < :end")
    long countBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COALESCE(MAX(o.dailyNumber), 0) FROM Order o WHERE o.orderDate = :date")
    int findMaxDailyNumberByOrderDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN :statuses")
    long countByStatusIn(@Param("statuses") List<OrderStatus> statuses);

    @Query("""
            SELECT COUNT(DISTINCT o.customerName)
            FROM Order o
            WHERE o.customerName IS NOT NULL
              AND o.createdAt >= :start AND o.createdAt < :end
              AND o.customerName NOT IN (
                SELECT DISTINCT o2.customerName FROM Order o2
                WHERE o2.customerName IS NOT NULL AND o2.createdAt < :start
              )
            """)
    long countNewCustomersBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            SELECT o FROM Order o
            WHERE o.status = com.sambath.admincafe.order.OrderStatus.COMPLETED
              AND o.statusUpdatedAt >= :start AND o.statusUpdatedAt < :end
            """)
    List<Order> findCompletedBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query(value = """
            SELECT oi.product_name AS product_name,
                   COALESCE(p.category, 'Uncategorized') AS category,
                   SUM(oi.quantity) AS units_sold,
                   SUM(oi.price_order) AS revenue
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            LEFT JOIN products p ON p.name = oi.product_name AND p.tenant_id = :tenantId
            WHERE o.tenant_id = :tenantId
              AND o.created_at >= :start AND o.created_at < :end
            GROUP BY oi.product_name, p.category
            ORDER BY SUM(oi.quantity) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopProducts(@Param("tenantId") Long tenantId,
                                   @Param("start") Instant start,
                                   @Param("end") Instant end,
                                   @Param("limit") int limit);

    @Query(value = """
            SELECT COALESCE(p.category, 'Uncategorized') AS category,
                   SUM(oi.quantity) AS units_sold,
                   SUM(oi.price_order) AS revenue
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            LEFT JOIN products p ON p.name = oi.product_name AND p.tenant_id = :tenantId
            WHERE o.tenant_id = :tenantId
              AND o.created_at >= :start AND o.created_at < :end
            GROUP BY p.category
            ORDER BY SUM(oi.price_order) DESC
            """, nativeQuery = true)
    List<Object[]> findCategoryAggregates(@Param("tenantId") Long tenantId,
                                          @Param("start") Instant start,
                                          @Param("end") Instant end);

    @Query(value = """
            SELECT p.id AS product_id,
                   oi.product_name AS product_name,
                   COALESCE(p.category, '-') AS category,
                   SUM(oi.quantity) AS units_sold,
                   SUM(oi.price_order) AS revenue,
                   CASE WHEN SUM(oi.quantity) > 0
                        THEN SUM(oi.price_order) / SUM(oi.quantity)
                        ELSE 0 END AS unit_price
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.id
            LEFT JOIN products p ON p.name = oi.product_name AND p.tenant_id = :tenantId
            WHERE o.tenant_id = :tenantId
              AND o.created_at >= :start AND o.created_at < :end
              AND (:salesPerson IS NULL OR o.server = :salesPerson)
            GROUP BY p.id, oi.product_name, p.category
            ORDER BY SUM(oi.price_order) DESC
            """, nativeQuery = true)
    List<Object[]> findSalesLineItems(@Param("tenantId") Long tenantId,
                                      @Param("start") Instant start,
                                      @Param("end") Instant end,
                                      @Param("salesPerson") String salesPerson);
}
