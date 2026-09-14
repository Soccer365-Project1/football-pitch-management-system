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
- **Giao diện Quản trị (Admin Portal):** Đã sao chép và thiết lập khung layout Admin (`AdminLayout`, `AdminSidebar`, `AdminTopbar`).
- **Tạo khung trang Admin:** Đã tạo các trang rỗng với thẻ tiêu đề cơ bản cho các tính năng quản trị (`Dashboard`, `Timeline`, `Bookings`, `Transactions`, `Users`, `Pitches`, `TimeSlots`, `Pricing`).
- **Định tuyến Admin:** Đã cấu hình và nhóm định tuyến cho phân hệ Quản trị (`admin.routes.tsx`) và tích hợp vào root router chính.
- **Hệ thống thông báo toàn cục:** Tích hợp `SweetAlert2`, tạo bộ tiện ích `toast.ts`, áp dụng bắt lỗi chung qua Axios Interceptor và thêm modal xác nhận Đăng xuất.

## 3. Hướng dẫn sử dụng Hệ thống Thông báo (SweetAlert2)

Hệ thống thông báo toàn cục được định nghĩa tại `frontend/src/utils/toast.ts`. Các component có thể gọi các hàm sau để hiển thị thông báo một cách đồng nhất:

- **Thông báo góc màn hình (Toast):** Sử dụng cho các tác vụ thành công, hoặc cảnh báo.
  ```typescript
  import { showToast } from '../utils/toast';
  
  // Hiển thị thông báo thành công
  showToast('Đăng nhập thành công!', 'success');
  
  // Hiển thị thông báo lỗi
  showToast('Có lỗi xảy ra', 'error');
  ```

- **Popup xác nhận (Confirm Modal):** Sử dụng khi cần người dùng xác nhận các hành động quan trọng (Đăng xuất, Xóa, Hủy đơn...). Trả về `Promise<boolean>`.
  ```typescript
  import { showConfirm } from '../utils/toast';
  
  const handleAction = async () => {
    const isConfirmed = await showConfirm(
      'Xác nhận xóa',
      'Bạn có chắc chắn muốn xóa bản ghi này?',
      'Xóa ngay' // Text của nút đồng ý
    );
    
    if (isConfirmed) {
      // Thực hiện logic sau khi xác nhận...
    }
  };
  ```

- **Xử lý lỗi tự động (Axios):** Mọi lỗi HTTP từ phía Server (trừ 401) đều đã được cấu hình bắt tự động ở `frontend/src/services/api.ts` và sẽ tự động hiển thị Toast báo lỗi. Do đó, bạn không cần gọi `showToast('Lỗi...', 'error')` thủ công ở các khối `catch` khi gọi API trừ khi có yêu cầu xử lý logic đặc biệt.

---

*Hệ thống Frontend (Khách hàng) hiện tại đã chạy ổn định và sẵn sàng cho việc phát triển các chức năng chi tiết hoặc tích hợp API.*
