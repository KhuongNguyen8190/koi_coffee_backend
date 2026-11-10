package com.koicoffee.backend.controller;

import com.koicoffee.backend.dto.PageResponse;
import com.koicoffee.backend.model.CashRegister;
import com.koicoffee.backend.model.Shift;
import com.koicoffee.backend.repository.CashRegisterRepository;
import com.koicoffee.backend.repository.OrderRepository;
import com.koicoffee.backend.repository.ShiftRepository;
import com.koicoffee.backend.service.WsNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftRepository shiftRepository;
    private final CashRegisterRepository cashRegisterRepository;
    private final OrderRepository orderRepository;
    private final WsNotificationService wsNotificationService;

    // ========================================================
    // 1. API: GET /api/shifts/current - KIỂM TRA XEM CÓ CA NÀO ĐANG MỞ KHÔNG
    // ========================================================
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentShift() {
        Optional<Shift> currentShift = shiftRepository.findFirstByStatusOrderByStartTimeDesc("OPEN");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");

        if (currentShift.isPresent()) {
            response.put("data", currentShift.get());
        } else {
            response.put("data", null);
            response.put("message", "Chưa có ca nào được mở");
        }

        return ResponseEntity.ok(response);
    }

    // ========================================================
    // 2. API: POST /api/shifts/open - MỞ CA LÀM VIỆC MỚI
    // ========================================================
    @PostMapping("/open")
    @Transactional
    public ResponseEntity<Map<String, Object>> openShift(@RequestBody Map<String, Object> payload) {
        Optional<Shift> existingShift = shiftRepository.findFirstByStatusOrderByStartTimeDesc("OPEN");
        if (existingShift.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng kết thúc ca hiện tại trước khi mở ca mới!"));
        }

        long initialCash = Long.parseLong(String.valueOf(payload.getOrDefault("initialCash", "0")));

        Shift shift = new Shift();
        shift.setStatus("OPEN");
        shift.setInitialCash(initialCash);
        shift.setStartTime(LocalDateTime.now());

        if (payload.containsKey("staffName")) {
            shift.setStaffName(String.valueOf(payload.get("staffName")));
        }

        Shift savedShift = shiftRepository.save(shift);
        wsNotificationService.broadcastShift("OPEN", "Ca làm việc mới đã được mở bởi " + shift.getStaffName());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", savedShift);
        response.put("message", "Đã mở ca thành công!");
        return ResponseEntity.ok(response);
    }

    // ========================================================
    // 3. API: POST /api/shifts/close - KẾT CA BÀN GIAO
    // ========================================================
    @PostMapping("/close")
    @Transactional
    public ResponseEntity<Map<String, Object>> closeShift(@RequestBody Map<String, Object> payload) {
        Optional<Shift> currentShiftOpt = shiftRepository.findFirstByStatusOrderByStartTimeDesc("OPEN");

        if (currentShiftOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy ca nào đang mở!"));
        }

        Shift shift = currentShiftOpt.get();
        LocalDateTime now = LocalDateTime.now();

        long actualCashInput = Long.parseLong(String.valueOf(payload.getOrDefault("actualCash", "0")));
        String note = payload.get("note") != null ? String.valueOf(payload.get("note")) : "";

        Long cashRev = orderRepository.calculateRevenueSince("CASH", shift.getStartTime());
        Long transRev = orderRepository.calculateRevenueSince("TRANSFER", shift.getStartTime());

        long cashRevenue = (cashRev != null) ? cashRev : 0L;
        long transferRevenue = (transRev != null) ? transRev : 0L;
        long totalRevenue = cashRevenue + transferRevenue;

        long expectedCash = shift.getInitialCash() + cashRevenue;
        long variance = actualCashInput - expectedCash;

        shift.setBatchCashRevenue(cashRevenue);
        shift.setTransferRevenue(transferRevenue);
        shift.setTotalRevenue(totalRevenue);
        shift.setActualCash(actualCashInput);
        shift.setVariance(variance);
        shift.setNote(note);
        shift.setEndTime(now);
        shift.setStatus("CLOSED");

        Shift savedShift = shiftRepository.save(shift);

        CashRegister register = cashRegisterRepository.findById(1L).orElseGet(() -> {
            CashRegister cr = new CashRegister();
            cr.setBalance(0L);
            return cr;
        });
        register.setBalance(actualCashInput);
        register.setLastUpdated(now);
        cashRegisterRepository.save(register);

        wsNotificationService.broadcastShift("CLOSE", "Ca làm việc đã kết thúc và chốt két tiền!");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đã chốt ca và cập nhật két tiền thành công!");
        response.put("id", savedShift.getId());

        return ResponseEntity.ok(response);
    }

    // ========================================================
    // 4. API: GET /api/shifts/latest-cash - LẤY SỐ DƯ KÉT TIỀN HIỆN TẠI
    // ========================================================
    @GetMapping("/latest-cash")
    public Map<String, Object> getLatestShiftCash() {
        long currentBalance = cashRegisterRepository.findById(1L)
                .map(CashRegister::getBalance)
                .orElse(0L);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("latestCash", currentBalance);

        return response;
    }

    // ========================================================
    // 5. API: GET /api/shifts - LẤY LỊCH SỬ KẾT CA (Hỗ trợ phân trang)
    // ========================================================
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

        List<Shift> history = shiftRepository.findAllByOrderByIdDesc();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", history);

        return response;
    }
}
