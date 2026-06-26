package com.koicoffee.backend.controller;

import com.koicoffee.backend.model.Category;
import com.koicoffee.backend.model.Product;
import com.koicoffee.backend.repository.CategoryRepository;
import com.koicoffee.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
// 🚀 1. IMPORT THÊM THƯ VIỆN WEBSOCKET
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // 🚀 2. KHAI BÁO CHIẾC LOA PHÁT THANH
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping
    public Map<String, Object> getAllProducts() {
        List<Product> products = productRepository.findAll();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", products);
        return response;
    }

    @PostMapping
    public Map<String, Object> createProduct(@RequestBody Map<String, Object> payload) {
        Product product = new Product();
        product.setName((String) payload.get("name"));
        product.setPrice(Integer.parseInt(payload.get("price").toString()));

        Long categoryId = Long.valueOf(payload.get("categoryId").toString());
        Category cat = categoryRepository.findById(categoryId).orElseThrow();
        product.setCategory(cat);

        Product savedProduct = productRepository.save(product);

        // 🚀 3. HÉT LÊN: BÁO CÓ SẢN PHẨM MỚI VỪA ĐƯỢC THÊM
        messagingTemplate.convertAndSend("/topic/public", "DATA_CHANGED");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", savedProduct);
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateProduct(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Product product = productRepository.findById(id).orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        product.setName((String) payload.get("name"));
        product.setPrice(Integer.parseInt(payload.get("price").toString()));

        Long categoryId = Long.valueOf(payload.get("categoryId").toString());
        Category cat = categoryRepository.findById(categoryId).orElseThrow();
        product.setCategory(cat);

        productRepository.save(product);

        // 🚀 4. HÉT LÊN: BÁO SẢN PHẨM VỪA BỊ CHỈNH SỬA
        messagingTemplate.convertAndSend("/topic/public", "DATA_CHANGED");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteProduct(@PathVariable Long id) {
        productRepository.deleteById(id);

        // 🚀 5. HÉT LÊN: BÁO SẢN PHẨM ĐÃ BỊ XÓA
        messagingTemplate.convertAndSend("/topic/public", "DATA_CHANGED");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }
}