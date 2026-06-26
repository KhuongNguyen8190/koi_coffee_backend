package com.koicoffee.backend.controller;

import com.koicoffee.backend.model.Category;
import com.koicoffee.backend.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    public Map<String, Object> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", categories);
        return response;
    }

    @PostMapping
    public Map<String, Object> createCategory(@RequestBody Category category) {
        Category savedCat = categoryRepository.save(category);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", savedCat);
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateCategory(@PathVariable Long id, @RequestBody Category catDetails) {
        Category cat = categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
        cat.setName(catDetails.getName());
        categoryRepository.save(cat);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteCategory(@PathVariable Long id) {
        categoryRepository.deleteById(id);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }
}