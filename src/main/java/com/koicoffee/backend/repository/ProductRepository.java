package com.koicoffee.backend.repository;

import com.koicoffee.backend.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByStatus(String status);
    List<Product> findByStatusOrderByNameAsc(String status);

    Page<Product> findByStatus(String status, Pageable pageable);
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
    Page<Product> findByCategoryIdAndStatus(Long categoryId, String status, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
           "(:status IS NULL OR :status = '' OR p.status = :status) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchProducts(@Param("status") String status,
                                 @Param("categoryId") Long categoryId,
                                 @Param("search") String search,
                                 Pageable pageable);
}
