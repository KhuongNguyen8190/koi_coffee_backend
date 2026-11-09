package com.koicoffee.backend.repository;

import com.koicoffee.backend.model.Shift;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {
    List<Shift> findAllByOrderByIdDesc();
    Page<Shift> findAllByOrderByIdDesc(Pageable pageable);
    Optional<Shift> findFirstByStatusOrderByStartTimeDesc(String status);
}
