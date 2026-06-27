package com.koicoffee.backend.repository;

import com.koicoffee.backend.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByOrderByIdDesc();
    List<Order> findByCreatedAtAfterOrderByIdDesc(LocalDateTime createdAt);

    @Query("SELECT COALESCE(SUM(o.totalPrice - o.discount), 0) FROM Order o WHERE o.status = 'PAID' AND o.paymentMethod = :method AND o.paymentTime >= :startTime")
    Long calculateRevenueSince(@Param("method") String method, @Param("startTime") LocalDateTime startTime);
}