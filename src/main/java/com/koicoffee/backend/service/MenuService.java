package com.koicoffee.backend.service;

import com.koicoffee.backend.config.CacheConfig;
import com.koicoffee.backend.model.Category;
import com.koicoffee.backend.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuService {

    private final CategoryService categoryService;
    private final ProductService productService;

    @Cacheable(value = CacheConfig.CACHE_MENU, key = "'full'")
    public Map<String, Object> getFullMenuCached() {
        log.info("Fetching full menu from DATABASE (cache miss)...");
        List<Category> categories = categoryService.getAllCategoriesCached();
        List<Product> products = productService.getActiveProductsCached();

        Map<String, Object> data = new HashMap<>();
        data.put("categories", categories);
        data.put("products", products);
        return data;
    }
}
