package com.koicoffee.backend.controller;

import com.koicoffee.backend.model.User;
import com.koicoffee.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        Map<String, Object> response = new HashMap<>();

        try {
            String rawUsername = credentials != null ? credentials.get("username") : null;
            String rawPassword = credentials != null ? credentials.get("password") : null;

            if (rawUsername == null || rawUsername.trim().isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
                response.put("status", "error");
                response.put("message", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            String username = rawUsername.trim();
            String password = rawPassword.trim();

            // Tự động khôi phục tài khoản Admin nếu database hoàn toàn trống
            if (userRepository.count() == 0 && "admin".equalsIgnoreCase(username)) {
                User fallbackAdmin = new User();
                fallbackAdmin.setUsername("admin");
                fallbackAdmin.setPassword(passwordEncoder.encode(password.isEmpty() ? "12345" : password));
                fallbackAdmin.setFullName("Quản Trị Viên Hệ Thống");
                fallbackAdmin.setRole("ADMIN");
                fallbackAdmin.setIsActive(true);
                fallbackAdmin.setCreatedAt(LocalDateTime.now());
                userRepository.save(fallbackAdmin);
            }

            // Tìm user chính xác theo username hoặc case-insensitive
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findAll().stream()
                        .filter(u -> u.getUsername() != null && u.getUsername().equalsIgnoreCase(username))
                        .findFirst();
            }

            if (userOpt.isEmpty()) {
                response.put("status", "error");
                response.put("message", "Sai tên đăng nhập hoặc mật khẩu!");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            User foundUser = userOpt.get();

            if (foundUser.getIsActive() != null && !foundUser.getIsActive()) {
                response.put("status", "error");
                response.put("message", "Tài khoản của bạn đã bị vô hiệu hóa bởi Quản trị viên!");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // Kiểm tra mật khẩu (hỗ trợ cả BCrypt và so sánh chuỗi dự phòng / mật khẩu mặc định 12345, 123456)
            boolean matches = false;
            try {
                matches = passwordEncoder.matches(password, foundUser.getPassword());
            } catch (Exception ignored) {
                matches = false;
            }

            // Fallback nếu chưa khớp:
            // 1. Password trong DB lưu dạng plaintext
            // 2. Tài khoản admin / staff1 mặc định đăng nhập bằng 12345 hoặc 123456
            if (!matches) {
                if (password.equals(foundUser.getPassword())) {
                    matches = true;
                } else if ("admin".equalsIgnoreCase(foundUser.getUsername()) && ("12345".equals(password) || "123456".equals(password))) {
                    matches = true;
                } else if ("staff1".equalsIgnoreCase(foundUser.getUsername()) && ("12345".equals(password) || "123456".equals(password))) {
                    matches = true;
                }

                if (matches) {
                    // Tự động mã hóa lại mật khẩu thành BCrypt chuẩn trong DB
                    foundUser.setPassword(passwordEncoder.encode(password));
                    userRepository.save(foundUser);
                }
            }

            if (!matches) {
                response.put("status", "error");
                response.put("message", "Sai tên đăng nhập hoặc mật khẩu!");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Sinh mã Session ngẫu nhiên và lưu trực tiếp vào Database
            String sessionId = UUID.randomUUID().toString();
            foundUser.setCurrentSessionId(sessionId);
            userRepository.save(foundUser);

            Map<String, Object> userData = new HashMap<>();
            userData.put("id", foundUser.getId());
            userData.put("username", foundUser.getUsername());
            userData.put("fullName", foundUser.getFullName());
            userData.put("role", foundUser.getRole());
            userData.put("sessionId", sessionId);

            response.put("status", "success");
            response.put("data", userData);

            // Gửi lệnh KICKOUT máy cũ qua WebSocket
            try {
                messagingTemplate.convertAndSend("/topic/kickout/" + foundUser.getId(), sessionId);
            } catch (Exception ignored) {}

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Lỗi hệ thống không xác định: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // API kiểm tra tính hợp lệ của Session khi Frontend Load trang
    @PostMapping("/verify-session")
    public ResponseEntity<Map<String, Object>> verifySession(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (payload == null || payload.get("userId") == null || payload.get("sessionId") == null) {
                response.put("status", "error");
                response.put("message", "Dữ liệu session không hợp lệ.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Long userId = Long.valueOf(payload.get("userId").toString());
            String sessionId = payload.get("sessionId").toString();

            Optional<User> userOpt = userRepository.findById(userId);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                if (user.getIsActive() != null && !user.getIsActive()) {
                    response.put("status", "error");
                    response.put("message", "Tài khoản đã bị khóa bởi Quản trị viên.");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                }

                // Kiểm tra xem sessionId gửi lên có khớp với sessionId đang lưu trong DB không
                if (sessionId.equals(user.getCurrentSessionId())) {
                    response.put("status", "success");
                    response.put("message", "Session hợp lệ");
                    return ResponseEntity.ok(response);
                }
            }

            // Nếu không khớp hoặc user không tồn tại -> Trả về lỗi 401 Unauthorized
            response.put("status", "error");
            response.put("message", "Session đã hết hạn hoặc được đăng nhập ở nơi khác.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);

        } catch (Exception e) {
            response.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
