package com.koicoffee.backend.controller;

import com.koicoffee.backend.dto.PageResponse;
import com.koicoffee.backend.model.Product;
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
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final WsNotificationService wsNotificationService;

    @GetMapping
    public Map<String, Object> getAllProducts(
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
                    PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "name")));
            return PageResponse.of(productPage).toMap();
        }

        // Lấy danh sách sản phẩm đang bán từ Cache
        List<Product> products = "all".equalsIgnoreCase(status)
                ? productService.getAllProductsCached()
                : productService.getActiveProductsCached();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", products);
        return response;
    }

    @PostMapping
    public Map<String, Object> createProduct(@RequestBody Map<String, Object> payload) {
        String name = (String) payload.get("name");
        int price = Integer.parseInt(payload.get("price").toString());
        String status = (String) payload.getOrDefault("status", "active");
        String sku = (String) payload.get("sku");
        Long categoryId = payload.get("categoryId") != null ? Long.valueOf(payload.get("categoryId").toString()) : null;

        Product savedProduct = productService.createProduct(name, price, status, categoryId, sku);
        wsNotificationService.broadcastMenu("PRODUCT_CREATED", savedProduct.getId(), "Sản phẩm mới: " + savedProduct.getName());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", savedProduct);
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateProduct(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        String name = (String) payload.get("name");
        Integer price = payload.get("price") != null ? Integer.parseInt(payload.get("price").toString()) : null;
        String status = (String) payload.get("status");
        String sku = (String) payload.get("sku");
        Long categoryId = payload.get("categoryId") != null ? Long.valueOf(payload.get("categoryId").toString()) : null;

        Product updated = productService.updateProduct(id, name, price, status, categoryId, sku);
        wsNotificationService.broadcastMenu("PRODUCT_UPDATED", id, "Đã cập nhật: " + updated.getName());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", updated);
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        wsNotificationService.broadcastMenu("PRODUCT_DELETED", id, "Đã xóa sản phẩm");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }
}
