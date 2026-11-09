package com.koicoffee.backend.controller.admin;

import com.koicoffee.backend.dto.PageResponse;
import com.koicoffee.backend.model.Order;
import com.koicoffee.backend.repository.OrderRepository;
import com.koicoffee.backend.service.NotificationService;
import com.koicoffee.backend.service.WsNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final WsNotificationService wsNotificationService;

    // 🚀 Lấy danh sách hóa đơn Admin: Phân trang & Giới hạn từ SQL trực tiếp, chống Slow Query
    @GetMapping
    public Map<String, Object> getAllOrders(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        int pageSize = Math.min(Math.max(size, 1), 100);
        int pageIndex = Math.max(page, 0);

        PageRequest pageRequest = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<Order> orderPage;

        if ((search != null && !search.trim().isEmpty()) || (status != null && !status.trim().isEmpty())) {
            orderPage = orderRepository.searchOrders(status, search != null ? search.trim() : null, pageRequest);
        } else {
            orderPage = orderRepository.findAll(pageRequest);
        }

        return PageResponse.of(orderPage).toMap();
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateOrder(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

            String oldStatus = order.getStatus();

            if (payload.containsKey("status")) {
                order.setStatus((String) payload.get("status"));
            }
            if (payload.containsKey("note")) {
                order.setNote((String) payload.get("note"));
            }

            orderRepository.save(order);

            String newStatus = order.getStatus();
            if (oldStatus != null && newStatus != null && !oldStatus.equalsIgnoreCase(newStatus)) {
                String statusVN;
                switch (newStatus.toUpperCase()) {
                    case "PAID": statusVN = "ĐÃ THANH TOÁN"; break;
                    case "CANCELLED": statusVN = "ĐÃ HỦY BỎ"; break;
                    case "PENDING": statusVN = "CHƯA THANH TOÁN"; break;
                    default: statusVN = newStatus;
                }

                String message = "🧾 Hóa đơn [" + order.getCode() + "] vừa được Admin chuyển trạng thái thành: " + statusVN;
                notificationService.createNotification(message, null);
                wsNotificationService.broadcastOrder("STATUS_CHANGED", id, message);
            } else {
                wsNotificationService.broadcastOrder("UPDATED", id, "Hóa đơn [" + order.getCode() + "] vừa được cập nhật");
            }

            response.put("status", "success");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteOrder(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
            String orderCode = order.getCode();

            orderRepository.deleteById(id);

            String message = "🗑️ Hóa đơn [" + orderCode + "] vừa bị Admin xóa khỏi hệ thống!";
            notificationService.createNotification(message, null);
            wsNotificationService.broadcastOrder("DELETED", id, message);

            response.put("status", "success");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }
}
