package com.koicoffee.backend.controller;

import com.koicoffee.backend.model.CashRegister;
import com.koicoffee.backend.model.Order;
import com.koicoffee.backend.model.OrderDetail;
import com.koicoffee.backend.model.Product;
import com.koicoffee.backend.repository.CashRegisterRepository;
import com.koicoffee.backend.repository.OrderDetailRepository;
import com.koicoffee.backend.repository.OrderRepository;
import com.koicoffee.backend.repository.ProductRepository;
import com.koicoffee.backend.service.WsNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CashRegisterRepository cashRegisterRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final WsNotificationService wsNotificationService;

    private int parseIntSafe(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty() || "null".equals(String.valueOf(value))) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long parseLongSafe(Object value) {
        if (value == null || String.valueOf(value).trim().isEmpty() || "null".equals(String.valueOf(value))) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    // 🚀 GET /api/orders: Phân trang & Giới hạn record từ Database chống Slow Query
    @GetMapping
    public Map<String, Object> getAllOrders(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "7") int days) {

        int pageSize = Math.min(Math.max(size, 1), 100);
        int pageIndex = Math.max(page, 0);

        PageRequest pageRequest = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<Order> orderPage;

        if (search != null && !search.trim().isEmpty()) {
            orderPage = orderRepository.searchOrders(status, search.trim(), pageRequest);
        } else if (status != null && !status.trim().isEmpty()) {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);
            orderPage = orderRepository.findByStatusAndCreatedAtAfter(status.trim(), cutoffTime, pageRequest);
        } else {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);
            orderPage = orderRepository.findByCreatedAtAfter(cutoffTime, pageRequest);
        }

        LocalDateTime lastShiftEnd = cashRegisterRepository.findById(1L)
                .map(CashRegister::getLastUpdated)
                .orElse(LocalDateTime.MIN);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", orderPage.getContent());
        response.put("lastShiftEnd", lastShiftEnd);

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", orderPage.getNumber());
        pagination.put("size", orderPage.getSize());
        pagination.put("totalElements", orderPage.getTotalElements());
        pagination.put("totalPages", orderPage.getTotalPages());
        pagination.put("hasNext", orderPage.hasNext());
        pagination.put("hasPrevious", orderPage.hasPrevious());
        response.put("pagination", pagination);

        return response;
    }

    @PostMapping
    @Transactional
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> payload) {
        Order order = new Order();
        String timeCode = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMHHmmss"));
        order.setCode("HD" + timeCode);
        order.setStaffName((String) payload.get("staffName"));

        order.setTotalPrice(parseLongSafe(payload.get("totalPrice")));
        order.setDiscount(parseLongSafe(payload.get("discount")));

        order.setCreatedAt(LocalDateTime.now());
        order.setStatus("PENDING");

        order = orderRepository.save(order);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
        for (Map<String, Object> itemData : items) {
            Long productId = Long.valueOf(itemData.get("id").toString());
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại!"));

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(parseIntSafe(itemData.get("quantity")));
            detail.setPrice(parseIntSafe(itemData.get("price")));
            detail.setNote(itemData.get("note") != null ? String.valueOf(itemData.get("note")) : null);

            orderDetailRepository.save(detail);
            order.getOrderDetails().add(detail);
        }

        // Bắn WebSocket thông báo đơn hàng mới
        wsNotificationService.broadcastOrder("CREATED", order.getId(), "Đơn mới [" + order.getCode() + "] được tạo");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", order);
        return response;
    }

    @PutMapping("/{id}/pay")
    @Transactional
    public Map<String, Object> payOrder(@PathVariable Long id, @RequestParam String method) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        order.setStatus("PAID");
        order.setPaymentMethod(method);
        order.setPaymentTime(LocalDateTime.now());
        orderRepository.save(order);

        wsNotificationService.broadcastOrder("PAID", id, "Đơn [" + order.getCode() + "] đã thanh toán (" + method + ")");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @PutMapping("/{id}")
    @Transactional
    public Map<String, Object> updateOrder(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        orderDetailRepository.deleteAll(order.getOrderDetails());
        order.getOrderDetails().clear();

        order.setTotalPrice(parseLongSafe(payload.get("totalPrice")));
        order.setDiscount(parseLongSafe(payload.get("discount")));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
        for (Map<String, Object> itemData : items) {
            Long productId = Long.valueOf(itemData.get("id").toString());
            Product product = productRepository.findById(productId).orElseThrow();

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(parseIntSafe(itemData.get("quantity")));
            detail.setPrice(parseIntSafe(itemData.get("price")));
            detail.setNote(itemData.get("note") != null ? String.valueOf(itemData.get("note")) : null);

            orderDetailRepository.save(detail);
            order.getOrderDetails().add(detail);
        }

        orderRepository.save(order);
        wsNotificationService.broadcastOrder("UPDATED", id, "Đơn [" + order.getCode() + "] vừa cập nhật món");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @PutMapping("/{id}/status")
    @Transactional
    public Map<String, Object> changeOrderStatus(@PathVariable Long id, @RequestParam String status) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setStatus(status);
        orderRepository.save(order);

        wsNotificationService.broadcastOrder("STATUS_CHANGED", id, "Đơn [" + order.getCode() + "] chuyển sang " + status);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @PutMapping("/{id}/payment-method")
    @Transactional
    public Map<String, Object> changePaymentMethod(@PathVariable Long id, @RequestParam String method) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setPaymentMethod(method);
        orderRepository.save(order);

        wsNotificationService.broadcastOrder("METHOD_CHANGED", id, "Đơn [" + order.getCode() + "] đổi PT thanh toán: " + method);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @PutMapping("/{id}/cancel")
    @Transactional
    public Map<String, Object> cancelOrder(@PathVariable Long id) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setStatus("CANCELLED");
        orderRepository.save(order);

        wsNotificationService.broadcastOrder("CANCELLED", id, "Đơn [" + order.getCode() + "] đã hủy bỏ");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @PutMapping("/{id}/restore")
    @Transactional
    public Map<String, Object> restoreOrder(@PathVariable Long id) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setStatus("PENDING");
        order.setPaymentMethod(null);
        order.setPaymentTime(null);
        orderRepository.save(order);

        wsNotificationService.broadcastOrder("RESTORED", id, "Đơn [" + order.getCode() + "] đã được khôi phục");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Map<String, Object> deleteOrder(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        orderRepository.delete(order);
        wsNotificationService.broadcastOrder("DELETED", id, "Đơn [" + order.getCode() + "] đã bị xóa");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đã xóa đơn hàng thành công");
        return response;
    }

    @PostMapping("/{id}/split")
    @Transactional
    public Map<String, Object> splitOrder(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Order parentOrder = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng gốc"));

        String method = (String) payload.get("paymentMethod");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> splitItems = (List<Map<String, Object>>) payload.get("items");

        int totalParentQty = parentOrder.getOrderDetails().stream().mapToInt(OrderDetail::getQuantity).sum();
        int totalSplitQty = splitItems.stream().mapToInt(item -> parseIntSafe(item.get("quantity"))).sum();

        Order childOrder = new Order();
        childOrder.setCode(parentOrder.getCode() + "-T" + System.currentTimeMillis() % 1000);
        childOrder.setParentOrderId(parentOrder.getId());
        childOrder.setStaffName(parentOrder.getStaffName());
        childOrder.setCreatedAt(LocalDateTime.now());
        childOrder.setStatus("PAID");
        childOrder.setPaymentMethod(method);
        childOrder.setPaymentTime(LocalDateTime.now());
        childOrder.setTotalPrice(0L);
        childOrder.setDiscount(0L);

        childOrder = orderRepository.save(childOrder);

        long splitTotal = 0;
        for (Map<String, Object> sItem : splitItems) {
            Long prodId = Long.valueOf(sItem.get("id").toString());
            int splitQty = parseIntSafe(sItem.get("quantity"));
            int splitPrice = parseIntSafe(sItem.get("price"));
            String note = sItem.get("note") != null ? String.valueOf(sItem.get("note")) : null;

            if (splitQty <= 0) continue;

            Product product = productRepository.findById(prodId).orElseThrow();
            OrderDetail cDetail = new OrderDetail();
            cDetail.setOrder(childOrder);
            cDetail.setProduct(product);
            cDetail.setQuantity(splitQty);
            cDetail.setPrice(splitPrice);
            cDetail.setNote(note);
            orderDetailRepository.save(cDetail);
            childOrder.getOrderDetails().add(cDetail);

            splitTotal += (long) splitPrice * splitQty;

            for (OrderDetail pDetail : parentOrder.getOrderDetails()) {
                if (pDetail.getProduct().getId().equals(prodId)) {
                    pDetail.setQuantity(pDetail.getQuantity() - splitQty);
                    break;
                }
            }
        }

        childOrder.setTotalPrice(splitTotal);
        orderRepository.save(childOrder);

        parentOrder.getOrderDetails().removeIf(d -> d.getQuantity() <= 0);

        if (parentOrder.getOrderDetails().isEmpty() || totalSplitQty >= totalParentQty) {
            parentOrder.setStatus("PAID");
            parentOrder.setPaymentMethod(method);
            parentOrder.setPaymentTime(LocalDateTime.now());
        } else {
            long newParentTotal = 0;
            for (OrderDetail d : parentOrder.getOrderDetails()) {
                newParentTotal += (long) d.getPrice() * d.getQuantity();
            }
            parentOrder.setTotalPrice(newParentTotal);
        }
        orderRepository.save(parentOrder);

        wsNotificationService.broadcastOrder("SPLIT", id, "Tách đơn [" + parentOrder.getCode() + "] thành công");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    // 🚀 TỐI ƯU getShiftSummary: Query thẳng từ SQL bằng Index thay vì load toàn bộ DB
    @GetMapping("/shift-summary")
    public Map<String, Object> getShiftSummary() {
        LocalDateTime lastShiftEnd = cashRegisterRepository.findById(1L)
                .map(CashRegister::getLastUpdated)
                .orElse(LocalDateTime.MIN);

        List<Order> shiftOrders = orderRepository.findByStatusAndPaymentTimeAfter("PAID", lastShiftEnd);

        long totalCash = 0;
        long totalTransfer = 0;

        for (Order o : shiftOrders) {
            long finalPrice = Math.max(0, (o.getTotalPrice() != null ? o.getTotalPrice() : 0) - (o.getDiscount() != null ? o.getDiscount() : 0));

            if ("CASH".equalsIgnoreCase(o.getPaymentMethod())) {
                totalCash += finalPrice;
            } else if ("TRANSFER".equalsIgnoreCase(o.getPaymentMethod())) {
                totalTransfer += finalPrice;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("lastShiftEnd", lastShiftEnd);
        response.put("totalCash", totalCash);
        response.put("totalTransfer", totalTransfer);
        response.put("totalRevenue", totalCash + totalTransfer);
        response.put("totalOrders", shiftOrders.size());

        return response;
    }

    @RequestMapping(value = "/close-shift", method = {RequestMethod.PUT, RequestMethod.POST})
    @Transactional
    public Map<String, Object> closeShift(@RequestBody(required = false) Map<String, Object> payload) {
        CashRegister cashRegister = cashRegisterRepository.findById(1L)
                .orElse(new CashRegister());

        cashRegister.setId(1L);
        cashRegister.setLastUpdated(LocalDateTime.now());

        cashRegisterRepository.save(cashRegister);
        wsNotificationService.broadcastShift("CLOSE", "Đã chốt ca làm việc!");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đã chốt ca thành công!");
        return response;
    }
}
