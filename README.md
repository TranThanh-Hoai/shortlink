# Shortlink - Dịch vụ Rút gọn Liên kết (URL Shortener)

[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Redis](https://img.shields.io/badge/Redis-7.x-red.svg)](https://redis.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Shortlink là một hệ thống rút gọn URL hiệu năng cao, được xây dựng trên nền tảng Spring Boot 4. Ứng dụng tích hợp các công nghệ hiện đại như Snowflake ID, Redis Caching để đảm bảo khả năng mở rộng và độ tin cậy.

## 🚀 Tính năng chính

- **Rút gọn URL thông minh**: Sử dụng thuật toán Base62 kết hợp với Snowflake ID để tạo mã rút gọn duy nhất và ngắn gọn.
- **Bí danh tùy chỉnh (Custom Alias)**: Cho phép người dùng cá nhân hóa các đường dẫn rút gọn.
- **Phân tích & Thống kê (Analytics)**: Theo dõi lượt click, ghi lại thông tin User-Agent và IP (đang phát triển) để phân tích hành vi.
- **Hiệu năng cao**: Tối ưu hóa tốc độ truy xuất bằng Redis Caching, đảm bảo độ trễ cực thấp cho các yêu cầu chuyển hướng.
- **Giới hạn tốc độ (Rate Limiting)**: Bảo vệ hệ thống khỏi các cuộc tấn công Brute-force và Spam bằng Bucket4j + Redis.
- **Bảo mật JWT**: Hệ thống xác thực và phân quyền dựa trên Token mạnh mẽ.

## 🏗 Kiến trúc hệ thống

Dự án sử dụng phương pháp tạo ID phân tán **Snowflake** để đảm bảo:
1. **Tính duy nhất**: Không trùng lặp ID trong môi trường phân tán.
2. **Hiệu năng**: Tạo ID cực nhanh mà không cần truy vấn Database.
3. **Thứ tự**: ID được tạo ra có tính chất tăng dần theo thời gian.

Mã ID sau đó được mã hóa sang **Base62** (gồm ký tự `0-9`, `a-z`, `A-Z`) để tạo ra các Short Code ngắn nhất có thể.

## 🛠 Công nghệ sử dụng

- **Backend**: Java 21, Spring Boot 4.0.6
- **Persistence**: Spring Data JPA, Hibernate
- **Database**: MySQL 8 / PostgreSQL 16
- **Caching & Rate Limit**: Redis, Bucket4j
- **Security**: Spring Security, JWT (jjwt)
- **API Documentation**: Springdoc OpenAPI (Swagger UI)
- **Containerization**: Docker, Docker Compose

## 📋 Yêu cầu hệ thống

- **Java 21** trở lên
- **Maven 3.9+**
- **Docker & Docker Compose** (khuyến nghị để chạy full stack)

## ⚙️ Cấu hình nhanh

Cấu hình mặc định nằm trong `src/main/resources/application.properties`. Bạn nên tạo file `.env` dựa trên [`.env.example`](file:///d:/Languges/java/shortlink/.env.example) để tùy chỉnh các tham số:

```properties
# Snowflake configuration
snowflake.worker-id=1
snowflake.datacenter-id=1

# JWT configuration
jwt.secret=YourSuperSecretKeyForJWTTokenGeneration
jwt.expiration=3600000
```

## 🏃 Hướng dẫn chạy ứng dụng

### Sử dụng Docker Compose (Khuyên dùng)

Lệnh này sẽ khởi động hệ thống bao gồm: App, PostgreSQL và Redis.

```bash
docker-compose up -d
```

### Chạy thủ công bằng Maven

1. Khởi động MySQL và Redis cục bộ.
2. Build và chạy:
```bash
mvn clean install
mvn spring-boot:run
```

## 📖 Tài liệu API

| Dịch vụ | URL | Ghi chú |
| :--- | :--- | :--- |
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` | Thử nghiệm API trực tiếp |
| **Actuator** | `http://localhost:8080/actuator` | Kiểm tra sức khỏe hệ thống |

## 📖 API Endpoints chính

- `POST /api/shorten`: Tạo link rút gọn.
- `GET /{shortCode}`: Chuyển hướng đến URL gốc.
- `GET /api/clicks/{linkId}`: Xem thống kê lượt click (hỗ trợ phân trang).

## 🤝 Đóng góp

Mọi đóng góp nhằm cải thiện hiệu năng hoặc thêm tính năng mới đều được hoan nghênh. Vui lòng mở Issue hoặc tạo PR.

---
✨ Phát triển bởi [TranThanh-Hoai](https://github.com/TranThanh-Hoai)

