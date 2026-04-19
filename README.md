# 🔗 ShortLink - High Performance URL Shortener Service

[![Java Version](https://img.shields.io/badge/Java-21-blue?style=flat-square&logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

**ShortLink** là một giải pháp rút gọn URL (URL Shortener) mạnh mẽ, được tối ưu hóa cho hiệu suất cao, bảo mật và khả năng mở rộng. Dự án được xây dựng nhằm mục đích không chỉ giải quyết các bài toán CRUD cơ bản mà còn áp dụng các kỹ thuật tiên tiến trong phát triển backend hiện đại như **Multi-level Caching**, **Rate Limiting Distributed**, và **System Observability**.

---

## ✨ Tính năng nổi bật

### 🎯 Tính năng lõi
- **Rút gọn URL thông minh:** Hỗ trợ mã hóa ngẫu nhiên (base62) hoặc tùy chỉnh bí danh (custom alias).
- **QR Code Generator:** Tự động tạo mã QR chất lượng cao cho mỗi liên kết rút gọn.
- **Analytics thời gian thực:** Thu thập thông tin click (Browser, OS, Device, Location) một cách bất đồng bộ (`@Async`).

### ⚡ Hiệu suất & Độ tin cậy
- **Multi-level Caching Strategy:**
    - **L1 Cache (Caffeine):** Tốc độ cực nhanh cho các liên kết hot nhất ngay tại ứng dụng.
    - **L2 Cache (Redis):** Đồng bộ hóa dữ liệu giữa các instance và cache tập trung.
- **Idempotency API:** Bảo vệ API tạo link bằng `Idempotency-Key`, ngăn chặn tạo trùng lặp do lỗi mạng hoặc retry.
- **Fail-safe Design:** Quản lý vòng đời cache thông minh (Invalidation/Repopulation) khi dữ liệu gốc thay đổi.

### 🛡️ Bảo mật
- **Authentication & Authorization:** Hệ thống đăng nhập và phân quyền sử dụng **JWT (JSON Web Token)**.
- **Ownership Validation:** Đảm bảo người dùng chỉ có thể quản lý (xem, sửa, xóa) các liên kết do chính họ tạo ra.
- **Distributed Rate Limiting:** Sử dụng **Bucket4j + Redis** để bảo vệ hệ thống khỏi các cuộc tấn công Brute-force và Spam API.

### 📊 Khả năng quan sát (Observability)
- **Monitoring Dashboard:** Tích hợp bộ đôi **Prometheus** và **Grafana** để theo dõi sức khỏe hệ thống, throughput và latency.
- **Custom Metrics:** Theo dõi các chỉ số kinh doanh như tổng số click, tỷ lệ lỗi, và hiệu quả của hệ thống cache.

---

## 🛠 Tech Stack

| Category | Technologies |
| :--- | :--- |
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.5.x, Spring Data JPA, Spring Security |
| **Database** | PostgreSQL |
| **Caching** | Redis, Caffeine |
| **Security** | JWT, BCrypt, Bucket4j |
| **Monitoring** | Spring Actuator, Prometheus, Grafana |
| **Documentation** | Swagger / OpenAPI 3.0 |
| **Infrastructure** | Docker, Docker Compose |

---

## 📂 Cấu trúc thư mục tiêu biểu

```text
src/main/java/com/hoaitran/shortlink/
├── config/        # Cấu hình Security, Async, Cache, Swagger...
├── controller/    # REST API endpoints
├── dto/           # Data Transfer Objects & Validation
├── entity/        # JPA Entities (Database Mapping)
├── exception/     # Global Exception Handling
├── repository/    # Data Access Layer
├── service/       # Business Logic Layer
└── security/      # JWT Filter, Provider & Custom UserDetails
```

---

## 📊 Dashboard & Documentation

Sau khi chạy dự án, bạn có thể truy cập các địa chỉ sau:

| Dịch vụ | Địa chỉ | Ghi chú |
| :--- | :--- | :--- |
| **Swagger UI** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | Tài liệu & Test API |
| **Prometheus** | [http://localhost:9090](http://localhost:9090) | Thu thập Metrics |
| **Grafana** | [http://localhost:3000](http://localhost:3000) | Dashboard (admin/admin) |
| **Health Check** | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) | Trạng thái hệ thống |

---

## ⚙️ Hướng dẫn cài đặt

### Yêu cầu hệ thống
- **Docker & Docker Compose** (Khuyến nghị)
- **JDK 21** & **Maven 3.9+** (Nếu chạy không qua Docker)

### 1. Triển khai nhanh với Docker
```bash
# Clone dự án
git clone https://github.com/TranThanh-Hoai/shortlink.git
cd shortlink

# Khởi chạy toàn bộ Services (PostgreSQL, Redis, Prometheus, Grafana, App)
docker compose up -d --build
```

### 2. Chạy Local (Manual)
1. Cấu hình các thông số database trong `src/main/resources/application.yml`.
2. Chạy ứng dụng:
   ```bash
   ./mvnw spring-boot:run
   ```

---

## 🧪 Kiểm thử (Testing)

Dự án chú trọng vào chất lượng mã nguồn với bộ Integration Tests bao phủ các kịch bản quan trọng từ Logic nghiệp vụ đến Bảo mật:

```bash
# Chạy toàn bộ Test suite
./mvnw test
```

---

## 📈 Roadmap & Định hướng phát triển

- [ ] **Database Migration:** Tích hợp Flyway/Liquibase để quản lý phiên bản schema.
- [ ] **CI/CD Pipeline:** Tự động hóa build, test và deploy qua GitHub Actions.
- [ ] **Soft Delete:** Triển khai cơ chế xóa mềm kết hợp dọn dẹp dữ liệu định kỳ.
- [ ] **User Profile:** Bổ sung tính năng quản lý thông tin cá nhân và cài đặt bảo mật cho người dùng.

---

## 👤 Tác giả

**Tran Thanh Hoai**
- Email: [tthoai654@gmail.com](mailto:tthoai654@gmail.com)
- GitHub: [@TranThanh-Hoai](https://github.com/TranThanh-Hoai)

---

⭐ Nếu bạn thấy dự án này hữu ích, hãy tặng một ngôi sao (Star) trên GitHub nhé!