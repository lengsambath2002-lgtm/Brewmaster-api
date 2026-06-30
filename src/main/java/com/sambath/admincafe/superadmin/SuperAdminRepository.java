package com.sambath.admincafe.superadmin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cross-tenant aggregates. All queries are native so they bypass the
 * Hibernate @TenantId filter that's active on the request thread.
 */
@Repository
@RequiredArgsConstructor
public class SuperAdminRepository {

    @PersistenceContext
    private EntityManager em;

    record TenantCounts(long users, long products, long orders) {}

    record TenantRevenue(long orders, BigDecimal revenue) {}

    public Map<Long, TenantCounts> countsByTenant(List<Long> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return Map.of();
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT t.id,
                       (SELECT COUNT(*) FROM users u WHERE u.tenant_id = t.id),
                       (SELECT COUNT(*) FROM products p WHERE p.tenant_id = t.id),
                       (SELECT COUNT(*) FROM orders o WHERE o.tenant_id = t.id)
                FROM tenants t
                WHERE t.id IN (:ids)
                """)
                .setParameter("ids", tenantIds)
                .getResultList();
        return rows.stream().collect(Collectors.toMap(
                r -> ((Number) r[0]).longValue(),
                r -> new TenantCounts(
                        ((Number) r[1]).longValue(),
                        ((Number) r[2]).longValue(),
                        ((Number) r[3]).longValue())));
    }

    public Map<Long, TenantRevenue> revenueByTenant(List<Long> tenantIds) {
        if (tenantIds == null || tenantIds.isEmpty()) {
            return Map.of();
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery("""
                SELECT t.id,
                       COUNT(DISTINCT tx.order_id),
                       COALESCE(SUM(tx.amount), 0)
                FROM tenants t
                LEFT JOIN transactions tx
                       ON tx.tenant_id = t.id AND tx.status = 'COMPLETED'
                WHERE t.id IN (:ids)
                GROUP BY t.id
                """)
                .setParameter("ids", tenantIds)
                .getResultList();
        return rows.stream().collect(Collectors.toMap(
                r -> ((Number) r[0]).longValue(),
                r -> new TenantRevenue(
                        ((Number) r[1]).longValue(),
                        toBigDecimal(r[2]))));
    }

    public long countAllOrders(String excludeSlug) {
        Number n = (Number) em.createNativeQuery("""
                SELECT COUNT(*)
                FROM orders o
                JOIN tenants t ON t.id = o.tenant_id
                WHERE t.slug <> :slug
                """)
                .setParameter("slug", excludeSlug)
                .getSingleResult();
        return n != null ? n.longValue() : 0L;
    }

    public BigDecimal sumAllRevenue(String excludeSlug) {
        Object value = em.createNativeQuery("""
                SELECT COALESCE(SUM(tx.amount), 0)
                FROM transactions tx
                JOIN tenants t ON t.id = tx.tenant_id
                WHERE tx.status = 'COMPLETED' AND t.slug <> :slug
                """)
                .setParameter("slug", excludeSlug)
                .getSingleResult();
        return toBigDecimal(value);
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(value.toString());
    }
}
