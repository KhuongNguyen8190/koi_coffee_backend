package com.koicoffee.backend.config;

import com.koicoffee.backend.model.Category;
import com.koicoffee.backend.model.Product;
import com.koicoffee.backend.repository.CategoryRepository;
import com.koicoffee.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        // ĐỔI ĐIỀU KIỆN: Kiểm tra bảng SẢN PHẨM, nếu chưa có món nào thì ép buộc tạo!
        if (productRepository.count() == 0) {

            // Xóa sạch danh mục cũ (nếu có) để tạo lại từ đầu tránh lỗi trùng lặp
            categoryRepository.deleteAll();

            Category cat1 = new Category(); cat1.setName("Cà Phê Truyền Thống");
            Category cat2 = new Category(); cat2.setName("Trà Trái Cây");
            cat1 = categoryRepository.save(cat1);
            cat2 = categoryRepository.save(cat2);

            Product p1 = new Product();
            p1.setCategory(cat1); // Dùng trực tiếp Object Category sẽ chuẩn hơn dùng ID
            p1.setName("Cà Phê Sữa Đá");
            p1.setPrice(29000);
            p1.setSku("CF-SUADA");
            p1.setStatus("active");
            productRepository.save(p1);

            Product p2 = new Product();
            p2.setCategory(cat2);
            p2.setName("Trà Đào Cam Sả");
            p2.setPrice(39000);
            p2.setSku("TEA-DAOCAMSA");
            p2.setStatus("active");
            productRepository.save(p2);

            System.out.println("✅ Đã đổ dữ liệu mẫu Menu thành công!");
        }
    }
}