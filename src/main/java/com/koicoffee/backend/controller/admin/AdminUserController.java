package com.koicoffee.backend.controller.admin;

import com.koicoffee.backend.dto.PageResponse;
import com.koicoffee.backend.model.User;
import com.koicoffee.backend.service.UserManagementService;
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
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserManagementService userManagementService;
    private final WsNotificationService wsNotificationService;

    @GetMapping
    public Map<String, Object> getAllUsers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search) {

        // Phân trang với LIMIT / OFFSET ở mức database
        if (page != null && size != null) {
            int pageSize = Math.min(Math.max(size, 1), 100);
            Page<User> userPage = userManagementService.searchUsersPaged(
                    role, search,
                    PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
            return PageResponse.of(userPage).toMap();
        }

        // Lấy từ Cache (Chỉ query DB lại khi có user thay đổi)
        List<User> users = userManagementService.getAllUsersCached();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", users);
        return response;
    }

    @PostMapping
    public Map<String, Object> createUser(@RequestBody User userReq) {
        User savedUser = userManagementService.createUser(userReq);
        wsNotificationService.broadcastUser("CREATE", savedUser.getUsername(), "Nhân viên mới: " + savedUser.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", savedUser);
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateUser(@PathVariable Long id, @RequestBody User userReq) {
        User updatedUser = userManagementService.updateUser(id, userReq);

        if (Boolean.FALSE.equals(updatedUser.getIsActive())) {
            wsNotificationService.broadcastUser("LOCK", updatedUser.getUsername(), "Tài khoản bị khóa: " + updatedUser.getUsername());
        } else {
            wsNotificationService.broadcastUser("UPDATE", updatedUser.getUsername(), "Tài khoản đã cập nhật: " + updatedUser.getUsername());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", updatedUser);
        return response;
    }

    @PutMapping("/{id}/reset-password")
    public Map<String, Object> resetPassword(@PathVariable Long id) {
        userManagementService.resetPassword(id, "12345");
        User user = userManagementService.getUserById(id);
        wsNotificationService.broadcastUser("UPDATE", user.getUsername(), "Mật khẩu đã được reset về 12345");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Đã reset mật khẩu về 12345");
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id) {
        String deletedUsername = userManagementService.deleteUser(id);
        wsNotificationService.broadcastUser("LOCK", deletedUsername, "Tài khoản đã bị xóa");

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }
}
