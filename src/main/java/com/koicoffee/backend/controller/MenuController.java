package com.koicoffee.backend.controller;

import com.koicoffee.backend.model.Category;
import com.koicoffee.backend.model.Product;
import com.koicoffee.backend.repository.CategoryRepository;
import com.koicoffee.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/menu") // Định tuyến gốc: http://localhost:8080/api/menu
@CrossOrigin(origins = "http://localhost:5173")
public class MenuController {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    // 1. ROUTE: GET /api/menu
    // Lấy toàn bộ menu (Category và Product)
    @GetMapping
    public Map<String, Object> getMenu() {
        List<Category> categories = categoryRepository.findAll();
        List<Product> products = productRepository.findByStatus("active");

        Map<String, Object> data = new HashMap<>();
        data.put("categories", categories);
        data.put("products", products);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", data);

        return response;
    }

    // 2. ROUTE: GET /api/menu/product/{id}
    // Lấy thông tin chi tiết 1 món cụ thể theo ID
    @GetMapping("/product/{id}")
    public Map<String, Object> getProductById(@PathVariable Long id) {
        Optional<Product> product = productRepository.findById(id);

        Map<String, Object> response = new HashMap<>();
        if (product.isPresent()) {
            response.put("status", "success");
            response.put("data", product.get());
        } else {
            response.put("status", "error");
            response.put("message", "Không tìm thấy sản phẩm");
        }
        return response;
    }

    // 3. ROUTE: POST /api/menu/product
    // Thêm món mới vào thực đơn
    @PostMapping("/product")
    public Map<String, Object> addProduct(@RequestBody Product newProduct) {
        // Lưu vào DB
        Product savedProduct = productRepository.save(newProduct);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đã thêm món mới thành công");
        response.put("data", savedProduct);

        return response;
    }
}