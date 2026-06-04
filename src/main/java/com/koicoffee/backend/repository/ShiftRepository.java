package com.koicoffee.backend.repository;

import com.koicoffee.backend.model.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {
    // Lấy danh sách lịch sử ca làm việc, sắp xếp mới nhất (ID lớn nhất) lên đầu
    List<Shift> findAllByOrderByIdDesc();
    Optional<Shift> findFirstByStatusOrderByStartTimeDesc(String status);
}