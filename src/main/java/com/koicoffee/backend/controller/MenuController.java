package com.koicoffee.backend.controller;

import com.koicoffee.backend.model.Product;
import com.koicoffee.backend.service.MenuService;
import com.koicoffee.backend.service.ProductService;
import com.koicoffee.backend.service.WsNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;
    private final ProductService productService;
    private final WsNotificationService wsNotificationService;

    // 1. ROUTE: GET /api/menu - Lấy toàn bộ thực đơn từ Cache (Chỉ query DB khi có thay đổi)
    @GetMapping
    public Map<String, Object> getMenu() {
        Map<String, Object> data = menuService.getFullMenuCached();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);
        return response;
    }

    // 2. ROUTE: GET /api/menu/product/{id}
    @GetMapping("/product/{id}")
    public Map<String, Object> getProductById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Product product = productService.getProductById(id);
            response.put("status", "success");
            response.put("data", product);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    // 3. ROUTE: POST /api/menu/product - Thêm món mới vào thực đơn
    @PostMapping("/product")
    public Map<String, Object> addProduct(@RequestBody Product newProduct) {
        Long catId = newProduct.getCategory() != null ? newProduct.getCategory().getId() : null;
        Product saved = productService.createProduct(
                newProduct.getName(),
                newProduct.getPrice() != null ? newProduct.getPrice() : 0,
                newProduct.getStatus(),
                catId,
                newProduct.getSku()
        );

        wsNotificationService.broadcastMenu("PRODUCT_CREATED", saved.getId(), "Thêm món mới: " + saved.getName());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đã thêm món mới thành công");
        response.put("data", saved);
        return response;
    }
}
