package com.koicoffee.backend.service;

import com.koicoffee.backend.dto.ProfileUpdateRequest;
import com.koicoffee.backend.model.User;
import com.koicoffee.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ==========================================================
    // 🚀 HÀM CẬP NHẬT THÔNG TIN CÁ NHÂN (PROFILE)
    // ==========================================================
    public void updateProfile(Long userId, ProfileUpdateRequest request) {
        // 1. Tìm người dùng trong Database (Sử dụng hàm findById có sẵn của JpaRepository)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người dùng!"));

        // 2. Cập nhật Họ và tên
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }

        // 3. Xử lý đổi mật khẩu (nếu có yêu cầu)
        if (request.getNewPassword() != null && !request.getNewPassword().trim().isEmpty()) {

            // Bắt buộc phải có mật khẩu cũ để đối chiếu
            if (request.getOldPassword() == null || request.getOldPassword().trim().isEmpty()) {
                throw new RuntimeException("Vui lòng nhập mật khẩu hiện tại để xác thực!");
            }

            // Dùng PasswordEncoder (BCrypt) để kiểm tra mật khẩu cũ có khớp trong DB không
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new RuntimeException("Mật khẩu hiện tại không chính xác!");
            }

            // Mã hóa mật khẩu mới và cập nhật vào entity
            user.setPassword(passwordEncoder.encode(request.getNewPassword().trim()));
        }

        // 4. Lưu thay đổi xuống Database
        userRepository.save(user);
    }
}