# Shortlink - Dịch vụ Rút gọn Liên kết (URL Shortener)

[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Shortlink là một ứng dụng backend mạnh mẽ được xây dựng bằng Spring Boot, cung cấp dịch vụ rút gọn URL với các tính năng nâng cao như đặt bí danh tùy chỉnh (custom alias), giới hạn tốc độ (rate limiting), bộ nhớ đệm (caching) và bảo mật bằng JWT.

## 🚀 Tính năng chính

- **Rút gọn URL**: Chuyển đổi các liên kết dài thành các liên kết ngắn gọn, dễ chia sẻ.
- **Bí danh tùy chỉnh (Custom Alias)**: Cho phép người dùng tự đặt tên cho các liên kết rút gọn.
- **Quản lý hết hạn**: Hỗ trợ thiết lập thời gian hết hạn cho các liên kết và tự động dọn dẹp các liên kết đã hết hạn.
- **Bảo mật JWT**: Hệ thống xác thực và phân quyền người dùng sử dụng JSON Web Token.
- **Giới hạn tốc độ (Rate Limiting)**: Sử dụng Bucket4j kết hợp với Redis để ngăn chặn việc lạm dụng API.
- **Bộ nhớ đệm (Caching)**: Tối ưu hóa hiệu năng truy xuất link bằng Redis.
- **Tài liệu API tự động**: Tích hợp Swagger UI (OpenAPI 3) để dễ dàng thử nghiệm và tích hợp.
- **Giám sát (Monitoring)**: Hỗ trợ Prometheus và Grafana để theo dõi trạng thái hệ thống.

## 🛠 Công nghệ sử dụng

- **Ngôn ngữ**: Java 21
- **Framework chính**: Spring Boot
- **Cơ sở dữ liệu**: MySQL (Lưu trữ chính), Redis (Caching & Rate Limiting)
- **Bảo mật**: Spring Security, JWT (jjwt)
- **Tài liệu API**: Springdoc OpenAPI
- **Quản lý thư viện**: Maven
- **Khác**: Lombok, Bucket4j, Docker & Docker Compose

## 📋 Yêu cầu hệ thống

- Java 21+
- Maven 3.6+
- MySQL 8.0+
- Redis 7.0+
- Docker (Tùy chọn)

## ⚙️ Cấu hình

Trước khi chạy ứng dụng, hãy cập nhật các thông số cấu hình trong file `src/main/resources/application.properties`:

```properties
# Database configuration
spring.datasource.url=jdbc:mysql://localhost:3306/shortlink
spring.datasource.username=your_username
spring.datasource.password=your_password

# Redis configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379

# JWT configuration
jwt.secret=your_very_long_secret_key_here
jwt.expiration=3600000
```

## 🏃 Hướng dẫn chạy ứng dụng

### Chạy bằng Maven

1. Clone repository:
   ```bash
   git clone https://github.com/TranThanh-Hoai/shortlink.git
   cd shortlink
   ```

2. Build ứng dụng:
   ```bash
   mvn clean install
   ```

3. Chạy ứng dụng:
   ```bash
   mvn spring-boot:run
   ```

### Chạy bằng Docker

Ứng dụng đã được cấu hình sẵn Docker Compose bao gồm: App, PostgreSQL (trong compose.yaml), Redis, Prometheus và Grafana.

```bash
docker-compose up -d
```

> **Lưu ý**: Hiện tại file `compose.yaml` đang sử dụng PostgreSQL làm DB, trong khi `pom.xml` sử dụng MySQL. Bạn có thể cần điều chỉnh để đồng bộ.

## 📖 Tài liệu API

Sau khi ứng dụng khởi chạy, bạn có thể truy cập tài liệu API tương tác tại:

- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## 🤝 Đóng góp

Mọi đóng góp đều được trân trọng! Vui lòng tạo Issue hoặc gửi Pull Request nếu bạn có ý tưởng cải thiện dự án.

## 📄 Giấy phép

Dự án này được phân phối dưới giấy phép MIT.

---
✨ Phát triển bởi [TranThanh-Hoai](https://github.com/TranThanh-Hoai)
