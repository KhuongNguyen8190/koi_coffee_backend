package com.koicoffee.backend.repository;

import com.koicoffee.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Hàm này giúp tìm user dựa trên tên đăng nhập
    Optional<User> findByUsername(String username);
    // Hàm admin quản lý User
    boolean existsByUsername(String username);
}