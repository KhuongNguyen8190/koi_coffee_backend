package com.koicoffee.backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_MENU = "menu";
    public static final String CACHE_CATEGORIES = "categories";
    public static final String CACHE_PRODUCTS = "products";
    public static final String CACHE_USERS = "users";

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(List.of(CACHE_MENU, CACHE_CATEGORIES, CACHE_PRODUCTS, CACHE_USERS));
        return cacheManager;
    }
}

