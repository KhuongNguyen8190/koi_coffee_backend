package com.koicoffee.backend.service;

import com.koicoffee.backend.model.Notification;
import com.koicoffee.backend.model.User;
import com.koicoffee.backend.repository.NotificationRepository;
import com.koicoffee.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository; // Thêm repository của User

    // 1. Lấy thông báo cho User đang đăng nhập
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findNotificationsForUser(userId);
    }

    // 2. Đánh dấu đã đọc
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(notif -> {
            notif.setRead(true);
            notificationRepository.save(notif);
        });
    }

    // 3. Tạo thông báo mới (ĐÃ FIX LOGIC: Tách riêng cho từng tài khoản)
    public void createNotification(String content, Long targetUserId) {
        if (targetUserId != null) {
            // Nếu chỉ định đích danh 1 người (VD: Gửi riêng cho Staff 1)
            Notification notification = new Notification();
            notification.setContent(content);
            notification.setUserId(targetUserId);
            notificationRepository.save(notification);
        } else {
            // Nếu gửi chung toàn hệ thống (Admin cập nhật món/đơn hàng)
            // Lấy toàn bộ user và nhân bản thông báo cho từng người
            List<User> allUsers = userRepository.findAll();
            List<Notification> bulkNotifications = new ArrayList<>();

            for (User user : allUsers) {
                // Chỉ gửi cho các user đang Active
                if (user.getIsActive() != null && user.getIsActive()) {
                    Notification n = new Notification();
                    n.setContent(content);
                    n.setUserId(user.getId()); // Gán ID riêng biệt cho từng người
                    bulkNotifications.add(n);
                }
            }
            // Lưu tất cả vào DB cùng lúc
            notificationRepository.saveAll(bulkNotifications);
        }
    }

    // ===============================================================
    // 4. CRON JOB TỰ ĐỘNG XÓA THÔNG BÁO QUÁ 7 NGÀY
    // Chạy vào lúc 00:00:00 mỗi ngày
    // ===============================================================
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupOldNotifications() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        notificationRepository.deleteOlderThan(sevenDaysAgo);
        System.out.println("Đã dọn dẹp các thông báo cũ hơn 7 ngày (Ngày dọn: " + LocalDateTime.now() + ")");
    }
}