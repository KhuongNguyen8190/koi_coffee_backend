# ☕ KOI Coffee POS - Backend API

Đây là hệ thống Backend cho dự án phần mềm quản lý bán hàng (POS) của quán KOI Coffee, được xây dựng bằng **Java Spring Boot**. Hệ thống cung cấp API cho việc quản lý đơn hàng, người dùng, sản phẩm, và sử dụng **WebSocket** để đồng bộ dữ liệu thời gian thực giữa các thiết bị.

## 🚀 Công nghệ sử dụng
- **Framework:** Java Spring Boot
- **Database:** MySQL / PostgreSQL
- **Security:** BCrypt Password Encoder
- **Real-time:** WebSockets (STOMP)
- **Deployment:** Render (hoặc môi trường Docker/Java tương đương)

## 🛠️ Yêu cầu hệ thống (Prerequisites)
- [JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) hoặc mới hơn.
- [Maven](https://maven.apache.org/) (hoặc sử dụng Maven Wrapper `mvnw` đi kèm).
- Hệ quản trị CSDL MySQL hoặc PostgreSQL.

## ⚙️ Cài đặt & Chạy dự án (Local Development)

1. **Clone repository:**
   ```bash
   git clone [https://github.com/your-username/koi-coffee-backend.git](https://github.com/your-username/koi-coffee-backend.git)
   cd koi-coffee-backend
Cấu hình Database:
Mở file src/main/resources/application.properties (hoặc application.yml) và thay đổi thông tin kết nối Database của bạn:

Properties
spring.datasource.url=jdbc:mysql://localhost:3306/koi_coffee_db
spring.datasource.username=root
spring.datasource.password=mat_khau_cua_ban
spring.jpa.hibernate.ddl-auto=update
Build và Chạy ứng dụng:
Chạy lệnh Maven Wrapper tại thư mục gốc:

Bash
# Build dự án
./mvnw clean install -DskipTests

# PosgreSQL Script
-- =========================================================================
-- PHẦN 1: DỌN DẸP BẢNG CŨ (NẾU CÓ) ĐỂ TRÁNH LỖI KHI CHẠY LẠI
-- =========================================================================
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS order_details CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS shifts CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS cash_registers CASCADE;

-- =========================================================================
-- PHẦN 2: TẠO CẤU TRÚC CÁC BẢNG (TABLES)
-- =========================================================================

-- 1. Bảng tài khoản người dùng
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng danh mục món
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

-- 3. Bảng sản phẩm (món ăn/thức uống/dịch vụ)
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price INTEGER NOT NULL,
    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    status VARCHAR(50) DEFAULT 'ACTIVE'
);

-- 4. Bảng hóa đơn
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    note TEXT,
    parent_order_id BIGINT,
    payment_method VARCHAR(50),
    payment_time TIMESTAMP,
    staff_name VARCHAR(100),
    status VARCHAR(50) DEFAULT 'PENDING',
    total_price INTEGER NOT NULL,
    discount INTEGER DEFAULT 0
);

-- 5. Bảng chi tiết hóa đơn (Các món trong 1 bill)
CREATE TABLE order_details (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES products(id) ON DELETE SET NULL,
    quantity INTEGER NOT NULL,
    price INTEGER NOT NULL,
    note TEXT
);

-- 6. Bảng Két tiền (Lưu số dư tiền mặt duy nhất của quán)
CREATE TABLE cash_registers (
    id BIGINT PRIMARY KEY,
    balance INTEGER DEFAULT 0,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. Bảng Lịch sử chốt ca (ĐÃ UPDATE CHO CÁCH QUẢN LÝ CHẶT CHẼ)
CREATE TABLE shifts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL, -- Liên kết với tài khoản nhân viên
    staff_name VARCHAR(100),
    status VARCHAR(20) DEFAULT 'OPEN', -- Trạng thái: OPEN (Đang mở ca) hoặc CLOSED (Đã chốt)
    initial_cash INTEGER DEFAULT 0,
    batch_cash_revenue INTEGER DEFAULT 0,
    transfer_revenue INTEGER DEFAULT 0,
    actual_cash INTEGER DEFAULT 0,
    variance INTEGER DEFAULT 0,
    total_revenue INTEGER DEFAULT 0,
    note TEXT,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- Giờ mở ca (Tự động lấy)
    end_time TIMESTAMP -- Giờ chốt ca (Chỉ cập nhật khi kết ca)
);

-- 8. Bảng Thông báo
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE -- Nếu NULL nghĩa là thông báo chung
);

-- =========================================================================
-- PHẦN 3: NẠP DỮ LIỆU MẪU VÀ THỰC TẾ (INSERT DATA)
-- =========================================================================

-- 1. Khởi tạo Két tiền ban đầu
INSERT INTO cash_registers (id, balance, last_updated) VALUES 
(1, 0, CURRENT_TIMESTAMP);

-- 2. Nạp 2 Tài khoản hệ thống (Mật khẩu: 12345)
INSERT INTO users (username, password, full_name, role) VALUES 
('admin', '$2a$10$ccHf5tJphjzUSWIqbxTq7u8RFKayRbUGQL5srn.YurpC2BI6NH3lC', 'Quản Trị Viên Hệ Thống', 'ADMIN'),
('staff1', '$2a$10$ccHf5tJphjzUSWIqbxTq7u8RFKayRbUGQL5srn.YurpC2BI6NH3lC', 'Nguyễn Nhân Viên Thu Ngân', 'STAFF');

-- 3. Nạp Danh mục Menu
INSERT INTO categories (id, name) VALUES 
(1, 'Coffee'), (2, 'Trà'), (3, 'Nước ép'), (4, 'Yaourt'), 
(5, 'Nước ngọt'), (6, 'Trà sữa'), (7, 'Soda'), (8, 'Thuốc lá');
SELECT setval(pg_get_serial_sequence('categories', 'id'), coalesce(max(id), 0) + 1, false) FROM categories;

-- 4. Nạp Chi tiết món ăn & giá
INSERT INTO products (name, price, category_id, status) VALUES 
('Cafe đen', 23000, 1, 'ACTIVE'), ('Cafe sữa', 28000, 1, 'ACTIVE'), ('Bạc xỉu', 28000, 1, 'ACTIVE'), ('Cacao', 30000, 1, 'ACTIVE'), ('Sữa tươi cafe', 30000, 1, 'ACTIVE'), ('Matcha latte', 30000, 1, 'ACTIVE'),
('Trà tắc', 20000, 2, 'ACTIVE'), ('Trà vải', 30000, 2, 'ACTIVE'), ('Trà dâu', 30000, 2, 'ACTIVE'), ('Trà xoài', 30000, 2, 'ACTIVE'), ('Trà việt quất', 30000, 2, 'ACTIVE'), ('Trà đào', 30000, 2, 'ACTIVE'), ('Trà kiwi', 30000, 2, 'ACTIVE'), ('Trà ổi hồng', 30000, 2, 'ACTIVE'), ('Trà chanh', 20000, 2, 'ACTIVE'), ('Trà gừng nóng', 20000, 2, 'ACTIVE'), ('Trà lipton', 20000, 2, 'ACTIVE'), ('Trà đào chanh xí muội', 30000, 2, 'ACTIVE'), ('Hồng trà trân châu trắng', 22000, 2, 'ACTIVE'),
('Ép thơm', 30000, 3, 'ACTIVE'), ('Ép táo', 30000, 3, 'ACTIVE'), ('Ép dưa hấu', 30000, 3, 'ACTIVE'), ('Ép ổi', 30000, 3, 'ACTIVE'), ('Ép cam', 25000, 3, 'ACTIVE'),
('Yaourt đào', 35000, 4, 'ACTIVE'), ('Yaourt xoài', 35000, 4, 'ACTIVE'), ('Yaourt việt quất', 35000, 4, 'ACTIVE'), ('Yaourt dâu', 35000, 4, 'ACTIVE'), ('Yaourt hủ', 12000, 4, 'ACTIVE'), ('Yaourt truyền thống', 22000, 4, 'ACTIVE'),
('Coca/ coca plus', 23000, 5, 'ACTIVE'), ('Thums up', 23000, 5, 'ACTIVE'), ('Sting', 23000, 5, 'ACTIVE'), ('Redbull', 25000, 5, 'ACTIVE'), ('Lipovitan', 23000, 5, 'ACTIVE'), ('Samurai', 23000, 5, 'ACTIVE'), ('Nuti', 23000, 5, 'ACTIVE'), ('Olong', 23000, 5, 'ACTIVE'), ('Nước ngọt các loại', 23000, 5, 'ACTIVE'), ('Dừa trái', 25000, 5, 'ACTIVE'), ('Nước suối', 10000, 5, 'ACTIVE'),
('Trà sữa truyền thống', 30000, 6, 'ACTIVE'), ('Trà sữa khoai môn', 30000, 6, 'ACTIVE'), ('Topping thêm', 5000, 6, 'ACTIVE'),
('Soda ổi xí muội', 35000, 7, 'ACTIVE'), ('Soda kiwi', 35000, 7, 'ACTIVE'), ('Soda chanh dây', 35000, 7, 'ACTIVE'), ('Soda việt quất', 35000, 7, 'ACTIVE'), ('Soda dâu xí muội', 35000, 7, 'ACTIVE'), ('Soda chanh', 25000, 7, 'ACTIVE'),
('Sài gòn bạc', 20000, 8, 'ACTIVE'), ('Mèo demi', 20000, 8, 'ACTIVE'), ('Mèo lớn', 30000, 8, 'ACTIVE'), ('555 việt', 40000, 8, 'ACTIVE');

# Chạy dự án
./mvnw spring-boot:run
Kiểm tra (Ping test):
Server sẽ chạy mặc định tại cổng 8080. Mở trình duyệt và truy cập:
http://localhost:8080/api/ping

🔒 Tính năng nổi bật
API CRUD Sản phẩm, Danh mục, Lịch sử đơn hàng, Quản lý Nhân sự.

Bảo mật Multi-login: Một tài khoản chỉ được phép đăng nhập trên 1 thiết bị/tab tại một thời điểm. Thiết bị cũ sẽ bị Kick-out qua WebSocket và phạt chờ 10s.

Thông báo (Notifications) theo thời gian thực (Real-time).
