# Kế hoạch Tinh gọn Code Frontend Quản trị Sân bóng & Khung giờ (Đơn giản, Trực quan cho Intern)

> **Mục tiêu chính:** Tối ưu và rút gọn cả 2 màn hình quản trị chính của module: **Quản lý Sân bóng (`AdminPitches.tsx`)** và **Quản lý Khung giờ (`AdminTimeSlots.tsx`)** theo phong cách **dễ hiểu, trực quan nhất cho intern**. Không làm phức tạp hóa vấn đề, không tạo nhiều tầng trừu tượng, giữ nguyên 100% nghiệp vụ và giao diện.

---

## 1. Vấn đề chung của cả 2 trang (Tại sao code bị dài hơn 1.000 dòng?)

Cả hai trang `AdminPitches.tsx` (1.041 dòng) và `AdminTimeSlots.tsx` (1.458 dòng) đều đang gặp đúng **3 vấn đề giống hệt nhau**:
1. **Trùng lặp khung Modal (`ModalOverlay`):** Cả hai trang đều tự viết lại component `ModalOverlay` (khoảng 45 dòng mỗi bên) thay vì dùng chung một file.
2. **Trùng lặp Form Thêm mới & Chỉnh sửa:**
   - Trong `AdminPitches`: Form Thêm sân và Form Sửa sân giống nhau 95% (tên sân, loại sân, mô tả, nút Lưu/Hủy) nhưng viết lặp 2 lần (~250 dòng).
   - Trong `AdminTimeSlots`: Form Thêm ca và Form Sửa ca cũng lặp lại 2 lần (~500 dòng).
3. **Các biến State của Form bị gom hết vào trang chính:** Khiến trang chính phải quản lý quá nhiều biến tạm (`addName`, `editName`, `addStartTime`, `editStartTime`...).

---

## 2. Cấu trúc thư mục mới (Rất gọn gàng & Đồng bộ)

Chỉ tổ chức lại theo cấu trúc cực kỳ trực diện, đàn em intern nhìn vào là hiểu ngay:

```
frontend/src/
├── utils/
│   └── timeUtils.ts                 <-- 1. Chứa 3 hàm tính giờ cơ bản (dưới 40 dòng)
├── components/
│   ├── common/
│   │   ├── ModalOverlay.tsx         <-- 2. Khung popup cơ bản (DÙNG CHUNG cho cả Sân và Khung giờ)
│   │   └── TimeInputWithPicker.tsx  <-- 3. Ô nhập giờ kết hợp (gõ số + chọn icon đồng hồ)
│   └── admin/
│       ├── pitch/
│       │   └── PitchModal.tsx       <-- 4. Form Thêm / Sửa Sân bóng (dưới 180 dòng)
│       └── timeslot/
│           └── TimeSlotModal.tsx    <-- 5. Form Thêm / Sửa Khung giờ (dưới 200 dòng)
└── pages/admin/
    ├── AdminPitches.tsx             <-- 6. Trang quản lý Sân bóng (giảm từ 1.041 dòng -> ~260 dòng)
    └── AdminTimeSlots.tsx           <-- 7. Trang quản lý Khung giờ (giảm từ 1.458 dòng -> ~220 dòng)
```

---

## 3. Chi tiết các bước thực hiện (4 Giai đoạn đơn giản)

### Giai đoạn 1: Tạo các thành phần dùng chung (Common Utilities & Components)
1. **`src/utils/timeUtils.ts`**:
   - `isValidTimeFormat(time)`: kiểm tra giờ dạng `HH:mm`.
   - `calculateDurationMinutes(start, end)`: tính số phút chênh lệch: `(endH - startH) * 60 + (endM - startM)`.
   - `formatDuration(minutes)`: đổi 90 phút thành `"1 giờ 30 phút"`.
2. **`src/components/common/ModalOverlay.tsx`**:
   - Tạo khung popup nổi với nền đen mờ (`rgba(0,0,0,0.55)`), bấm ra ngoài thì gọi `onClose`. Dùng chung cho cả trang Pitches và TimeSlots.
3. **`src/components/common/TimeInputWithPicker.tsx`**:
   - Ô nhập giờ cho TimeSlot: chỉ cho nhập số và dấu `:`, bấm icon đồng hồ để mở bộ chọn giờ.

### Giai đoạn 2: Tinh gọn Quản lý Sân bóng (AdminPitches)
1. **Tạo `src/components/admin/pitch/PitchModal.tsx`**:
   - Gom 2 popup Thêm sân mới và Sửa sân bóng thành 1 component form.
   - Nhận props đơn giản:
     - `isOpen`: boolean
     - `isEdit`: boolean (thêm hay sửa)
     - `initialData`: thông tin sân bóng hiện tại (khi sửa)
     - `pitchTypes`: danh sách loại sân (để đổ vào dropdown `<select>`)
     - `onClose`: hàm đóng modal
     - `onSubmit`: hàm gửi dữ liệu
     - `submitting`: boolean
   - Có kiểm tra rỗng tên sân, chọn loại sân.
2. **Tái cấu trúc `src/pages/admin/AdminPitches.tsx`**:
   - Bỏ code `ModalOverlay` và 2 form modal dài dòng.
   - Trang chỉ tập trung vào: bộ lọc tìm kiếm, bảng danh sách sân (Sticky Header), phân trang, gọi API `pitchService`.
   - Nhúng `<PitchModal />` vào cuối trang.
   - **Dung lượng giảm từ 1.041 dòng xuống ~260 dòng!**

### Giai đoạn 3: Tinh gọn Quản lý Khung giờ (AdminTimeSlots)
1. **Tạo `src/components/admin/timeslot/TimeSlotModal.tsx`**:
   - Gom 2 popup Thêm ca đá và Sửa ca đá thành 1 component form.
   - Nhận props: `isOpen`, `isEdit`, `initialData`, `onClose`, `onSubmit`, `submitting`.
   - Tự động kiểm tra thời lượng 1h - 2h (`duration < 60 || duration > 120`).
   - 2 Card bấm chọn Giờ thường / Giờ vàng (cân xứng 50% - 50%).
   - 2 Nút bấm Hủy / Lưu (cân xứng 50% - 50%).
2. **Tái cấu trúc `src/pages/admin/AdminTimeSlots.tsx`**:
   - Trang chỉ tập trung vào: gọi API danh sách ca đá thật, hiển thị bảng ca đá kèm giá sân 5 và sân 7, nút Thêm, nút Sửa, nút Xóa (Optimistic UI).
   - Nhúng `<TimeSlotModal />` vào cuối trang.
   - **Dung lượng giảm từ 1.458 dòng xuống ~220 dòng!**

### Giai đoạn 4: Kiểm tra và Nghiệm thu toàn diện
1. Chạy `npm run lint` kiểm tra toàn bộ frontend không có bất kỳ lỗi nào.
2. Chạy `npm run build` để chắc chắn bản build production thành công 100%.
3. Kiểm tra thực tế trên trình duyệt (`http://localhost:5173`):
   - Màn hình Sân bóng: Tìm kiếm, lọc loại sân, thêm sân, sửa sân, đổi bảo trì, xóa sân.
   - Màn hình Khung giờ: Hiển thị bảng giá thật, thêm ca mới, sửa ca, xóa ca.

---

## 4. Cam kết chất lượng (Theo FE Rules trong GEMINI.md)

- **100% comment tiếng Việt chi tiết** trong mọi file mới và file sửa đổi.
- Cách viết code trực quan, dùng `useState` và `if...else` thông thường, ai đọc vào cũng hiểu được ngay.
- Không thay đổi bất kỳ logic nghiệp vụ nào (Zero regression).
