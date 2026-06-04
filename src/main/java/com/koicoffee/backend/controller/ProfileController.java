package com.koicoffee.backend.controller;

import com.koicoffee.backend.dto.ProfileUpdateRequest;
import com.koicoffee.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users/profile")
@CrossOrigin(origins = "*")
public class ProfileController {

    @Autowired
    private UserService userService;

    // ==========================================================
    // 🚀 API ĐỔI THÔNG TIN CÁ NHÂN (TÊN & MẬT KHẨU)
    // ==========================================================
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @PathVariable Long id,
            @RequestBody ProfileUpdateRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Gọi logic xử lý từ Service
            userService.updateProfile(id, request);

            // Trả về JSON chuẩn cho Frontend bắt trạng thái 'success'
            response.put("status", "success");
            response.put("message", "Cập nhật thông tin cá nhân thành công!");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // Bắt lỗi do người dùng nhập sai mật khẩu cũ hoặc không tìm thấy user
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            // Bắt các lỗi hệ thống không lường trước
            response.put("status", "error");
            response.put("message", "Đã xảy ra lỗi hệ thống khi cập nhật!");
            return ResponseEntity.internalServerError().body(response);
        }
    }
}