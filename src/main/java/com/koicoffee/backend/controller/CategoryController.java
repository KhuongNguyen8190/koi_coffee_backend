package com.koicoffee.backend.controller;

import com.koicoffee.backend.model.Category;
import com.koicoffee.backend.service.CategoryService;
import com.koicoffee.backend.service.WsNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final WsNotificationService wsNotificationService;

    @GetMapping
    public Map<String, Object> getAllCategories() {
        List<Category> categories = categoryService.getAllCategoriesCached();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", categories);
        return response;
    }

    @PostMapping
    public Map<String, Object> createCategory(@RequestBody Category category) {
        Category savedCat = categoryService.createCategory(category.getName());
        wsNotificationService.broadcastMenu("CATEGORY_CREATED", savedCat.getId(), "Danh mục mới: " + savedCat.getName());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", savedCat);
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateCategory(@PathVariable Long id, @RequestBody Category catDetails) {
        Category updated = categoryService.updateCategory(id, catDetails.getName());
        wsNotificationService.broadcastMenu("CATEGORY_UPDATED", id, "Danh mục đã cập nhật: " + updated.getName());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", updated);
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        wsNotificationService.broadcastMenu("CATEGORY_DELETED", id, "Danh mục đã bị xóa");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }
}
