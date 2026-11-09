package com.koicoffee.backend.controller;

import com.koicoffee.backend.model.User;
import com.koicoffee.backend.service.UserManagementService;
import com.koicoffee.backend.service.WsNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementService userManagementService;
    private final WsNotificationService wsNotificationService;

    @GetMapping
    public Map<String, Object> getAllUsers() {
        List<User> users = userManagementService.getAllUsersCached();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", users);
        return response;
    }

    @PostMapping
    public Map<String, Object> createUser(@RequestBody User user) {
        User savedUser = userManagementService.createUser(user);
        wsNotificationService.broadcastUser("CREATE", savedUser.getUsername(), "Đã tạo tài khoản: " + savedUser.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", savedUser);
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        User updated = userManagementService.updateUser(id, userDetails);
        wsNotificationService.broadcastUser("UPDATE", updated.getUsername(), "Đã cập nhật tài khoản: " + updated.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", updated);
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id) {
        String deletedUsername = userManagementService.deleteUser(id);
        wsNotificationService.broadcastUser("DELETE", deletedUsername, "Đã xóa tài khoản: " + deletedUsername);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }
}
