package com.koicoffee.backend.controller.admin;

import com.koicoffee.backend.dto.PageResponse;
import com.koicoffee.backend.model.Category;
import com.koicoffee.backend.service.CategoryService;
import com.koicoffee.backend.service.NotificationService;
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
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final NotificationService notificationService;
    private final WsNotificationService wsNotificationService;

    @GetMapping
    public Map<String, Object> getAllCategories(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search) {

        // Nếu client yêu cầu phân trang
        if (page != null && size != null) {
            int pageSize = Math.min(Math.max(size, 1), 100);
            Page<Category> categoryPage = categoryService.getCategoriesPaged(
                    search, PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "name")));
            return PageResponse.of(categoryPage).toMap();
        }

        // Mặc định lấy từ Cache siêu tốc
        List<Category> categories = categoryService.getAllCategoriesCached();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", categories);
        return response;
    }

    @PostMapping
    public Map<String, Object> createCategory(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = (String) payload.get("name");
            Category savedCategory = categoryService.createCategory(name);

            String message = "🆕 Danh mục mới [" + savedCategory.getName() + "] vừa được Admin thêm vào!";
            notificationService.createNotification(message, null);
            wsNotificationService.broadcastMenu("CATEGORY_CREATED", savedCategory.getId(), message);

            response.put("status", "success");
            response.put("data", savedCategory);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateCategory(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = (String) payload.get("name");
            Category category = categoryService.updateCategory(id, name);

            String message = "📝 Danh mục [" + category.getName() + "] vừa được Admin cập nhật!";
            notificationService.createNotification(message, null);
            wsNotificationService.broadcastMenu("CATEGORY_UPDATED", id, message);

            response.put("status", "success");
            response.put("data", category);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteCategory(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Category category = categoryService.getCategoryById(id);
            String catName = category.getName();

            categoryService.deleteCategory(id);

            String message = "🗑️ Danh mục [" + catName + "] vừa bị Admin xóa!";
            notificationService.createNotification(message, null);
            wsNotificationService.broadcastMenu("CATEGORY_DELETED", id, message);

            response.put("status", "success");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }
}
