# ĐẶC TẢ THIẾT KẾ KỸ THUẬT: PHÂN HỆ QUẢN LÝ SÂN BÓNG (ADMIN DASHBOARD)
## Technical Design Specification — Subtask ST-03 (FPMS-103)

- **Mã công việc (Jira)**: `FPMS-103` (Subtask `ST-03`)
- **Nhánh Git**: `feature/FPMS-103-frontend-pitch-crud`
- **Tài liệu căn cứ**: SRS Mục 3.4.14 (Trang 72–79) & Backend API Controller `AdminPitchController.java`
- **Người thiết kế**: Google DeepMind Antigravity Pair Programmer & Nguyễn Hữu Giáp
- **Trạng thái**: Đã hoàn thành & Chuẩn hoá (Implemented & Standardized)
- **Ngày cập nhật**: 25/09/2026

---

## 1. Tổng Quan & Mục Tiêu Nghiệp Vụ

### 1.1. Mục tiêu
Cung cấp giao diện Quản lý Danh mục Sân bóng chuyên nghiệp, bám sát layout card tinh gọn và tối ưu trải nghiệm Admin:
- Toàn bộ nghiệp vụ hiển thị trong một khung Card duy nhất cao `calc(100vh - 140px)` chống tràn giao diện.
- Tích hợp thanh công cụ tìm kiếm và bộ lọc trực tiếp trên đầu bảng.
- Bảng danh sách hỗ trợ **Sticky Header**, cuộn dọc/ngang mượt mà.
- Phân trang đặt gọn gàng ở **góc phải chân card** (`justify-content: flex-end`).
- Tích hợp đầy đủ các API Spring Boot Backend, bắt lỗi trùng tên (Mã lỗi 3001), hộp thoại SweetAlert2 xác nhận đổi trạng thái/xóa sân.

### 1.2. Quyết định Kiến trúc & Thiết kế
1. **Kiến trúc Single-Page Card Container (`AdminPitches.tsx`)**:
   - Tinh giản toàn bộ logic vào một component quản trị duy nhất [AdminPitches.tsx](file:///f:/Working/JavaBackend/football-pitch-management-system/frontend/src/pages/admin/AdminPitches.tsx).
   - Loại bỏ các file component vụn vặt trong `src/components/admin/pitches/` để tránh phân mảnh mã nguồn, dễ bảo trì và tối ưu hiệu năng render.
2. **Khớp nối DTO Backend thực tế**:
   - Backend phân trang sử dụng `PageResponse<T>` với danh sách `items: T[]` (1-indexed thông qua `pageNo`).
   - Tự động bắt lỗi trùng tên sân (`PITCH_NAME_ALREADY_EXISTS` - Mã lỗi 3001) và hiển thị thông báo đỏ ngay trên form.
3. **Modal Overlay qua React Portal**:
   - Xây dựng component `ModalOverlay` dùng `createPortal(..., document.body)` giúp modal hiển thị độc lập với phân cấp DOM của trang, tránh lỗi cắt viền (overflow clipping).
4. **SweetAlert2 chuẩn UX**:
   - Chuyển đổi trạng thái `ACTIVE` ⇄ `MAINTENANCE` có Dialog cảnh báo phân biệt màu sắc theo ngữ cảnh (Cam/Vàng `#f59e0b` cho Bảo trì, Xanh `#10b981` cho Kích hoạt lại).
   - Xóa mềm sân bóng có Dialog cảnh báo màu đỏ (`#ef4444`).
5. **Chính sách Kiểm thử (Testing Policy)**:
   - Theo chỉ đạo của Product Owner, tầng Frontend không duy trì các bộ test JSX tĩnh để ưu tiên tốc độ bàn giao và phát triển giao diện.
   - Kiểm soát chất lượng thông qua **TypeScript Compiler (`tsc -b`)**, **Linter (`oxlint`)** và **Kiểm thử trực quan tương tác thực tế**.

---

## 2. Cấu Trúc Mã Nguồn Sau Chuẩn Hoá

```
frontend/src/
├── types/
│   └── pitch.ts                  # Khai báo TypeScript types, DTOs & filter params
├── services/
│   └── pitchService.ts           # Đóng gói các hàm gọi API RESTful qua axios api.ts
└── pages/admin/
    └── AdminPitches.tsx          # Single-Page Container tích hợp Toolbar, Sticky Table, Modals & Right Pagination
```

*(Thư mục `src/components/admin/pitches/` và các file test frontend thừa đã được dọn dẹp sạch sẽ).*

---

## 3. Đặc Tả Dữ Liệu & API Contract

### 3.1. TypeScript Definitions (`src/types/pitch.ts`)

```typescript
// 1. Trạng thái hoạt động của sân
export type PitchStatus = 'ACTIVE' | 'MAINTENANCE';

// 2. Loại sân bóng
export interface PitchType {
  id: number;
  name: string;
  playerCapacity: number;
  description?: string;
}

// 3. Thực thể Sân bóng hiển thị trên Frontend
export interface Pitch {
  id: number;
  name: string;
  pitchType: PitchType;
  status: PitchStatus;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

// 4. Phân trang chuẩn từ Backend PageResponse.java
export interface PageResponse<T> {
  items: T[];           // Danh sách dữ liệu từ Spring Boot
  content?: T[];        // Fallback backward-compatibility
  pageNo: number;       // Trang hiện tại (1-indexed)
  pageSize: number;     // Số bản ghi trên 1 trang
  totalElements: number;// Tổng số bản ghi thỏa mãn điều kiện
  totalPages: number;   // Tổng số trang
  isLast?: boolean;     // Đã đến trang cuối chưa
}

// 5. Tham số lọc và phân trang truyền lên API
export interface PitchFilterParams {
  keyword?: string;
  pitchTypeId?: number | string; // 'ALL' hoặc ID cụ thể
  status?: PitchStatus | 'ALL' | '';
  page: number; // 1-indexed
  size: number;
}

// 6. Payload Thêm mới / Cập nhật sân
export interface PitchRequest {
  name: string;
  pitchTypeId: number;
  description?: string;
}

// 7. Payload Đổi trạng thái sân
export interface UpdatePitchStatusRequest {
  status: PitchStatus;
}
```

### 3.2. Chi Tiết API Endpoints (`src/services/pitchService.ts`)

| Phương thức | Endpoint | Request Body / Query Params | Phản hồi (`data`) | Mô tả |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/admin/pitches` | `keyword`, `pitchTypeId`, `status`, `page`, `size` | `PageResponse<Pitch>` | Tìm kiếm, lọc và phân trang danh sách sân. |
| `GET` | `/api/v1/admin/pitches/types` | Không | `PitchType[]` | Danh mục loại sân phục vụ dropdown bộ lọc và modal form. |
| `GET` | `/api/v1/admin/pitches/{id}` | Không | `Pitch` | Lấy chi tiết sân bóng theo ID. |
| `POST` | `/api/v1/admin/pitches` | `PitchRequest` | `Pitch` | Thêm sân mới (Trạng thái mặc định `ACTIVE`, chặn trùng tên). |
| `PUT` | `/api/v1/admin/pitches/{id}` | `PitchRequest` | `Pitch` | Chỉnh sửa tên, loại sân và mô tả của sân. |
| `PATCH` | `/api/v1/admin/pitches/{id}/status` | `{ status }` | `Pitch` | Chuyển đổi trạng thái `ACTIVE` ⇄ `MAINTENANCE`. |
| `DELETE` | `/api/v1/admin/pitches/{id}` | Không | `void` | Xóa mềm sân bóng (`isDeleted = true`). |

---

## 4. Đặc Tả Giao Diện & Bố Cục UI (`AdminPitches.tsx`)

### 4.1. Khung Card Tổng Thể (Master Card)
- **Container**: `style={{ height: 'calc(100vh - 140px)', display: 'flex', flexDirection: 'column' }}`.
- **Card**: Thẻ `.card` với `flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', transform: 'none'`.
- Nền `var(--color-bg-surface)`, viền `1px solid var(--color-border)`.

### 4.2. Thanh Công Cụ Toolbar (Top Bar) & Responsive
- **Vị trí**: Nằm ở phần đỉnh của card, `flex-shrink: 0`, margin bottom `1.5rem`.
- **Desktop ($\ge$ 860px)**:
  - Cụm bên trái: Ô tìm kiếm input: Chiều cao `42px`, chiều rộng cố định `300px`, bo góc `var(--radius-md)`. Icon `Search` bên trái, placeholder: *"Tìm theo tên sân..."*. Nhập từ khóa tự động lọc danh sách và đưa `page` về `1`.
  - Cụm bên phải: Dropdown Loại sân, Dropdown Trạng thái, Nút `+ Thêm sân mới`.
- **Tablet (768px - 860px)**:
  - Ô tìm kiếm chuyển thành 100% chiều rộng ở hàng 1.
  - Dropdown Loại sân, Dropdown Trạng thái và Nút Thêm mới chia đều ở hàng 2.
- **Mobile ($\le$ 640px)**:
  - Ô tìm kiếm 100% chiều rộng ở hàng 1.
  - Cụm bộ lọc chia theo `grid 1fr 1fr` 2 cột cân đối ở hàng 2 (Dropdown Loại sân 50%, Dropdown Trạng thái 50%).
  - Nút `+ Thêm sân mới` chiếm full-width ở hàng 3 (`grid-column: span 2`).

### 4.3. Bảng Dữ Liệu (Sticky Header Table) & Trạng Thái Rỗng (Empty State)
- **Vùng cuộn**: `flex: 1, overflowY: 'auto', overflowX: 'auto', borderTop: '1px solid var(--color-border)'`.
- **Header cố định**: `thead` có `position: sticky, top: 0, backgroundColor: 'var(--color-bg-surface)', zIndex: 10`.
- **Responsive Table**:
  - Khi có dữ liệu: Thiết lập `min-width: 650px` cho phép cuộn/vuốt ngang mượt mà trên mobile, chống bẹp méo cột và bảo toàn kích thước nút thao tác.
  - Khi rỗng / đang tải (`.is-empty`): Bỏ ép `650px`, chuyển sang `min-width: 100%` và `height: 100%` để không sinh thanh cuộn ngang thừa.
- **Căn giữa trạng thái rỗng (Empty State Centering)**:
  - Dòng thông báo *"Không tìm thấy sân bóng nào phù hợp"* được đặt trong ô `<td>` kéo dài toàn bộ chiều cao còn lại của card (`verticalAlign: 'middle'`).
  - Flexbox container bên trong căn giữa tuyệt đối (`alignItems: 'center'`, `justifyContent: 'center'`) cả theo trục ngang lẫn trục dọc của màn hình/card.
- **Cấu trúc 4 cột**:
  1. `TÊN SÂN`:
     - Tên sân bóng in đậm font-semibold.
     - Mã định danh nhỏ gọn bên dưới: `<div className="text-xs text-muted font-normal mt-1">Mã: {pitch.id}</div>`.
  2. `LOẠI SÂN`:
     - Huy hiệu `badge` nền `var(--color-bg-base)` bo viền: `Sân 5 người`, `Sân 7 người`...
  3. `TRẠNG THÁI`:
     - `ACTIVE`: Badge xanh lá mạ `badge badge-success` ("Đang hoạt động").
     - `MAINTENANCE`: Badge cam hổ phách `badge badge-warning` ("Đang bảo trì").
  4. `THAO TÁC`:
     - Nút **Đổi trạng thái bảo trì**:
       - Nếu đang `ACTIVE`: Nút `btn btn-secondary text-primary` icon `Settings` (Cài đặt bảo trì).
       - Nếu đang `MAINTENANCE`: Nút `btn btn-secondary text-success` icon `Wrench` (Mở khóa sân).
     - Nút **Chỉnh sửa**: `btn btn-secondary` icon `Edit`.
     - Nút **Xóa sân**: `btn btn-secondary text-danger` icon `Trash2`.

### 4.4. Phân Trang Góc Phải (Right-Aligned Pagination)
- **Vị trí**: Đặt tại đáy card với `flexShrink: 0, borderTop: '1px solid var(--color-border)', padding: '0.75rem 1.25rem'`.
- **Layout**:
  - Desktop & Tablet: Căn sang góc phải chân card (`justify-content: flex-end`).
  - Mobile (< 640px): Tự động chuyển dạng cột (`flex-direction: column`, `align-items: center`) căn giữa dọc, tránh bị tràn mép ngang.
- **Thành phần**:
  - Dòng thông tin: *"Hiển thị **[start] - [end]** trên tổng số **[total]** sân bóng"*.
  - Cụm điều hướng:
    - Nút `Trước` (`btn btn-secondary`, icon `ChevronLeft`, disabled khi `page <= 1`).
    - Khối hiển thị trang hiện tại: `{page} / {totalPages}` với nền `var(--color-bg-base)`, viền `var(--color-border)`.
    - Nút `Sau` (`btn btn-secondary`, icon `ChevronRight`, disabled khi `page >= totalPages`).

### 4.5. Modal Thêm / Chỉnh Sửa (`ModalOverlay`)
- Mount qua `createPortal` vào `document.body`.
- Lớp backdrop phủ toàn màn hình `rgba(0,0,0,0.5)`, `zIndex: 9999`.
- Dialog card `maxWidth: 500px, maxHeight: 90vh, overflowY: 'auto'`.
- Header có tiêu đề rõ ràng và nút `X` đóng modal.
- Body form gồm:
  - Tên sân (bắt buộc, max 100 ký tự, có validate inline).
  - Loại sân (dropdown chọn từ danh sách).
  - Mô tả (textarea tùy chọn).
- Footer: Nút `Hủy bỏ` và Nút Submit (`Tạo Sân Mới` hoặc `Lưu Thay Đổi`).
- Trạng thái loading: Hiển thị icon `Loader2` xoay tròn khi đang lưu.

---

## 5. Quy Trình Nghiệp Vụ & Xử Lý Ca Biên

### 5.1. Luồng Đổi Trạng Thái & SweetAlert2 Phân Biệt Ngữ Cảnh
1. Click icon `Settings` / `Wrench`:
   - Nếu chuyển sang `MAINTENANCE`:
     - Dialog SweetAlert2 `warning`, màu nút `#f59e0b` (Cam).
     - Tiêu đề: *"Chuyển sân sang Bảo trì?"*.
     - Nội dung: *"Sân \"{name}\" sẽ tạm dừng hoạt động và không thể nhận lịch đặt mới..."*.
   - Nếu mở lại `ACTIVE`:
     - Dialog SweetAlert2 `question`, màu nút `#10b981` (Xanh Emerald).
     - Tiêu đề: *"Mở lại hoạt động sân?"*.
     - Nội dung: *"Sân \"{name}\" sẽ sẵn sàng đón khách và mở lịch đặt trên hệ thống."*.
2. Bấm xác nhận ➔ Gọi `PATCH /api/v1/admin/pitches/{id}/status` ➔ Reload bảng dữ liệu.

### 5.2. Ca Biên Trùng Tên Sân Bóng (Backend Code 3001)
- Khi thêm hoặc sửa sân, nếu Backend trả về mã lỗi `3001` (`PITCH_NAME_ALREADY_EXISTS`):
  - **Không đóng Modal**, giữ nguyên dữ liệu người dùng đang nhập.
  - Hiển thị dòng chữ đỏ báo lỗi trực tiếp dưới ô input: `* Tên sân bóng đã tồn tại trên hệ thống, vui lòng chọn tên khác.`
  - Hiển thị Toast thông báo lỗi.

### 5.3. Ca Biên Xóa Sân & Tự Động Lùi Trang
- Click icon `Trash2` ➔ Dialog SweetAlert2 xác nhận màu đỏ (`#ef4444`).
- Gọi `DELETE /api/v1/admin/pitches/{id}`.
- **Ca biên phân trang**: Nếu bản ghi bị xóa là bản ghi duy nhất trên trang hiện tại và `page > 1`, hệ thống tự động lùi về trang trước `page - 1`.

---

## 6. Tiêu Chuẩn Nghiệm Thu (Definition of Done - DoD)

1. Giao diện tích hợp chuẩn mực trong [AdminPitches.tsx](file:///f:/Working/JavaBackend/football-pitch-management-system/frontend/src/pages/admin/AdminPitches.tsx).
2. Phân trang đặt đúng góc phải chân card, hiển thị đúng số lượng sân thực tế.
3. Không còn thư mục hoặc file component thừa trong `src/components/admin/pitches/`.
4. Không còn file test frontend tồn đọng theo yêu cầu dọn dẹp.
5. Biên dịch `npm run build` (`tsc -b && vite build`) thành công 100% không cảnh báo lỗi type.
6. Kiểm tra `npm run lint` đạt 0 lỗi (`0 errors`).
