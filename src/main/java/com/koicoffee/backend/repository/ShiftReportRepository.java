package com.koicoffee.backend.repository;

import com.koicoffee.backend.model.ShiftReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftReportRepository extends JpaRepository<ShiftReport, Long> {
    // Kế thừa sẵn các hàm tìm kiếm, lưu, xóa từ JpaRepository nên không cần viết thêm gì ở đây
}