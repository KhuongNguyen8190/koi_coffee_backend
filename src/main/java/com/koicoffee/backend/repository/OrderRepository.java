package com.koicoffee.backend.repository;

import com.koicoffee.backend.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Phân trang với LIMIT & OFFSET trực tiếp từ SQL
    Page<Order> findByCreatedAtAfter(LocalDateTime createdAt, Pageable pageable);
    Page<Order> findByStatus(String status, Pageable pageable);
    Page<Order> findByStatusAndCreatedAtAfter(String status, LocalDateTime createdAt, Pageable pageable);
    List<Order> findByStatusAndPaymentTimeAfter(String status, LocalDateTime paymentTime);

    @Query("SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR :status = '' OR o.status = :status) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(o.code) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(o.staffName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> searchOrders(@Param("status") String status, @Param("search") String search, Pageable pageable);

    @Query("SELECT COALESCE(SUM(o.totalPrice - o.discount), 0) FROM Order o WHERE o.status = 'PAID' AND o.paymentMethod = :method AND o.paymentTime >= :startTime")
    Long calculateRevenueSince(@Param("method") String method, @Param("startTime") LocalDateTime startTime);
}
