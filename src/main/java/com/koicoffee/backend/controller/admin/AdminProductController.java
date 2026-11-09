package com.koicoffee.backend.controller.admin;

import com.koicoffee.backend.dto.PageResponse;
import com.koicoffee.backend.model.Product;
import com.koicoffee.backend.service.NotificationService;
import com.koicoffee.backend.service.ProductService;
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
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final NotificationService notificationService;
    private final WsNotificationService wsNotificationService;

    @GetMapping
    public Map<String, Object> getAllProductsForAdmin(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status) {

        // Phân trang với LIMIT / OFFSET ở mức database
        if (page != null && size != null) {
            int pageSize = Math.min(Math.max(size, 1), 100);
            Page<Product> productPage = productService.searchProductsPaged(
                    status, categoryId, search,
                    PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "id")));
            return PageResponse.of(productPage).toMap();
        }

        // Lấy tất cả từ cache (tránh scan lại bảng nếu data chưa đổi)
        List<Product> products = productService.getAllProductsCached();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", products);
        return response;
    }

    @PostMapping
    public Map<String, Object> createProduct(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = (String) payload.get("name");
            int price = Integer.parseInt(payload.get("price").toString());
            String status = (String) payload.getOrDefault("status", "active");
            String sku = (String) payload.get("sku");
            Long categoryId = payload.get("categoryId") != null ? Long.valueOf(payload.get("categoryId").toString()) : null;

            Product savedProduct = productService.createProduct(name, price, status, categoryId, sku);

            String message = "🆕 Món mới [" + savedProduct.getName() + "] vừa được Admin thêm vào thực đơn!";
            notificationService.createNotification(message, null);
            wsNotificationService.broadcastMenu("PRODUCT_CREATED", savedProduct.getId(), message);

            response.put("status", "success");
            response.put("data", savedProduct);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateProduct(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            Product oldProduct = productService.getProductById(id);
            String oldStatus = oldProduct.getStatus();

            String name = payload.containsKey("name") ? (String) payload.get("name") : null;
            Integer price = payload.containsKey("price") ? Integer.parseInt(payload.get("price").toString()) : null;
            String status = payload.containsKey("status") ? (String) payload.get("status") : null;
            String sku = payload.containsKey("sku") ? (String) payload.get("sku") : null;
            Long categoryId = payload.get("categoryId") != null ? Long.valueOf(payload.get("categoryId").toString()) : null;

            Product updated = productService.updateProduct(id, name, price, status, categoryId, sku);

            String newStatus = updated.getStatus();
            String message;

            if (oldStatus != null && newStatus != null && !oldStatus.equalsIgnoreCase(newStatus)) {
                String statusVN = newStatus.equalsIgnoreCase("INACTIVE") ? "HẾT HÀNG" : "ĐANG BÁN";
                message = "🍔 Món [" + updated.getName() + "] vừa được Admin chuyển trạng thái thành: " + statusVN;
            } else {
                message = "📝 Món [" + updated.getName() + "] vừa được Admin cập nhật thông tin!";
            }

            notificationService.createNotification(message, null);
            wsNotificationService.broadcastMenu("PRODUCT_UPDATED", id, message);

            response.put("status", "success");
            response.put("data", updated);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteProduct(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Product product = productService.getProductById(id);
            String productName = product.getName();

            productService.deleteProduct(id);

            String message = "🗑️ Món [" + productName + "] vừa bị Admin xóa khỏi hệ thống!";
            notificationService.createNotification(message, null);
            wsNotificationService.broadcastMenu("PRODUCT_DELETED", id, message);

            response.put("status", "success");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Không thể xóa vì sản phẩm này đã nằm trong hóa đơn cũ!");
        }
        return response;
    }
}
