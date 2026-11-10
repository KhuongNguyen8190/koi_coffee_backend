package com.koicoffee.backend.config;

import com.koicoffee.backend.model.CashRegister;
import com.koicoffee.backend.model.Category;
import com.koicoffee.backend.model.Product;
import com.koicoffee.backend.model.User;
import com.koicoffee.backend.repository.CashRegisterRepository;
import com.koicoffee.backend.repository.CategoryRepository;
import com.koicoffee.backend.repository.ProductRepository;
import com.koicoffee.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CashRegisterRepository cashRegisterRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Khởi tạo tài khoản Quản trị viên (admin) nếu chưa có
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("12345"));
            admin.setFullName("Quản Trị Viên Hệ Thống");
            admin.setRole("ADMIN");
            admin.setIsActive(true);
            admin.setCreatedAt(LocalDateTime.now());
            userRepository.save(admin);
            System.out.println("✅ [DataSeeder] Đã khởi tạo tài khoản ADMIN mặc định: admin / 12345");
        }

        // 2. Khởi tạo tài khoản Thu ngân (staff1) nếu chưa có
        if (userRepository.findByUsername("staff1").isEmpty()) {
            User staff = new User();
            staff.setUsername("staff1");
            staff.setPassword(passwordEncoder.encode("12345"));
            staff.setFullName("Nguyễn Nhân Viên Thu Ngân");
            staff.setRole("STAFF");
            staff.setIsActive(true);
            staff.setCreatedAt(LocalDateTime.now());
            userRepository.save(staff);
            System.out.println("✅ [DataSeeder] Đã khởi tạo tài khoản STAFF mặc định: staff1 / 12345");
        }

        // 3. Khởi tạo Két tiền ban đầu nếu chưa có
        if (cashRegisterRepository.findById(1L).isEmpty()) {
            CashRegister register = new CashRegister();
            register.setId(1L);
            register.setBalance(0L);
            register.setLastUpdated(LocalDateTime.now());
            cashRegisterRepository.save(register);
            System.out.println("✅ [DataSeeder] Đã khởi tạo Két tiền CashRegister ID=1");
        }

        // 4. Khởi tạo Menu & Danh mục nếu database chưa có sản phẩm nào
        if (productRepository.count() == 0) {
            // Dọn dẹp danh mục mồ côi nếu có
            categoryRepository.deleteAll();

            Category catCoffee = new Category();
            catCoffee.setName("Cà Phê Truyền Thống");
            catCoffee = categoryRepository.save(catCoffee);

            Category catTea = new Category();
            catTea.setName("Trà Trái Cây & Trà Sữa");
            catTea = categoryRepository.save(catTea);

            Category catIce = new Category();
            catIce.setName("Đá Xay & Sinh Tố");
            catIce = categoryRepository.save(catIce);

            Category catSnack = new Category();
            catSnack.setName("Bánh & Ăn Kèm");
            catSnack = categoryRepository.save(catSnack);

            createProduct(catCoffee, "Cà Phê Sữa Đá", 29000, "CF-SUADA");
            createProduct(catCoffee, "Cà Phê Đen Đá", 25000, "CF-DENDA");
            createProduct(catCoffee, "Bạc Xỉu Sữa Tươi", 32000, "CF-BACXIU");

            createProduct(catTea, "Trà Đào Cam Sả", 39000, "TEA-DAOCAMSA");
            createProduct(catTea, "Trà Vải Hoa Hồng", 39000, "TEA-VAI");
            createProduct(catTea, "Trà Sữa Koi Đặc Biệt", 42000, "TEA-MILK");

            createProduct(catIce, "Matcha Đá Xay", 45000, "ICE-MATCHA");
            createProduct(catIce, "Cà Phê Cốt Dừa Đá Xay", 45000, "ICE-COCOCF");

            createProduct(catSnack, "Bánh Mì Que Hải Phòng", 15000, "SNACK-BMQUE");
            createProduct(catSnack, "Croissant Bơ Tỏi", 28000, "SNACK-CROISSANT");

            System.out.println("✅ [DataSeeder] Đã khởi tạo dữ liệu mẫu Menu thành công!");
        }
    }

    private void createProduct(Category category, String name, Integer price, String sku) {
        Product p = new Product();
        p.setCategory(category);
        p.setName(name);
        p.setPrice(price);
        p.setSku(sku);
        p.setStatus("active");
        productRepository.save(p);
    }
}
