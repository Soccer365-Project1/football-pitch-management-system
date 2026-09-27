# Kế Hoạch Triển Khai Thực Dụng (Pragmatic Implementation Plan)
## SUBTASK ST-03 (FPMS-103): GIAO DIỆN & TÍCH HỢP API QUẢN LÝ SÂN BÓNG (ADMIN DASHBOARD)

- **Trạng thái**: Đã hoàn thành & Chuẩn hoá (Implemented & Standardized)
- **Tài liệu đặc tả kỹ thuật**: [`docs/fe/pitch-and-timeslot-module/02_THIET_KE_CHI_TIET_ST03_QUAN_LY_SAN_BONG_SPEC.md`](file:///f:/Working/JavaBackend/football-pitch-management-system/docs/fe/pitch-and-timeslot-module/02_THIET_KE_CHI_TIET_ST03_QUAN_LY_SAN_BONG_SPEC.md)

**Mục tiêu:** Xây dựng hoàn chỉnh giao diện Quản lý Danh mục Sân bóng (Admin Dashboard) theo chuẩn SRS Mục 3.4.14, kết nối 100% API Spring Boot Backend, giao diện Card tối ưu, phân trang góc phải và thông báo SweetAlert2 theo ngữ cảnh.

**Kiến trúc:** Áp dụng kiến trúc Single-Page Card Container tích hợp trong `frontend/src/pages/admin/AdminPitches.tsx`. Dọn dẹp thư mục component con `src/components/admin/pitches/` và các file test frontend thừa để tối ưu tốc độ phát triển và chống phân mảnh mã nguồn.

---

## Bảng Phân Rã & Nhật Ký Hoàn Thành

- [x] **Task 1: Chuẩn hóa kiểu dữ liệu TypeScript (`src/types/pitch.ts`)**
  - Khớp nối DTOs: `Pitch`, `PitchType`, `PitchStatus`, `PageResponse<T>`, `PitchFilterParams`, `PitchRequest`.
- [x] **Task 2: Xây dựng tầng dịch vụ API (`src/services/pitchService.ts`)**
  - Đóng gói 6 API RESTful kết nối Backend Spring Boot.
- [x] **Task 3: Triển khai giao diện Card tổng thể (`src/pages/admin/AdminPitches.tsx`)**
  - Khung card `calc(100vh - 140px)` với `overflow: hidden`, `transform: none`.
  - Thanh toolbar tích hợp ô tìm kiếm 300px, dropdown chọn loại sân, dropdown trạng thái và nút `+ Thêm sân mới`.
- [x] **Task 4: Bảng danh sách với Sticky Header & Thao tác nghiệp vụ**
  - Cột TÊN SÂN (kèm mã ID), LOẠI SÂN (badge), TRẠNG THÁI (badge success/warning), THAO TÁC (Đổi bảo trì, Sửa, Xóa).
  - Tích hợp SweetAlert2 xác nhận đổi trạng thái và xóa sân kèm phân biệt màu sắc.
  - Tự động lùi về trang trước khi xóa bản ghi duy nhất trên trang > 1.
- [x] **Task 5: Phân trang góc phải (Right-Aligned Pagination)**
  - Đặt tại góc phải chân card với layout inline flex (`justifyContent: 'flex-end'`).
  - Hiển thị dải bản ghi và các nút điều hướng `Trước`, `page / totalPages`, `Sau`.
- [x] **Task 6: Modal Thêm & Chỉnh sửa qua React Portal**
  - Sử dụng `createPortal(..., document.body)` chống lỗi tràn viền.
  - Bắt lỗi trùng tên sân từ Backend (Mã lỗi 3001) và hiển thị thông báo lỗi trực tiếp dưới ô input.
- [x] **Task 7: Dọn dẹp mã nguồn & Kiểm thử chất lượng**
  - Dọn dẹp thư mục `src/components/admin/pitches/`.
  - Dọn dẹp các file test frontend theo chỉ đạo của Product Owner.
  - Kiểm tra `npm run build` (`tsc -b && vite build`) đạt `exit code 0`.
  - Kiểm tra `npm run lint` (`oxlint`) đạt `0 errors`.
