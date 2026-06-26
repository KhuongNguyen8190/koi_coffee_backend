package com.koicoffee.backend.controller;

import com.koicoffee.backend.model.Notification;
import com.koicoffee.backend.model.User;
import com.koicoffee.backend.repository.UserRepository;
import com.koicoffee.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // 🚀 BƯỚC MỚI: Tiêm UserRepository để lấy thông tin tài khoản
    @Autowired
    private UserRepository userRepository;

    // API lấy danh sách thông báo
    @GetMapping
    public Map<String, Object> getMyNotifications(@RequestParam Long userId) {
        // 1. Lấy tất cả thông báo của User này (Bao gồm cả thông báo hệ thống chung)
        List<Notification> notifs = notificationService.getUserNotifications(userId);

        // 2. Tìm thông tin User để lấy ngày tạo tài khoản
        User user = userRepository.findById(userId).orElse(null);

        // 3. 🚀 LOGIC LỌC THÔNG BÁO: Chỉ lấy những thông báo có sau khi tài khoản được tạo
        if (user != null && user.getCreatedAt() != null) {
            LocalDateTime accountCreatedAt = user.getCreatedAt();

            notifs = notifs.stream()
                    .filter(n -> n.getCreatedAt() != null && !n.getCreatedAt().isBefore(accountCreatedAt))
                    .collect(Collectors.toList());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", notifs);
        return response;
    }

    // API đánh dấu đã đọc
    @PutMapping("/{id}/read")
    public Map<String, Object> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }
}