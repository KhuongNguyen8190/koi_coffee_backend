# Sử dụng môi trường Java 17 (Nếu project của bạn dùng Java 21, hãy đổi số 17 thành 21)
FROM eclipse-temurin:17-jdk-alpine

# Tạo thư mục làm việc bên trong máy chủ
WORKDIR /app

# Copy toàn bộ code từ GitHub vào máy chủ
COPY . .

# Cấp quyền thực thi cho Maven Wrapper (Khắc phục triệt để lỗi Permission Denied)
RUN chmod +x mvnw

# Build project thành file .jar và bỏ qua bước test
RUN ./mvnw clean package -DskipTests

# Khởi chạy ứng dụng (Nếu tên file .jar trong thư mục target của bạn khác, hãy sửa lại dòng này cho khớp)
CMD ["java", "-jar", "target/koi-coffee-backend-0.0.1-SNAPSHOT.jar"]