package com.koicoffee.backend.service;

import com.koicoffee.backend.config.CacheConfig;
import com.koicoffee.backend.model.User;
import com.koicoffee.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Cacheable(value = CacheConfig.CACHE_USERS, key = "'all'")
    public List<User> getAllUsersCached() {
        log.info("Fetching users from DATABASE (cache miss)...");
        return userRepository.findAllByOrderByCreatedAtDesc();
    }

    public Page<User> searchUsersPaged(String role, String search, Pageable pageable) {
        log.debug("Fetching paged users from DATABASE (page={}, size={}, role={}, search={})",
                pageable.getPageNumber(), pageable.getPageSize(), role, search);
        return userRepository.searchUsers(role, search, pageable);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + id));
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_USERS, allEntries = true)
    public User createUser(User userReq) {
        log.info("Creating user '{}' and evicting users cache", userReq.getUsername());
        if (userRepository.existsByUsername(userReq.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại!");
        }

        userReq.setCreatedAt(LocalDateTime.now());
        if (userReq.getIsActive() == null) userReq.setIsActive(true);
        if (userReq.getRole() == null || userReq.getRole().isEmpty()) userReq.setRole("STAFF");

        String rawPassword = (userReq.getPassword() != null && !userReq.getPassword().trim().isEmpty())
                ? userReq.getPassword().trim()
                : "12345";
        userReq.setPassword(passwordEncoder.encode(rawPassword));

        return userRepository.save(userReq);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_USERS, allEntries = true)
    public User updateUser(Long id, User userReq) {
        log.info("Updating user ID {} and evicting users cache", id);
        User existingUser = getUserById(id);

        if (userReq.getFullName() != null) existingUser.setFullName(userReq.getFullName());
        if (userReq.getRole() != null) existingUser.setRole(userReq.getRole());
        if (userReq.getIsActive() != null) existingUser.setIsActive(userReq.getIsActive());

        if (userReq.getPassword() != null && !userReq.getPassword().trim().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(userReq.getPassword().trim()));
        }

        return userRepository.save(existingUser);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_USERS, allEntries = true)
    public void resetPassword(Long id, String newPassword) {
        log.info("Resetting password for user ID {} and evicting users cache", id);
        User existingUser = getUserById(id);
        String pass = (newPassword != null && !newPassword.trim().isEmpty()) ? newPassword.trim() : "12345";
        existingUser.setPassword(passwordEncoder.encode(pass));
        userRepository.save(existingUser);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_USERS, allEntries = true)
    public String deleteUser(Long id) {
        log.info("Deleting user ID {} and evicting users cache", id);
        User existingUser = getUserById(id);
        String username = existingUser.getUsername();
        userRepository.delete(existingUser);
        return username;
    }
}

