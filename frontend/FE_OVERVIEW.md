# Tổng quan cấu trúc và Tiến độ Frontend

Tài liệu này mô tả cấu trúc thư mục chuẩn của dự án Frontend và danh sách các công việc đã hoàn thành để khởi tạo base dự án.

## 1. Cấu trúc thư mục (`frontend/src`)

Dự án được chia module hóa để dễ dàng quản lý và mở rộng theo chuẩn:

```text
src/
├── assets/         # Hình ảnh, fonts, file tĩnh (logo.svg, global.css)
├── components/     # UI component dùng chung
│   ├── common/     # Các component nhỏ gọn, tái sử dụng 
│   └── layout/     # Các thành phần cấu trúc layout (Header, Footer, Sidebar...)
├── config/         # Cấu hình toàn cục (axios instance, môi trường)
├── constants/      # Các hằng số (API endpoints, roles, chuỗi tĩnh)
├── contexts/       # React Context (AuthContext, ThemeContext)
├── hooks/          # Custom hooks dùng chung (useAuth, useDebounce)
├── layouts/        # Bố cục chính của ứng dụng (CustomerLayout, AdminLayout)
├── pages/          # Các trang chính của ứng dụng
│   ├── Home.tsx    
│   ├── Login.tsx   
│   └── ...         # Các trang tính năng của Khách hàng và Admin
├── routes/         # Cấu hình router (định nghĩa các tuyến đường)
│   ├── index.tsx               # Root router provider
│   └── customer.routes.tsx     # Định tuyến cho phân hệ khách hàng
├── services/       # Xử lý gọi API (auth.service.ts, user.service.ts)
├── store/          # Quản lý state toàn cục (Redux, Zustand)
├── types/          # Định nghĩa TypeScript interfaces/types toàn cục
├── utils/          # Các hàm helper logic (formatDate, validator)
├── App.tsx         # Component gốc của ứng dụng cấu hình RouterProvider
├── index.css       # File CSS tổng quan toàn cục
└── main.tsx        # Điểm khởi chạy (entry point) của React
```

## 2. Các công việc đã hoàn thành

- **Cấu trúc thư mục:** Đã khởi tạo base thư mục chuẩn cho dự án (components, layouts, pages, routes, hooks, services, ...).
- **Định tuyến (Router):** Đã thiết lập xong `react-router-dom` v7+ (sử dụng `RouterProvider`) và chia tách thành công cấu hình định tuyến cho phân hệ khách hàng (`customer.routes.tsx`).
- **Giao diện Khách hàng (Customer Portal):** Đã di chuyển và tích hợp thành công layout chung (Header, Footer) cùng với trang Chủ (Home), trang Đăng nhập (Login) và đồng bộ toàn bộ CSS/Icons.
- **Tạo khung các trang tính năng:** Đã tạo sẵn các file page riêng biệt (`BookPitch`, `Register`, `ForgotPassword`, `Profile`, `MyBookings`, `Checkout`) và kết nối sẵn vào router, sẵn sàng để phát triển chi tiết.

---

*Hệ thống Frontend (Khách hàng) hiện tại đã chạy ổn định và sẵn sàng cho việc phát triển các chức năng chi tiết hoặc tích hợp API.*
