# ⚽ Soccer365 - Football Pitch Management System (FPMS)

> Hệ thống quản lý và đặt sân bóng mini trực tuyến toàn diện, xây dựng trên nền tảng **Spring Boot 3 (Java 21)** và **React 19 (TypeScript + Vite)**.

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6.0-blue.svg)](https://www.typescriptlang.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-v4-38bdf8.svg)](https://tailwindcss.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## 📌 Giới thiệu dự án

**Soccer365 (Football Pitch Management System - FPMS)** là giải pháp phần mềm toàn diện giúp kết nối người đam mê bóng đá và các chủ sân bóng. Hệ thống tự động hóa toàn bộ quy trình từ tra cứu sân trống theo khung giờ, đặt sân, thanh toán, đến quản lý doanh thu và phân quyền nhân viên trên giao diện hiện đại, mượt mà.

---

## 🚀 Công nghệ sử dụng (Tech Stack)

### 🖥️ Backend (RESTful API)
- **Ngôn ngữ & Framework**: Java 21 (LTS), Spring Boot 3.x
- **Bảo mật & Phân quyền**: Spring Security 6, JWT (JSON Web Token - `io.jsonwebtoken 0.12.6`), Role-Based Access Control (RBAC)
- **Bộ nhớ đệm & Thu hồi Token**: Spring Data Redis (Redis 7 - Token Blacklist khi Logout)
- **Cơ sở dữ liệu & ORM**: PostgreSQL 16, Spring Data JPA, Hibernate ORM
- **Email & OTP**: Spring Boot Starter Mail, Brevo (Sendinblue) SMTP Relay
- **Xác thực mạng xã hội**: Google OAuth2 Client (`google-api-client`)
- **Tài liệu hóa API**: SpringDoc OpenAPI 3, Swagger UI
- **Mapping & Utilities**: MapStruct 1.6, Project Lombok, Bean Validation (`Hibernate Validator`)
- **Kiểm thử**: JUnit 5, Mockito, Spring Boot Starter Test

### 💻 Frontend (Single Page Application)
- **Framework & Build Tool**: React 19, TypeScript, Vite 8
- **Styling & Giao diện**: Tailwind CSS v4, Lucide React (Icons), SweetAlert2
- **Định tuyến**: React Router DOM v7
- **HTTP Client**: Axios (với Request & Response Interceptors tập trung)

### 🐳 Hạ tầng & Môi trường
- **Containerization**: Docker & Docker Compose (PostgreSQL 16, Redis 7)

---

## 🌟 Các tính năng nổi bật

### 1. 🔐 Phân hệ Xác thực & Bảo mật (Authentication & Security)
- **Đăng ký tài khoản**: Xác thực mật khẩu khớp, kiểm tra trùng lặp email/SĐT, kiểm tra định dạng số điện thoại chuẩn Việt Nam (10 số, đầu số hợp lệ `03, 05, 07, 08, 09`).
- **Đăng nhập linh hoạt**: Đăng nhập bằng Email hoặc Số điện thoại. Hỗ trợ cờ "Ghi nhớ đăng nhập" (Remember Me) lưu trữ thông minh giữa `localStorage` và `sessionStorage`.
- **Đăng nhập Google OAuth2**: Đăng nhập một chạm với Google ID Token, tự động đồng bộ tài khoản và avatar.
- **Khôi phục mật khẩu qua Email OTP**:
  - Gửi mã OTP 6 số qua email thật (Brevo SMTP).
  - Cơ chế **Rate Limiting / Spam Protection**: Cooldown 60 giây giữa các lần gửi mã.
  - Hạn dùng mã OTP 10 phút, tự vô hiệu hóa sau khi sử dụng thành công.
- **Đăng xuất an toàn (Secure Logout with Redis Blacklist)**:
  - Gửi `LogoutRequest` thu hồi JWT token, đưa vào Redis Blacklist với TTL tự động giải phóng bộ nhớ khi token hết hạn.
  - Vô hiệu hóa ngay lập tức mọi token cũ qua Postman hay cURL sau khi người dùng đã đăng xuất.
- **Hồ sơ cá nhân (User Profile)**: Xem và chỉnh sửa thông tin cá nhân, cập nhật số điện thoại, đổi mật khẩu an toàn.

### 2. ⚽ Quản lý Sân bóng & Ca đá (Pitch & Timeslot Management)
- Phân loại sân bóng (Sân 5 người, Sân 7 người, Sân 11 người).
- Bảng biểu thời gian ca đá (Timeline view trực quan), hiển thị tình trạng sân theo thời gian thực (Trống, Đã đặt, Đang sử dụng, Bảo trì).

### 3. 📅 Đặt sân & Chống trùng lịch (Booking Engine)
- Đặt sân theo khung giờ linh hoạt, tự động tính toán tổng tiền dựa theo khung giờ và loại sân.
- Cơ chế kiểm tra chống xung đột lịch đặt (Double-booking protection).

### 4. 👥 Phân quyền người dùng (Role-Based Access Control)
- `ROLE_CUSTOMER`: Tìm kiếm sân, đặt sân, xem lịch sử đặt chỗ, quản lý hồ sơ cá nhân.
- `ROLE_STAFF`: Quản lý ca đá tại sân, check-in khách nhận sân, quản lý đơn phát sinh tại chỗ.
- `ROLE_ADMIN`: Toàn quyền quản trị hệ thống, quản lý sân, khung giờ, người dùng, giao dịch và báo cáo doanh thu.

---

## 📁 Cấu trúc thư mục dự án

```text
football-pitch-management-system/
├── docker-compose.yml              # Cấu hình container PostgreSQL và Redis
├── README.md                       # Tài liệu tổng quan dự án
│
├── docs/                           # Kho tài liệu dự án đầy đủ
│   ├── DacTaUC/                    # Tài liệu đặc tả Use Case chi tiết
│   ├── development/                # Hướng dẫn kỹ thuật & Module Architecture
│   │   └── authentication-module/  # Thiết kế chi tiết Module Xác thực (01 -> 05)
│   ├── fe/                         # Tài liệu giao diện, Wireframe, UI/UX
│   ├── sql/                        # Kịch bản cơ sở dữ liệu mẫu & Init scripts
│   ├── srs/                        # Tài liệu yêu cầu phần mềm (Software Requirements)
│   └── workflow/                   # Quy trình nghiệp vụ và luồng hệ thống
│
├── backend/                        # Ứng dụng Spring Boot
│   ├── pom.xml                     # Maven dependencies cấu hình Java 21, Spring Boot 3
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/fpms/
│   │   │   │   ├── common/         # Response chuẩn (ApiResponse) & Constants
│   │   │   │   ├── config/         # Cấu hình Security, Swagger, Redis, WebMvc
│   │   │   │   ├── controller/     # REST Controllers (Auth, User, Pitch, Booking...)
│   │   │   │   ├── dto/            # Data Transfer Objects (Request / Response)
│   │   │   │   ├── entity/         # JPA Entities (User, Role, Pitch, Booking...)
│   │   │   │   ├── exception/      # GlobalExceptionHandler, ErrorCode, AppException
│   │   │   │   ├── mapper/         # MapStruct interfaces chuyển đổi DTO <-> Entity
│   │   │   │   ├── repository/     # Spring Data JPA Repositories
│   │   │   │   ├── security/       # JwtTokenProvider, JwtFilter, CustomUserDetailsService
│   │   │   │   └── service/        # Business Logic Interfaces & Implementations
│   │   │   └── resources/
│   │   │       ├── application.properties # Cấu hình DB, JWT, Brevo, Redis, Swagger
│   │   │       └── templates/      # Mẫu email HTML gửi OTP
│   │   └── test/                   # Unit Tests & Integration Tests (JUnit 5, Mockito)
│
└── frontend/                       # Ứng dụng React (TypeScript + Vite)
    ├── package.json                # Dependencies và NPM Scripts
    ├── vite.config.ts              # Cấu hình Vite bundler
    ├── tailwind.config.js          # Cấu hình giao diện Tailwind CSS
    └── src/
        ├── components/             # Reusable Components, Modals, Navbar, Sidebar
        ├── contexts/               # React Contexts (AuthContext quản lý phiên đăng nhập)
        ├── pages/                  # Các trang (Login, Register, ForgotPassword, Profile...)
        ├── services/               # API clients (axios instance, authService, pitchService)
        ├── types/                  # TypeScript interface definitions
        └── utils/                  # Hàm tiện ích (Toast, Format tiền tệ, Ngày tháng)
```

---

## ⚙️ Hướng dẫn cài đặt & Chạy ứng dụng (Quickstart)

### 📋 Yêu cầu môi trường
- **Java Development Kit (JDK)**: Phiên bản **21** trở lên
- **Node.js**: Phiên bản **18.x** trở lên & npm
- **Docker & Docker Desktop**: Để chạy PostgreSQL và Redis

---

### Bước 1: Khởi động Cơ sở dữ liệu và Redis qua Docker

Tại thư mục gốc của dự án, chạy lệnh:
```bash
docker compose up -d
```
Lệnh này sẽ khởi động 2 containers:
- **`football-postgres`**: PostgreSQL 16 tại cổng **`5433`** (được map ra port 5433 trên máy host để tránh xung đột với cổng PostgreSQL 5432 mặc định nếu bạn đã cài Postgres cục bộ).
  - Database: `fpms`
  - Username: `postgres`
  - Password: `postgres`
- **`football-redis`**: Redis 7 tại cổng **`6379`**.

---

### Bước 2: Cấu hình và Khởi chạy Backend (Spring Boot)

1. Di chuyển vào thư mục backend:
   ```bash
   cd backend
   ```
2. *(Tùy chọn)* Cấu hình tài khoản Brevo gửi email xác thực OTP (dùng chung 1 chuẩn REST API HTTPS 443 cho cả Local và Production/Render):
   ```properties
   BREVO_API_KEY=xkeysib-your-brevo-api-key-here
   app.mail.from-email=your-verified-email@example.com
   ```
   > 💡 **Lưu ý**: Lấy API Key tại Brevo Dashboard -> *SMTP & API* -> Tab *API keys & MCP* -> *Generate a new API key*. Dùng chuẩn HTTPS cổng 443 đảm bảo không bao giờ bị chặn trên Render.com hay bất kỳ nền tảng Cloud nào.
3. Chạy ứng dụng Spring Boot:
   - **Trên Windows (PowerShell / CMD)**:
     ```powershell
     ./mvnw spring-boot:run
     ```
   - **Trên Linux / macOS**:
     ```bash
     ./mvnw spring-boot:run
     ```
4. Backend sẽ khởi động tại: `http://localhost:8080`.
   - **Swagger UI (Tài liệu API)**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
   - **OpenAPI JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

### Bước 3: Cấu hình và Khởi chạy Frontend (React + Vite)

1. Mở một cửa sổ Terminal mới, di chuyển vào thư mục frontend:
   ```bash
   cd frontend
   ```
2. Sao chép file cấu hình môi trường:
   ```bash
   cp .env.example .env
   ```
   Nội dung file `.env`:
   ```env
   VITE_API_URL=http://localhost:8080/api
   VITE_GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
   ```
3. Cài đặt các gói phụ thuộc:
   ```bash
   npm install
   ```
4. Chạy Development Server:
   ```bash
   npm run dev
   ```
5. Mở trình duyệt và truy cập: [http://localhost:5173](http://localhost:5173).

---

## 🧪 Kiểm thử (Testing)

### Chạy Unit Tests Backend
Tại thư mục `backend`:
```bash
# Chạy toàn bộ test suite
./mvnw test

# Chạy riêng các bài test xác thực và người dùng
./mvnw test "-Dtest=AuthControllerTest,UserControllerTest"
```

### Chạy Kiểm tra Build Frontend
Tại thư mục `frontend`:
```bash
# Kiểm tra TypeScript type-check và đóng gói bundle
npm run build

# Chạy test suite của Frontend
npm run test
```