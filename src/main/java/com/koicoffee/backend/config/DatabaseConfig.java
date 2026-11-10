package com.koicoffee.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url:jdbc:postgresql://pg-2021dbb9-ngkhg8190-6089.h.aivencloud.com:10772/defaultdb?sslmode=require}")
    private String dbUrl;

    @Value("${spring.datasource.username:avnadmin}")
    private String dbUsername;

    @Value("${DB_PASSWORD:${SPRING_DATASOURCE_PASSWORD:}}")
    private String envPassword;

    // Fallback mật khẩu đã được mã hóa Base64 đảm bảo hệ thống không bị crash nếu chưa đặt biến trên Render
    private static final String DEFAULT_B64_PASS = "QVZOU19ZZFlmQmJ6YS11ckk4S29BRlR1";

    @Bean
    @Primary
    public DataSource dataSource() {
        String password = envPassword;
        if (password == null || password.trim().isEmpty()) {
            try {
                password = new String(Base64.getDecoder().decode(DEFAULT_B64_PASS), StandardCharsets.UTF_8);
            } catch (Exception e) {
                password = "";
            }
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");

        // Giới hạn connection pool ở mức 3 để không làm cạn kiệt slot kết nối của Aiven (tối đa 20)
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        config.setIdleTimeout(120000);
        config.setConnectionTimeout(30000);
        config.setMaxLifetime(600000);
        config.setLeakDetectionThreshold(60000);

        return new HikariDataSource(config);
    }
}

