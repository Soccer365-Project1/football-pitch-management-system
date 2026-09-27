# ĐẶC TẢ TIÊU CHÍ NGHIỆM THU & THIẾT KẾ KỊCH BẢN KIỂM THỬ TÍCH HỢP (FPMS-103)
## Acceptance Criteria & Comprehensive Test Scenarios Specification — Subtask ST-05

> **Kỹ năng chuẩn hóa**: `acceptance-criteria-and-test-design`  
> **Dự án**: Football Pitch Management System (FPMS)  
> **Mã công việc (Jira)**: `FPMS-103` (Subtask `ST-05`: *[Testing] Thực hiện Integration Test kết nối FE - BE và nghiệm thu dữ liệu DB*)  
> **Nhánh Git**: `test/FPMS-103-integration-testing`  
> **Kế hoạch thực thi đi kèm**: [2026-09-27-admin-integration-test-fe-be-db.md](../plans/2026-09-27-admin-integration-test-fe-be-db.md)  
> **Tài liệu tham chiếu**: SRS Mục 3.4.14 (Trang 72–79) & Mục 3.4.15 (Trang 79–86)   
> **Trạng thái**: Đã phê duyệt & Đang triển khai thực thi (Standardized Specification)  
> **Ngày cập nhật**: 27/09/2026  

---

## 1. Phân Tích Phạm Vi & Tác Nhân (Scope & Actors)

```
┌────────────────────────────────────────────────────────────────────────┐
│  BƯỚC 1: PHÂN TÍCH PHẠM VI & TÁC NHÂN (SCOPE & ACTORS)                │
└────────────────────────────────────────────────────────────────────────┘
```

### 1.1. Tác Nhân Hệ Thống (Personas & Roles)
1. **Quản trị viên hệ thống / Chủ sân (`ROLE_ADMIN`)**:
   - Được cấp toàn quyền trên các endpoint `/api/v1/admin/pitches/**` và `/api/v1/admin/time-slots/**`.
   - Có thể thêm, sửa, đổi trạng thái bảo trì/hoạt động, cấu hình giờ vàng, xóa tài nguyên.
2. **Khách hàng (`ROLE_CUSTOMER`)**:
   - Chỉ được xem danh mục công khai, không có quyền can thiệp vào các API cấu hình quản trị.
   - Khi cố tình gọi API `/api/v1/admin/**`, hệ thống bắt buộc phải từ chối với mã phản hồi `403 Forbidden`.
3. **Người dùng chưa xác thực (Anonymous / Unauthenticated)**:
   - Chưa gửi Bearer JWT Token hoặc Token không hợp lệ/hết hạn.
   - Hệ thống từ chối truy cập với mã phản hồi `401 Unauthorized` hoặc `403 Forbidden`.

### 1.2. Ranh Giới Phạm Vi (Scope Boundaries)
- **Trong phạm vi (In-Scope)**:
  - **Tích hợp 3 tầng (End-to-End Backend & Database)**: `MockMvc (HTTP Client)` ➔ `Controller` ➔ `Service` ➔ `Repository` ➔ `PostgreSQL (fpms_test)`.
  - **Phân hệ Quản lý Sân bóng (`PitchManagement`)**: CRUD, kiểm tra trùng lặp tên không phân biệt hoa thường (`PITCH_NAME_ALREADY_EXISTS`), chuyển đổi trạng thái `ACTIVE` ⇄ `MAINTENANCE`, tìm kiếm / lọc / phân trang, kiểm tra toàn vẹn CSDL chặn xóa khi đã phát sinh đơn đặt sân (`PITCH_HAS_BOOKINGS`).
  - **Phân hệ Quản lý Khung giờ (`TimeSlotManagement`)**: CRUD, phân loại Giờ vàng / Giờ thường (`is_peak_hour`), kiểm tra thứ tự giờ (`startTime < endTime`), kiểm tra độ dài ca (60 – 120 phút), thuật toán chống chồng chéo thời gian (Time Overlap), cho phép tiếp giáp biên thời gian (Touching Boundary), xóa mềm (`is_active = false`), chặn xóa khi có đơn đặt trong tương lai (`TIME_SLOT_HAS_ACTIVE_BOOKINGS`).
  - **Bảo mật phân quyền (RBAC)**: Bảo vệ toàn bộ endpoint admin trước `ROLE_CUSTOMER` và Anonymous.
  - **Kiểm thử tích hợp FE - BE**: Ma trận đối soát phản hồi UI/UX (Loading state, Toast notification, Error modal, Table re-render).
- **Ngoài phạm vi (Out-of-Scope)**:
  - Tích hợp cổng thanh toán trực tuyến bên thứ 3 (VNPAY / MoMo).
  - Nghiệp vụ đặt sân trên Mobile App dành cho người dùng cuối.

---

## 2. Tiêu Chí Nghiệm Thu (Acceptance Criteria - AC)

```
┌────────────────────────────────────────────────────────────────────────┐
│  BƯỚC 2: SOẠN THẢO TIÊU CHÍ NGHIỆM THU (GHERKIN & BUSINESS RULES)     │
└────────────────────────────────────────────────────────────────────────┘
```

### 2.1. Kịch Bản Hành Vi Chính (Gherkin Format)

#### Phân hệ Quản lý Sân bóng (Pitch Management)
```gherkin
Kịch bản: AC_PITCH_01 - Thêm mới sân bóng thành công và lưu trữ thực tế trong PostgreSQL
Given Admin đã đăng nhập với vai trò ROLE_ADMIN
When Gửi yêu cầu POST /api/v1/admin/pitches với Tên sân "Sân 5C - Test IT", pitchTypeId = 1
Then API phản hồi HTTP 201 Created kèm dữ liệu sân có status = 'ACTIVE'
And CSDL PostgreSQL bảng "pitches" lưu đúng 1 bản ghi có name = 'Sân 5C - Test IT', status = 'ACTIVE'

Kịch bản: AC_PITCH_02 - Chặn thêm sân bóng khi tên sân bị trùng lặp (không phân biệt hoa thường)
Given CSDL đã tồn tại sân bóng mang tên "Sân 5 Trùng Tên"
When Gửi yêu cầu POST /api/v1/admin/pitches với Tên sân "sân 5 trùng tên"
Then API phản hồi HTTP 400 Bad Request kèm mã lỗi 3002 (PITCH_NAME_ALREADY_EXISTS)
And CSDL không phát sinh thêm bất kỳ bản ghi mới nào

Kịch bản: AC_PITCH_03 - Cập nhật trạng thái sân bóng sang MAINTENANCE và khôi phục ACTIVE
Given Đã tồn tại sân bóng đang có status = 'ACTIVE'
When Gửi PATCH /api/v1/admin/pitches/{id}/status với body {"status": "MAINTENANCE"}
Then API phản hồi HTTP 200 OK kèm status = 'MAINTENANCE'
And CSDL bảng "pitches" cập nhật status = 'MAINTENANCE'
When Gửi tiếp PATCH /api/v1/admin/pitches/{id}/status với body {"status": "ACTIVE"}
Then API phản hồi HTTP 200 OK và CSDL cập nhật lại status = 'ACTIVE'

Kịch bản: AC_PITCH_04 - Chặn xóa sân bóng đã có lịch sử đặt sân trong CSDL
Given Đã tồn tại sân bóng có liên kết đơn đặt sân trong bảng "bookings"
When Gửi yêu cầu DELETE /api/v1/admin/pitches/{id}
Then API phản hồi HTTP 400 Bad Request kèm mã lỗi 3004 (PITCH_HAS_BOOKINGS)
And Bản ghi sân bóng vẫn được giữ nguyên vẹn trong CSDL

Kịch bản: AC_PITCH_05 - Chặn quyền truy cập API Quản trị sân bóng từ tài khoản Khách hàng
Given Người dùng đăng nhập với vai trò ROLE_CUSTOMER
When Gửi yêu cầu GET /api/v1/admin/pitches hoặc POST /api/v1/admin/pitches
Then Spring Security chặn lại và phản hồi HTTP 403 Forbidden
```

#### Phân hệ Quản lý Khung giờ (TimeSlot Management)
```gherkin
Kịch bản: AC_SLOT_01 - Tạo ca đá Giờ vàng (Peak Hour) thành công
Given Admin đã đăng nhập với vai trò ROLE_ADMIN
When Gửi POST /api/v1/admin/time-slots với startTime "17:30", endTime "19:00", isPeakHour true
Then API phản hồi HTTP 201 Created kèm isPeakHour = true
And CSDL PostgreSQL bảng "time_slots" lưu bản ghi với start_time = '17:30:00' và is_peak_hour = true

Kịch bản: AC_SLOT_02 - Chặn tạo ca đá bị trùng hoặc chồng chéo thời gian (Time Overlap)
Given Hệ thống đã tồn tại ca đá "17:30 - 19:00"
When Gửi POST /api/v1/admin/time-slots với khoảng thời gian giao thoa "18:00 - 19:30"
Then API phản hồi HTTP 400 Bad Request kèm mã lỗi 3013 (TIME_SLOT_OVERLAPPING)
And CSDL không tạo thêm ca đá mới

Kịch bản: AC_SLOT_03 - Cho phép tạo ca đá tiếp giáp biên thời gian (Touching Boundary)
Given Hệ thống đã có ca đá "17:30 - 19:00"
When Gửi POST /api/v1/admin/time-slots với startTime "19:00", endTime "20:30" (giờ bắt đầu trùng giờ kết thúc ca trước)
Then API phản hồi HTTP 201 Created
And CSDL lưu ca đá thành công vì không vi phạm chồng chéo

Kịch bản: AC_SLOT_04 - Chặn tạo ca đá khi thời lượng không nằm trong khoảng 60 - 120 phút
When Gửi POST /api/v1/admin/time-slots với startTime "06:00", endTime "06:59" (59 phút)
Then API phản hồi HTTP 400 Bad Request kèm mã lỗi 3012 (TIME_SLOT_INVALID_DURATION)
When Gửi POST /api/v1/admin/time-slots với startTime "06:00", endTime "08:01" (121 phút)
Then API phản hồi HTTP 400 Bad Request kèm mã lỗi 3015 (TIME_SLOT_INVALID_DURATION)

Kịch bản: AC_SLOT_05 - Xóa mềm khung giờ chưa có đơn đặt trong tương lai
Given Đã tồn tại ca đá chưa có đơn đặt sân từ hôm nay trở về sau
When Gửi DELETE /api/v1/admin/time-slots/{id}
Then API phản hồi HTTP 200 OK
And CSDL bảng "time_slots" cập nhật is_active = false
```

---

### 2.2. Checklist Quy Tắc Nghiệp Vụ (Business Rules Checklist)

| Mã Quy Tắc | Tên Quy Tắc | Chi Tiết Ràng Buộc Kỹ Thuật & CSDL | Xử Lý Khi Vi Phạm |
| :--- | :--- | :--- | :--- |
| **BR_PITCH_01** | Bắt buộc tên sân | Trường `name` không được null, rỗng hoặc chỉ chứa khoảng trắng (`@NotBlank`). | HTTP 400 (Validation Error) |
| **BR_PITCH_02** | Độ dài tên sân | Độ dài chuỗi `name` từ 1 đến tối đa 100 ký tự (`@Size(max=100)`). | HTTP 400 (Validation Error) |
| **BR_PITCH_03** | Duy nhất tên sân | Tên sân không được trùng lặp với các sân khác chưa bị xóa (`isDeleted = false`), so sánh không phân biệt chữ hoa chữ thường (`ILIKE` / `lower()`). | HTTP 400 (`PITCH_NAME_ALREADY_EXISTS` - 3002) |
| **BR_PITCH_04** | Toàn vẹn loại sân | `pitchTypeId` phải tồn tại trong bảng `pitch_types` và có `is_deleted = false`. | HTTP 404 (`PITCH_TYPE_NOT_FOUND` - 3003) |
| **BR_PITCH_05** | Toàn vẹn dữ liệu đặt | Không cho phép xóa cứng sân bóng nếu `bookingRepository.existsByPitchId(id) == true`. | HTTP 400 (`PITCH_HAS_BOOKINGS` - 3005) |
| **BR_PITCH_06** | Trạng thái mặc định | Sân mới tạo mặc định mang trạng thái `ACTIVE`. Chỉ chấp nhận 2 trạng thái: `ACTIVE` hoặc `MAINTENANCE`. | HTTP 400 (`PITCH_STATUS_INVALID` - 3004) |
| **BR_SLOT_01** | Thứ tự thời gian | Giờ bắt đầu phải nhỏ hơn giờ kết thúc (`startTime < endTime`). | HTTP 400 (`TIME_SLOT_INVALID_TIME` - 3012) |
| **BR_SLOT_02** | Độ dài ca đá (BVA) | Thời lượng ca đá ($\Delta t = \text{endTime} - \text{startTime}$) phải thỏa mãn: $60 \le \Delta t \le 120$ (phút). | HTTP 400 (`TIME_SLOT_INVALID_DURATION` - 3015) |
| **BR_SLOT_03** | Chống chồng chéo | Hai ca đá bất kỳ không được có khoảng thời gian giao nhau: `startTime < existing.endTime AND existing.startTime < endTime`. | HTTP 400 (`TIME_SLOT_OVERLAPPING` - 3013) |
| **BR_SLOT_04** | Tiếp giáp thời gian | Cho phép ca mới có `startTime == existing.endTime` hoặc `endTime == existing.startTime`. | Hợp lệ (HTTP 201 Created) |
| **BR_SLOT_05** | Chặn xóa ca bận | Chặn xóa mềm nếu có đơn đặt từ ngày hiện tại trở đi có trạng thái trong: `PENDING_HOLD`, `WAITING_APPROVAL`, `CONFIRMED`, `IN_PROGRESS`, `WAITING_CANCELLATION`. | HTTP 400 (`TIME_SLOT_HAS_ACTIVE_BOOKINGS` - 3014) |
| **BR_SLOT_06** | Xóa mềm ca đá | Khi xóa thành công, chỉ cập nhật `is_active = false`, không xóa bản ghi khỏi CSDL. | HTTP 200 OK |
| **BR_SEC_01** | Phân quyền RBAC | Tất cả các API `/api/v1/admin/**` yêu cầu bắt buộc quyền `ROLE_ADMIN`. | HTTP 403 Forbidden |
| **BR_SEC_02** | Xác thực Token | Không có Bearer Token hoặc Token không hợp lệ. | HTTP 401 Unauthorized / 403 Forbidden |

---

## 3. Ma Trận Kịch Bản Kiểm Thử Chi Tiết (Comprehensive Test Matrix)

```
┌────────────────────────────────────────────────────────────────────────┐
│  BƯỚC 3: THIẾT KẾ MA TRẬN 6 KHÍA CẠNH KIỂM THỬ (6-AXIS TEST MATRIX)    │
│  1. Happy Path │ 2. Negative │ 3. BVA │ 4. Edge Cases │ 5. Security    │
│  6. UI/UX & Frontend Integration                                       │
└────────────────────────────────────────────────────────────────────────┘
```

Quy tắc đặt mã Test Case chuẩn hóa: `TC_[MÃ_MODULE]_[HÀNH_ĐỘNG]_[STT]`

### 3.1. Phân Hệ Quản Lý Sân Bóng (Pitch Management) — 17 Kịch Bản Backend + CSDL

| Mã TC | Phân Loại | Mô Tả Kịch Bản Kiểm Thử | Tiền Điều Kiện | Các Bước Thực Hiện | Dữ Liệu Kiểm Thử | Kết Quả Mong Đợi |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC_PITCH_CREATE_01** | Happy Path | Thêm sân bóng mới tiêu chuẩn hợp lệ | Đăng nhập Admin (`ROLE_ADMIN`), đã có PitchType ID = 1 | 1. Chuẩn bị PitchRequest hợp lệ<br>2. Gửi POST `/api/v1/admin/pitches`<br>3. Kiểm tra DB | `name`: "Sân 5C - Test IT"<br>`pitchTypeId`: 1<br>`description`: "Cỏ nhân tạo FIFA" | - HTTP 201 Created<br>- `status`: 'ACTIVE'<br>- CSDL có 1 bản ghi `status = ACTIVE` |
| **TC_PITCH_CREATE_02** | Negative | Chặn thêm sân trùng tên với sân đã có (Case-insensitive) | Đã có sẵn sân "Sân 5 Trùng Tên" trong DB | 1. Gửi POST `/api/v1/admin/pitches` với tên viết thường<br>2. Kiểm tra mã lỗi trả về | `name`: "sân 5 trùng tên"<br>`pitchTypeId`: 1 | - HTTP 400 Bad Request<br>- `code`: 3002 (`PITCH_NAME_ALREADY_EXISTS`)<br>- CSDL không tăng bản ghi |
| **TC_PITCH_CREATE_03** | Negative | Thêm sân bóng với loại sân không tồn tại | Đăng nhập Admin | 1. Gửi POST `/api/v1/admin/pitches` với pitchTypeId ảo | `name`: "Sân Loại Ảo"<br>`pitchTypeId`: 999999 | - HTTP 404 Not Found<br>- `code`: 3003 (`PITCH_TYPE_NOT_FOUND`)<br>- CSDL không lưu |
| **TC_PITCH_CREATE_04** | Negative / BVA Min-1 | Bỏ trống tên sân bóng | Đăng nhập Admin | 1. Gửi POST `/api/v1/admin/pitches` với tên chuỗi rỗng | `name`: ""<br>`pitchTypeId`: 1 | - HTTP 400 Bad Request<br>- Lỗi validation "Tên sân bóng không được để trống" |
| **TC_PITCH_CREATE_05** | BVA Min | Tên sân bóng đạt độ dài tối thiểu 1 ký tự | Đăng nhập Admin | 1. Gửi POST `/api/v1/admin/pitches` với name 1 ký tự<br>2. Kiểm tra DB | `name`: "A"<br>`pitchTypeId`: 1 | - HTTP 201 Created<br>- CSDL lưu bản ghi tên "A" |
| **TC_PITCH_CREATE_06** | BVA Max | Tên sân bóng đạt đúng độ dài tối đa 100 ký tự | Đăng nhập Admin | 1. Gửi POST `/api/v1/admin/pitches` với chuỗi đúng 100 ký tự | `name`: Chuỗi 100 ký tự hợp lệ<br>`pitchTypeId`: 1 | - HTTP 201 Created<br>- CSDL lưu độ dài `length(name) = 100` |
| **TC_PITCH_CREATE_07** | Negative / BVA Max+1 | Tên sân bóng vượt quá 100 ký tự (101 ký tự) | Đăng nhập Admin | 1. Gửi POST `/api/v1/admin/pitches` với chuỗi 101 ký tự | `name`: Chuỗi 101 ký tự<br>`pitchTypeId`: 1 | - HTTP 400 Bad Request<br>- Lỗi validation "Tên sân không được vượt quá 100 ký tự" |
| **TC_PITCH_CREATE_08** | Edge Case | Tên sân có khoảng trắng đầu/cuối và dấu Tiếng Việt | Đăng nhập Admin | 1. Gửi POST `/api/v1/admin/pitches` với khoảng trắng thừa | `name`: "  Sân Cỏ Nhân Tạo Chuẩn FIFA 5A  "<br>`pitchTypeId`: 1 | - HTTP 201 Created<br>- CSDL tự động trim chuỗi: `"Sân Cỏ Nhân Tạo Chuẩn FIFA 5A"` |
| **TC_PITCH_GET_01** | Happy Path | Lọc danh sách sân theo keyword, loại sân, trạng thái và phân trang | DB có sẵn nhiều sân bóng | 1. Gửi GET `/api/v1/admin/pitches?keyword=Test&pitchTypeId=1&status=ACTIVE&page=1&size=10` | Query params tìm kiếm | - HTTP 200 OK<br>- Trả về `PageResponse` khớp số lượng lọc trong CSDL |
| **TC_PITCH_GET_02** | Happy Path | Lấy chi tiết sân bóng theo ID hợp lệ | Đã có sân bóng trong DB | 1. Gửi GET `/api/v1/admin/pitches/{id}` | `id`: ID của sân đang tồn tại | - HTTP 200 OK<br>- Trả về thông tin chi tiết của sân bóng |
| **TC_PITCH_GET_03** | Negative | Lấy chi tiết sân bóng theo ID không tồn tại | Đăng nhập Admin | 1. Gửi GET `/api/v1/admin/pitches/999999` | `id`: 999999 | - HTTP 404 Not Found<br>- `code`: 3001 (`PITCH_NOT_FOUND`) |
| **TC_PITCH_UPDATE_01** | Happy Path | Chỉnh sửa tên và mô tả của sân bóng | Đã có sân bóng trong DB | 1. Gửi PUT `/api/v1/admin/pitches/{id}` với thông tin mới<br>2. Kiểm tra CSDL | `name`: "Sân 5C Đã Đổi Tên"<br>`desc`: "Mô tả mới" | - HTTP 200 OK<br>- CSDL cập nhật `name = 'Sân 5C Đã Đổi Tên'` |
| **TC_PITCH_UPDATE_02** | Negative | Cập nhật tên sân trùng với tên của một sân khác | Đã có Sân A và Sân B | 1. Gửi PUT `/api/v1/admin/pitches/{id_B}` với tên của Sân A | `name`: Tên của Sân A | - HTTP 400 Bad Request<br>- `code`: 3002 (`PITCH_NAME_ALREADY_EXISTS`) |
| **TC_PITCH_STATUS_01** | State Change | Chuyển đổi trạng thái sân `ACTIVE` $\rightarrow$ `MAINTENANCE` và khôi phục | Sân đang ở trạng thái `ACTIVE` | 1. PATCH `/status` sang MAINTENANCE<br>2. Kiểm tra DB<br>3. PATCH `/status` sang ACTIVE | `status`: MAINTENANCE, sau đó `status`: ACTIVE | - Cả 2 lần đều HTTP 200 OK<br>- CSDL cập nhật tương ứng `MAINTENANCE` rồi `ACTIVE` |
| **TC_PITCH_DELETE_01** | Happy Path | Xóa cứng sân bóng mới tạo chưa có lịch sử đặt | Sân mới tạo chưa có booking | 1. Gửi DELETE `/api/v1/admin/pitches/{id}`<br>2. Kiểm tra CSDL | `id`: ID sân mới | - HTTP 200 OK<br>- `SELECT count(*) WHERE id = :id` trả về 0 |
| **TC_PITCH_DELETE_02** | Negative / Data Integrity | Chặn xóa sân bóng đã có lịch sử đơn đặt sân | Sân đã có bản ghi trong bảng `bookings` | 1. Tạo đơn đặt sân gắn với sân bóng<br>2. Gửi DELETE `/api/v1/admin/pitches/{id}` | `id`: ID sân đã có đơn đặt | - HTTP 400 Bad Request<br>- `code`: 3005 (`PITCH_HAS_BOOKINGS`)<br>- Sân không bị xóa |
| **TC_PITCH_SEC_01** | Security RBAC | Chặn User vai trò `ROLE_CUSTOMER` gọi API quản trị sân bóng | Đăng nhập tài khoản `ROLE_CUSTOMER` | 1. Gửi GET `/api/v1/admin/pitches`<br>2. Gửi POST `/api/v1/admin/pitches` | Token role `ROLE_CUSTOMER` | - HTTP 403 Forbidden<br>- Request bị chặn hoàn toàn tại Filter Chain |

---

### 3.2. Phân Hệ Quản Lý Khung Giờ (TimeSlot Management) — 17 Kịch Bản Backend + CSDL

| Mã TC | Phân Loại | Mô Tả Kịch Bản Kiểm Thử | Tiền Điều Kiện | Các Bước Thực Hiện | Dữ Liệu Kiểm Thử | Kết Quả Mong Đợi |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC_SLOT_CREATE_01** | Happy Path | Tạo ca đá Giờ thường hợp lệ (90 phút) | Đăng nhập Admin | 1. Gửi POST `/api/v1/admin/time-slots`<br>2. Kiểm tra CSDL | `startTime`: 06:00, `endTime`: 07:30, `isPeakHour`: false | - HTTP 201 Created<br>- CSDL lưu `is_peak_hour = false`, `is_active = true` |
| **TC_SLOT_CREATE_02** | Happy Path | Tạo ca đá Giờ vàng hợp lệ (90 phút) | Đăng nhập Admin | 1. Gửi POST `/api/v1/admin/time-slots`<br>2. Kiểm tra CSDL | `startTime`: 17:30, `endTime`: 19:00, `isPeakHour`: true | - HTTP 201 Created<br>- CSDL lưu `is_peak_hour = true` |
| **TC_SLOT_CREATE_03** | BVA Min | Thời lượng ca đá đúng bằng tối thiểu 60 phút | Đăng nhập Admin | 1. Gửi POST ca đá đúng 60 phút | `startTime`: 06:00, `endTime`: 07:00 | - HTTP 201 Created<br>- Ca đá được lưu vào CSDL |
| **TC_SLOT_CREATE_04** | BVA Max | Thời lượng ca đá đúng bằng tối đa 120 phút | Đăng nhập Admin | 1. Gửi POST ca đá đúng 120 phút | `startTime`: 07:00, `endTime`: 09:00 | - HTTP 201 Created<br>- Ca đá được lưu vào CSDL |
| **TC_SLOT_CREATE_05** | Negative / BVA Min-1 | Thời lượng ca đá < 60 phút (59 phút) | Đăng nhập Admin | 1. Gửi POST ca đá dài 59 phút | `startTime`: 06:00, `endTime`: 06:59 | - HTTP 400 Bad Request<br>- `code`: 3015 (`TIME_SLOT_INVALID_DURATION`) |
| **TC_SLOT_CREATE_06** | Negative / BVA Max+1 | Thời lượng ca đá > 120 phút (121 phút) | Đăng nhập Admin | 1. Gửi POST ca đá dài 121 phút | `startTime`: 06:00, `endTime`: 08:01 | - HTTP 400 Bad Request<br>- `code`: 3015 (`TIME_SLOT_INVALID_DURATION`) |
| **TC_SLOT_CREATE_07** | Negative | Giờ kết thúc trước giờ bắt đầu | Đăng nhập Admin | 1. Gửi POST với endTime < startTime | `startTime`: 19:00, `endTime`: 17:30 | - HTTP 400 Bad Request<br>- `code`: 3012 (`TIME_SLOT_INVALID_TIME`) |
| **TC_SLOT_CREATE_08** | Negative | Giờ kết thúc bằng giờ bắt đầu | Đăng nhập Admin | 1. Gửi POST với endTime == startTime | `startTime`: 07:00, `endTime`: 07:00 | - HTTP 400 Bad Request<br>- `code`: 3012 (`TIME_SLOT_INVALID_TIME`) |
| **TC_SLOT_CREATE_09** | Negative / Overlap 1 | Trùng khít hoàn toàn thời gian với ca đã có | Đã có ca 17:30 - 19:00 | 1. Gửi POST với đúng 17:30 - 19:00 | `startTime`: 17:30, `endTime`: 19:00 | - HTTP 400 Bad Request<br>- `code`: 3013 (`TIME_SLOT_OVERLAPPING`) |
| **TC_SLOT_CREATE_10** | Negative / Overlap 2 | Ca mới nằm lọt thỏm bên trong ca đã có | Đã có ca 17:30 - 19:00 | 1. Gửi POST với khoảng thời gian nằm giữa | `startTime`: 17:45, `endTime`: 18:45 | - HTTP 400 Bad Request<br>- `code`: 3013 (`TIME_SLOT_OVERLAPPING`) |
| **TC_SLOT_CREATE_11** | Negative / Overlap 3 | Ca mới bắt đầu trước và lấn vào ca đã có | Đã có ca 17:30 - 19:00 | 1. Gửi POST ca lấn đầu | `startTime`: 17:00, `endTime`: 18:30 | - HTTP 400 Bad Request<br>- `code`: 3013 (`TIME_SLOT_OVERLAPPING`) |
| **TC_SLOT_CREATE_12** | Negative / Overlap 4 | Ca mới bắt đầu trong ca cũ và kết thúc sau | Đã có ca 17:30 - 19:00 | 1. Gửi POST ca lấn đuôi | `startTime`: 18:30, `endTime`: 20:00 | - HTTP 400 Bad Request<br>- `code`: 3013 (`TIME_SLOT_OVERLAPPING`) |
| **TC_SLOT_CREATE_13** | Edge Case | Tiếp giáp biên thời gian (Touching Boundary) | Đã có ca 17:30 - 19:00 | 1. Gửi POST ca bắt đầu ngay tại 19:00 | `startTime`: 19:00, `endTime`: 20:30 | - HTTP 201 Created<br>- Cho phép lưu vì không giao thoa |
| **TC_SLOT_GET_01** | Happy Path | Lấy danh sách toàn bộ ca đá sắp xếp theo `startTime` | Đã có nhiều ca đá trong DB | 1. Gửi GET `/api/v1/admin/time-slots` | `activeOnly`: true | - HTTP 200 OK<br>- Danh sách trả về được sắp xếp tăng dần theo `startTime` |
| **TC_SLOT_UPDATE_01** | State Change | Chuyển đổi ca đá từ Giờ thường sang Giờ vàng | Đã có ca Giờ thường | 1. Gửi PUT `/api/v1/admin/time-slots/{id}` với `isPeakHour = true` | `isPeakHour`: true | - HTTP 200 OK<br>- CSDL cập nhật `is_peak_hour = true` |
| **TC_SLOT_DELETE_01** | Happy Path / Soft Delete | Xóa mềm ca đá chưa có đơn đặt trong tương lai | Ca đá chưa có ai đặt | 1. Gửi DELETE `/api/v1/admin/time-slots/{id}`<br>2. Kiểm tra CSDL | `id`: ID ca đá hợp lệ | - HTTP 200 OK<br>- CSDL giữ nguyên bản ghi nhưng đổi `is_active = false` |
| **TC_SLOT_SEC_01** | Security RBAC | Chặn User vai trò `ROLE_CUSTOMER` gọi API quản trị khung giờ | Đăng nhập tài khoản `ROLE_CUSTOMER` | 1. Gửi GET `/api/v1/admin/time-slots`<br>2. Gửi POST `/api/v1/admin/time-slots` | Token role `ROLE_CUSTOMER` | - HTTP 403 Forbidden<br>- Request bị chặn tại Filter Chain |

---

### 3.3. Phân Hệ Tích Hợp Giao Diện Frontend - Backend (FE - BE Integration Checklist) — 6 Kịch Bản UI/UX

| Mã TC | Phân Loại | Mô Tả Kịch Bản Kiểm Thử UI | Tiền Điều Kiện | Các Bước Thực Hiện Trên UI | Dữ Liệu Nhập | Kết Quả Mong Đợi Trên Frontend |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **TC_UI_PITCH_01** | UI/UX & Feedback | Thêm sân bóng và hiển thị Toast thành công trên UI | Admin đang mở Modal "Thêm sân bóng mới" | 1. Nhập tên sân "Sân 5D"<br>2. Chọn loại sân<br>3. Bấm Submit | Form hợp lệ | - Nút Submit hiển thị spinner loading khi đang gọi API<br>- Đóng modal thành công<br>- Bắn thông báo Toast xanh: *"Thêm sân bóng thành công"*<br>- Danh sách bảng sân tự động re-fetch và hiện Sân 5D |
| **TC_UI_PITCH_02** | UI/UX & Validation | Validate form nhập liệu tức thì trên UI | Mở Modal Thêm sân bóng | 1. Để trống tên sân và bấm Lưu | Tên rỗng | - Ô input viền đỏ, hiển thị lỗi ngay dưới ô nhập: *"Tên sân bóng không được để trống"*<br>- Không gửi request API xuống backend |
| **TC_UI_PITCH_03** | UI/UX & Error | Xử lý lỗi trùng tên sân từ backend trả về | Mở Modal Thêm sân | 1. Nhập tên sân đã tồn tại<br>2. Bấm Submit | Tên sân đã có | - Nhận lỗi 400 từ API<br>- Bắn thông báo Toast đỏ hoặc Alert: *"Tên sân bóng đã tồn tại"*<br>- Giữ nguyên form để admin sửa lại, không làm mất dữ liệu đã nhập |
| **TC_UI_SLOT_01** | UI/UX & Feedback | Thêm ca đá Giờ vàng và hiển thị Badge Giờ vàng | Mở Modal Thêm khung giờ | 1. Chọn 17:30 - 19:00<br>2. Bật Switch "Giờ vàng"<br>3. Bấm Lưu | Giờ vàng: ON | - Đóng modal, bắn Toast xanh thành công<br>- Dòng ca đá mới trên bảng hiển thị Badge vàng nổi bật: *"Giờ vàng"* |
| **TC_UI_SLOT_02** | UI/UX & Error | Hiển thị cảnh báo lỗi chồng chéo giờ đá | Đã có ca 17:30 - 19:00 trên bảng | 1. Thêm ca 18:00 - 19:30<br>2. Bấm Lưu | Ca lọt vào khoảng cũ | - Backend trả về mã lỗi 3013<br>- UI hiển thị Toast đỏ cảnh báo rõ ràng: *"Khung giờ bị trùng lặp hoặc chồng chéo với khung giờ khác"* |
| **TC_UI_SLOT_03** | UI/UX & A11y | Modal xác nhận trước khi xóa khung giờ | Đang xem danh sách ca đá | 1. Bấm nút Thùng rác xóa ca đá<br>2. Bấm "Hủy" hoặc "Xác nhận" | N/A | - Hiển thị Popconfirm/Modal xác nhận: *"Bạn có chắc chắn muốn xóa khung giờ này không?"*<br>- Bấm Hủy: Giữ nguyên<br>- Bấm Xác nhận: Gọi DELETE, xóa thành công bắn Toast và làm mờ/ẩn ca đá |

---

## 4. Báo Cáo Xuất Jira / TestRail & Bảng Tiêu Chí Nghiệm Thu (Step 4)

```
┌────────────────────────────────────────────────────────────────────────┐
│  BƯỚC 4: XUẤT BÁO CÁO JIRA/TESTRAIL & QA ACCEPTANCE CHECKLIST          │
└────────────────────────────────────────────────────────────────────────┘
```

### 4.1. Tổng Hợp Số Lượng Test Cases Theo 6 Trục Kiểm Thử
- **Happy Path**: 8 Test Cases (`TC_PITCH_CREATE_01`, `TC_PITCH_GET_01`, `TC_PITCH_GET_02`, `TC_PITCH_UPDATE_01`, `TC_PITCH_DELETE_01`, `TC_SLOT_CREATE_01`, `TC_SLOT_CREATE_02`, `TC_SLOT_GET_01`, `TC_SLOT_DELETE_01`).
- **Negative Cases**: 10 Test Cases (`TC_PITCH_CREATE_02`, `TC_PITCH_CREATE_03`, `TC_PITCH_GET_03`, `TC_PITCH_UPDATE_02`, `TC_SLOT_CREATE_07`, `TC_SLOT_CREATE_08`, `TC_SLOT_CREATE_09`, `TC_SLOT_CREATE_10`, `TC_SLOT_CREATE_11`, `TC_SLOT_CREATE_12`).
- **Boundary Value Analysis (BVA)**: 6 Test Cases (`TC_PITCH_CREATE_04`, `TC_PITCH_CREATE_05`, `TC_PITCH_CREATE_06`, `TC_PITCH_CREATE_07`, `TC_SLOT_CREATE_03`, `TC_SLOT_CREATE_04`, `TC_SLOT_CREATE_05`, `TC_SLOT_CREATE_06`).
- **Edge Cases & State Handling**: 5 Test Cases (`TC_PITCH_CREATE_08`, `TC_PITCH_STATUS_01`, `TC_SLOT_CREATE_13`, `TC_SLOT_UPDATE_01`).
- **Security & Data Integrity (RBAC / Constraints)**: 5 Test Cases (`TC_PITCH_DELETE_02`, `TC_PITCH_SEC_01`, `TC_SLOT_SEC_01`, `TC_SLOT_DELETE_01`).
- **UI/UX & Frontend Integration**: 6 Test Cases (`TC_UI_PITCH_01` ➔ `TC_UI_PITCH_03`, `TC_UI_SLOT_01` ➔ `TC_UI_SLOT_03`).
- **Tổng cộng**: **40 Kịch bản kiểm thử toàn diện** đạt chuẩn quốc tế QA/TestRail.

### 4.2. Tiêu Chuẩn Nghiệm Thu Hoàn Thành (Definition of Done - DoD)
- [ ] Toàn bộ **34 kịch bản kiểm thử tự động Backend - DB** được lập trình và chạy thực tế trên CSDL `fpms_test`.
- [ ] Lệnh kiểm thử `./mvnw test -Dtest=PitchManagementIntegrationTest,TimeSlotManagementIntegrationTest` chạy pass 100% với **0 Errors**, **0 Failures**.
- [ ] Ghi lại đầy đủ bằng chứng đối soát log CSDL và JSON response.
- [ ] Cập nhật kế hoạch thực thi `docs/superpowers/plans/2026-09-27-admin-integration-test-fe-be-db.md`.
