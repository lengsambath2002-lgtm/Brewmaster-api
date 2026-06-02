package com.sambath.admincafe.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.createdAt >= :start AND t.createdAt < :end")
    BigDecimal sumAmountBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.createdAt >= :start AND t.createdAt < :end")
    long countBetween(@Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COALESCE(AVG(t.amount), 0) FROM Transaction t WHERE t.createdAt >= :start AND t.createdAt < :end")
    BigDecimal avgAmountBetween(@Param("start") Instant start, @Param("end") Instant end);
}
