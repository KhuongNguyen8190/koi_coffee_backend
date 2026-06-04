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

# Chạy dự án
./mvnw spring-boot:run
Kiểm tra (Ping test):
Server sẽ chạy mặc định tại cổng 8080. Mở trình duyệt và truy cập:
http://localhost:8080/api/ping

🔒 Tính năng nổi bật
API CRUD Sản phẩm, Danh mục, Lịch sử đơn hàng, Quản lý Nhân sự.

Bảo mật Multi-login: Một tài khoản chỉ được phép đăng nhập trên 1 thiết bị/tab tại một thời điểm. Thiết bị cũ sẽ bị Kick-out qua WebSocket và phạt chờ 10s.

Thông báo (Notifications) theo thời gian thực (Real-time).
