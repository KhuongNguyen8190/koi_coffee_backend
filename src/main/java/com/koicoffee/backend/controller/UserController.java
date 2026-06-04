package com.koicoffee.backend.controller;

import com.koicoffee.backend.model.User;
import com.koicoffee.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public Map<String, Object> getAllUsers() {
        List<User> users = userRepository.findAll();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", users);
        return response;
    }

    @PostMapping
    public Map<String, Object> createUser(@RequestBody User user) {
        // Mặc định tạo tài khoản nhân viên nếu không truyền
        if (user.getRole() == null) user.setRole("STAFF");

        User savedUser = userRepository.save(user);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", savedUser);
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        user.setUsername(userDetails.getUsername());
        user.setFullName(userDetails.getFullName());
        user.setRole(userDetails.getRole());
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(userDetails.getPassword());
        }

        userRepository.save(user);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        return response;
    }
}