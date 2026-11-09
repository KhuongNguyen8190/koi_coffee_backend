package com.koicoffee.backend.controller.admin;

import com.koicoffee.backend.dto.PageResponse;
import com.koicoffee.backend.model.Shift;
import com.koicoffee.backend.repository.ShiftRepository;
import com.koicoffee.backend.service.WsNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/shifts")
@RequiredArgsConstructor
public class AdminShiftController {

    private final ShiftRepository shiftRepository;
    private final WsNotificationService wsNotificationService;

    @GetMapping
    public Map<String, Object> getAllShifts(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        if (page != null && size != null) {
            int pageSize = Math.min(Math.max(size, 1), 100);
            Page<Shift> shiftPage = shiftRepository.findAllByOrderByIdDesc(
                    PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "id")));
            return PageResponse.of(shiftPage).toMap();
        }

        List<Shift> shifts = shiftRepository.findAllByOrderByIdDesc();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", shifts);
        return response;
    }

    // 🚀 API Sửa: Cập nhật chi tiết doanh thu và Giờ mở ca / kết ca
    @PutMapping("/{id}")
    public Map<String, Object> updateShift(@PathVariable Long id, @RequestBody Shift req) {
        Shift existing = shiftRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc!"));

        existing.setStaffName(req.getStaffName());

        if (req.getStartTime() != null) {
            existing.setStartTime(req.getStartTime());
        }
        if (req.getEndTime() != null) {
            existing.setEndTime(req.getEndTime());
        }

        existing.setInitialCash(req.getInitialCash() != null ? req.getInitialCash() : 0);
        existing.setBatchCashRevenue(req.getBatchCashRevenue() != null ? req.getBatchCashRevenue() : 0);
        existing.setTransferRevenue(req.getTransferRevenue() != null ? req.getTransferRevenue() : 0);

        existing.setTotalRevenue(existing.getBatchCashRevenue() + existing.getTransferRevenue());
        existing.setActualCash(req.getActualCash() != null ? req.getActualCash() : 0);
        existing.setNote(req.getNote());
        existing.setVariance(existing.getActualCash() - (existing.getInitialCash() + existing.getTotalRevenue()));

        Shift saved = shiftRepository.save(existing);
        wsNotificationService.broadcastShift("UPDATE", "Ca làm việc #" + id + " đã được cập nhật bởi Admin");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", saved);
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteShift(@PathVariable Long id) {
        shiftRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy ca làm việc này!"));
        shiftRepository.deleteById(id);

        wsNotificationService.broadcastShift("DELETE", "Ca làm việc #" + id + " đã bị xóa bởi Admin");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đã xóa lịch sử ca làm việc!");
        return response;
    }
}
