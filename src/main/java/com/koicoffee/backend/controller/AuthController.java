package com.koicoffee.backend.controller;

import com.koicoffee.backend.model.User;
import com.koicoffee.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // 🚀 THÊM: Inject công cụ gửi tin nhắn WebSocket
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        Map<String, Object> response = new HashMap<>();

        try {
            String username = credentials.get("username");
            String password = credentials.get("password");

            // 1. Tìm user trong DB
            Optional<User> userOpt = userRepository.findAll().stream()
                    .filter(u -> u.getUsername().equals(username))
                    .findFirst();

            if (userOpt.isEmpty()) {
                response.put("status", "error");
                response.put("message", "Sai tên đăng nhập hoặc mật khẩu!");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            User foundUser = userOpt.get();

            // 2. Kiểm tra tài khoản bị khóa
            if (foundUser.getIsActive() != null && !foundUser.getIsActive()) {
                response.put("status", "error");
                response.put("message", "Tài khoản của bạn đã bị vô hiệu hóa bởi Quản trị viên!");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // 3. Kiểm tra mật khẩu
            if (!passwordEncoder.matches(password, foundUser.getPassword())) {
                response.put("status", "error");
                response.put("message", "Sai tên đăng nhập hoặc mật khẩu!");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 🚀 THÊM: Sinh mã Session ngẫu nhiên cho lần đăng nhập này
            String sessionId = UUID.randomUUID().toString();

            Map<String, Object> userData = new HashMap<>();
            userData.put("id", foundUser.getId());
            userData.put("username", foundUser.getUsername());
            userData.put("fullName", foundUser.getFullName());
            userData.put("role", foundUser.getRole());
            userData.put("sessionId", sessionId);

            // 4. Đăng nhập thành công
            response.put("status", "success");
            response.put("data", userData);

            // 🚀 THÊM: Gửi lệnh KICKOUT kèm SessionID mới qua Socket để văng các máy cũ
            messagingTemplate.convertAndSend("/topic/kickout/" + foundUser.getId(), sessionId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Lỗi hệ thống không xác định!");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}