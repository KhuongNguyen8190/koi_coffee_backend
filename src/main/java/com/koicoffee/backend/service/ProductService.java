package com.koicoffee.backend.service;

import com.koicoffee.backend.config.CacheConfig;
import com.koicoffee.backend.model.Category;
import com.koicoffee.backend.model.Product;
import com.koicoffee.backend.repository.CategoryRepository;
import com.koicoffee.backend.repository.ProductRepository;
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
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Cacheable(value = CacheConfig.CACHE_PRODUCTS, key = "'active'")
    public List<Product> getActiveProductsCached() {
        log.info("Fetching active products from DATABASE (cache miss)...");
        return productRepository.findByStatusOrderByNameAsc("active");
    }

    @Cacheable(value = CacheConfig.CACHE_PRODUCTS, key = "'all'")
    public List<Product> getAllProductsCached() {
        log.info("Fetching all products from DATABASE (cache miss)...");
        return productRepository.findAll();
    }

    public Page<Product> searchProductsPaged(String status, Long categoryId, String search, Pageable pageable) {
        log.debug("Fetching paged products from DATABASE (page={}, size={}, status={}, catId={}, search={})",
                pageable.getPageNumber(), pageable.getPageSize(), status, categoryId, search);
        return productRepository.searchProducts(status, categoryId, search, pageable);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại với ID: " + id));
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_PRODUCTS, CacheConfig.CACHE_MENU}, allEntries = true)
    public Product createProduct(String name, int price, String status, Long categoryId, String sku) {
        log.info("Creating product '{}' and evicting caches: {}, {}", name, CacheConfig.CACHE_PRODUCTS, CacheConfig.CACHE_MENU);
        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setStatus(status != null && !status.isEmpty() ? status : "active");
        product.setSku(sku);

        if (categoryId != null) {
            Category cat = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + categoryId));
            product.setCategory(cat);
        }

        return productRepository.save(product);
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_PRODUCTS, CacheConfig.CACHE_MENU}, allEntries = true)
    public Product updateProduct(Long id, String name, Integer price, String status, Long categoryId, String sku) {
        log.info("Updating product ID {} and evicting caches", id);
        Product product = getProductById(id);

        if (name != null) product.setName(name);
        if (price != null) product.setPrice(price);
        if (status != null) product.setStatus(status);
        if (sku != null) product.setSku(sku);

        if (categoryId != null) {
            Category cat = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với ID: " + categoryId));
            product.setCategory(cat);
        }

        return productRepository.save(product);
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_PRODUCTS, CacheConfig.CACHE_MENU}, allEntries = true)
    public void deleteProduct(Long id) {
        log.info("Deleting product ID {} and evicting caches", id);
        Product product = getProductById(id);
        productRepository.delete(product);
    }
}
