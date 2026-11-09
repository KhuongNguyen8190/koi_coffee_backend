package com.koicoffee.backend.service;

import com.koicoffee.backend.config.CacheConfig;
import com.koicoffee.backend.model.Category;
import com.koicoffee.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Cacheable(value = CacheConfig.CACHE_CATEGORIES, key = "'all'")
    public List<Category> getAllCategoriesCached() {
        log.info("Fetching categories from DATABASE (cache miss)...");
        return categoryRepository.findAllByOrderByNameAsc();
    }

    public Page<Category> getCategoriesPaged(String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return categoryRepository.findByNameContainingIgnoreCase(search.trim(), pageable);
        }
        return categoryRepository.findAll(pageable);
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + id));
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_CATEGORIES, CacheConfig.CACHE_MENU}, allEntries = true)
    public Category createCategory(String name) {
        log.info("Creating category '{}' and evicting caches: {}, {}", name, CacheConfig.CACHE_CATEGORIES, CacheConfig.CACHE_MENU);
        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_CATEGORIES, CacheConfig.CACHE_MENU}, allEntries = true)
    public Category updateCategory(Long id, String name) {
        log.info("Updating category ID {} and evicting caches", id);
        Category category = getCategoryById(id);
        category.setName(name);
        return categoryRepository.save(category);
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_CATEGORIES, CacheConfig.CACHE_MENU}, allEntries = true)
    public void deleteCategory(Long id) {
        log.info("Deleting category ID {} and evicting caches", id);
        if (!categoryRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy danh mục với ID: " + id);
        }
        categoryRepository.deleteById(id);
    }
}

