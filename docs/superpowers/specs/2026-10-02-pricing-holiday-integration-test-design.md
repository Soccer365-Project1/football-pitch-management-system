# ĐẶC TẢ TIÊU CHÍ NGHIỆM THU & THIẾT KẾ KỊCH BẢN KIỂM THỬ TÍCH HỢP (FPMS-104 / FPMS-128)
## Acceptance Criteria & Comprehensive Test Scenarios Specification — Subtask FPMS-128

> **Kỹ năng chuẩn hóa**: `acceptance-criteria-and-test-design`  
> **Dự án**: Football Pitch Management System (FPMS)  
> **Mã công việc (Jira)**: `FPMS-104` (Subtask `FPMS-128`: *[Testing] Integration Test kiểm tra luồng cấu hình giá và ngày lễ*)  
> **Nhánh Git**: `test/FPMS-104-integration-testing`  
> **Kế hoạch thực thi đi kèm**: [2026-10-02-pricing-holiday-integration-test.md](../plans/2026-10-02-pricing-holiday-integration-test.md)  
> **Tài liệu tham chiếu**: [PITCH_TIMESLOT_PRICING_HOLIDAY_VALIDATION_SPECIFICATION.md](../../PITCH_TIMESLOT_PRICING_HOLIDAY_VALIDATION_SPECIFICATION.md)  
> **Trạng thái**: Đã phê duyệt & Sẵn sàng triển khai (Standardized Specification)  
> **Ngày cập nhật**: 02/10/2026  

---

## 1. Phân Tích Phạm Vi & Tác Nhân (Scope & Actors)

```
┌────────────────────────────────────────────────────────────────────────┐
│  BƯỚC 1: PHÂN TÍCH PHẠM VI & TÁC NHÂN (SCOPE & ACTORS)                │
└────────────────────────────────────────────────────────────────────────┘
```

### 1.1. Tác Nhân Hệ Thống (Personas & Roles)
1. **Quản trị viên hệ thống / Chủ sân (`ROLE_ADMIN`)**:
   - Được cấp toàn quyền trên các endpoint `/api/v1/admin/pricing/**` và `/api/v1/admin/holidays/**`.
   - Có thể cấu hình bảng ma trận giá cho từng Loại sân (Sân 5, Sân 7), cập nhật giá theo cơ chế Upsert thông minh, thêm/sửa/xóa ngày lễ.
2. **Khách hàng (`ROLE_CUSTOMER`)**:
   - Không có quyền can thiệp vào các API cấu hình quản trị giá và ngày lễ.
   - Khi cố tình gọi API `/api/v1/admin/pricing/**` hoặc `/api/v1/admin/holidays/**`, hệ thống bắt buộc từ chối với mã phản hồi `403 Forbidden`.
3. **Người dùng chưa xác thực (Anonymous / Unauthenticated)**:
   - Chưa gửi Bearer JWT Token hoặc Token không hợp lệ/hết hạn.
   - Khi gọi API quản trị bị từ chối truy cập với mã `401 Unauthorized` hoặc `403 Forbidden`.

### 1.2. Ranh Giới Phạm Vi (Scope Boundaries)
- **Trong phạm vi (In-Scope - Thuần túy nội bộ phân hệ FPMS-104)**:
  - **Tích hợp Backend & PostgreSQL Test Database**: `MockMvc (HTTP Client)` ➔ `AdminPriceMatrixController` / `AdminHolidayController` ➔ `Service` ➔ `Repository` ➔ `PostgreSQL (fpms_test)`.
  - **Phân hệ Quản lý Bảng giá (`PriceMatrixManagement`)**:
    - Lưu ma trận giá theo loại sân (Upsert thông minh: cập nhật tại chỗ mà không xóa bản ghi, bảo toàn khóa ngoại).
    - Lấy bảng giá theo Loại sân (`pitchTypeId`), lấy toàn bộ ma trận giá hệ thống (`matrix`).
    - Validate giá tối thiểu $\ge 1.000$ VNĐ (`PRICE_INVALID`), tối đa 100.000.000 VNĐ (`@DecimalMax`).
    - Validate danh sách `rates` không rỗng (`3034`), các trường `dayType` (`3036`), `isPeakHour` (`3016`) bắt buộc.
    - Chặn quyền truy cập của khách hàng thường (`403 Forbidden`) và người dùng chưa đăng nhập.
  - **Phân hệ Quản lý Ngày lễ (`HolidayManagement`)**:
    - Thêm ngày lễ mới, chặn trùng ngày (`3022`), lọc danh sách theo năm (`year`).
    - Cập nhật thông tin ngày lễ, tự động `trim()` khoảng trắng thừa trong tên và mô tả.
    - Giới hạn độ dài tên ngày lễ tối đa 150 ký tự (`3025`), tối thiểu 1 ký tự.
    - Xóa ngày lễ và kiểm tra tính toàn vẹn CSDL.
    - Chặn quyền truy cập của khách hàng thường (`403 Forbidden`) và người dùng chưa đăng nhập.
- **Ngoài phạm vi (Out-of-Scope)**:
  - Luồng tạo đơn đặt sân phía người dùng cuối (sẽ kiểm thử toàn diện tại Story Đặt sân).
  - Tích hợp cổng thanh toán trực tuyến bên thứ 3 (VNPAY / MoMo).
  - Giao diện người dùng Frontend (đã kiểm thử tập trung vào Backend API & DB theo chỉ định).

---

## 2. Tiêu Chí Nghiệm Thu (Acceptance Criteria - AC)

```
┌────────────────────────────────────────────────────────────────────────┐
│  BƯỚC 2: SOẠN THẢO TIÊU CHÍ NGHIỆM THU (GHERKIN & BUSINESS RULES)     │
└────────────────────────────────────────────────────────────────────────┘
```

### 2.1. Kịch Bản Hành Vi Chính (Gherkin Format)

#### Phân hệ Bảng giá Sân (Price Matrix Management)
```gherkin
Kịch bản: AC_PRICE_01 - Cấu hình lưu bảng giá cho Loại sân mới và nghiệm thu trong PostgreSQL
Given Admin đã đăng nhập với vai trò ROLE_ADMIN
And CSDL đã có Loại sân "Sân 5 người" (ID = 1)
When Gửi yêu cầu PUT /api/v1/admin/pricing/pitch-types/1 với danh sách 6 ô giá:
  | dayType | isPeakHour | price |
  | WEEKDAY | false      | 200000 |
  | WEEKDAY | true       | 300000 |
  | WEEKEND | false      | 250000 |
  | WEEKEND | true       | 350000 |
  | HOLIDAY | false      | 300000 |
  | HOLIDAY | true       | 400000 |
Then API phản hồi HTTP 200 OK kèm danh sách 6 mức giá
And CSDL bảng "price_matrices" có đủ 6 bản ghi liên kết với pitch_type_id = 1

Kịch bản: AC_PRICE_02 - Cập nhật giá theo cơ chế Upsert tại chỗ (không làm thay đổi ID bản ghi)
Given CSDL đã có cấu hình giá Sân 5, WEEKDAY, isPeakHour = false với giá 200.000 VNĐ (ID = 10)
When Gửi yêu cầu PUT /api/v1/admin/pricing/pitch-types/1 với giá mới là 220.000 VNĐ cho ô đó
Then API phản hồi HTTP 200 OK
And CSDL bảng "price_matrices" cập nhật giá thành 220.000 VNĐ nhưng vẫn giữ nguyên ID = 10

Kịch bản: AC_PRICE_03 - Chặn cấu hình mức giá dưới 1.000 VNĐ
Given Admin đã đăng nhập với vai trò ROLE_ADMIN
When Gửi yêu cầu PUT /api/v1/admin/pricing/pitch-types/1 với price = 999 VNĐ
Then API phản hồi HTTP 400 Bad Request kèm mã lỗi 3032 (PRICE_INVALID)
And Dữ liệu trong CSDL không bị thay đổi
```

#### Phân hệ Ngày lễ (Holiday Management)
```gherkin
Kịch bản: AC_HOLIDAY_01 - Thêm mới ngày lễ hợp lệ vào hệ thống
Given Admin đã đăng nhập với vai trò ROLE_ADMIN
When Gửi yêu cầu POST /api/v1/admin/holidays với:
  holidayDate: "2026-09-02"
  name: "Quốc khánh 2/9"
  description: "Phụ thu ngày lễ toàn quốc"
Then API phản hồi HTTP 201 Created kèm dữ liệu ngày lễ
And CSDL bảng "holidays" lưu đúng bản ghi có ngày 2026-09-02

Kịch bản: AC_HOLIDAY_02 - Chặn thêm ngày lễ trùng ngày đã có
Given CSDL đã tồn tại ngày lễ vào ngày 2026-09-02
When Gửi yêu cầu POST /api/v1/admin/holidays với holidayDate = "2026-09-02"
Then API phản hồi HTTP 409 Conflict kèm mã lỗi 3022 (HOLIDAY_DATE_ALREADY_EXISTS)
And CSDL không phát sinh bản ghi mới

Kịch bản: AC_HOLIDAY_03 - Tự động trim khoảng trắng thừa trong tên và mô tả
Given Admin gửi POST /api/v1/admin/holidays với name = "  Tết Dương Lịch  "
When API xử lý thành công trả về 201 Created
Then CSDL bảng "holidays" lưu name = "Tết Dương Lịch" (đã loại bỏ khoảng trắng 2 đầu)
```

---

## 3. Ma Trận Kịch Bản Kiểm Thử Chi Tiết (Test Matrix - 6 Khía Cạnh)

### 3.1. Phân hệ Quản lý Bảng giá Sân (`PriceMatrixManagementIntegrationTest`)

| Mã TC | Phân loại | Tên kịch bản kiểm thử | Dữ liệu đầu vào | Kết quả mong đợi |
| :--- | :--- | :--- | :--- | :--- |
| **TC_PRICE_01** | Happy Path | Lưu bảng giá 6 ô hoàn chỉnh cho Sân 5 | `pitchTypeId = 1`, 6 rate items hợp lệ | HTTP 200 OK, CSDL có 6 bản ghi liên kết |
| **TC_PRICE_02** | Happy Path | Cơ chế Upsert: Cập nhật đè giá ô đã có | `rates` chứa ô đã có với giá mới | HTTP 200 OK, giá cập nhật tại chỗ, ID không đổi |
| **TC_PRICE_03** | Happy Path | Lấy bảng giá theo Loại sân | `GET /api/v1/admin/pricing/pitch-types/1` | HTTP 200 OK, trả về đủ thông tin loại sân & danh sách rates |
| **TC_PRICE_04** | Happy Path | Lấy toàn bộ ma trận giá hệ thống | `GET /api/v1/admin/pricing/matrix` | HTTP 200 OK, trả về danh sách phân nhóm theo PitchType |
| **TC_PRICE_05** | Negative | Lưu giá với `pitchTypeId` không tồn tại | `pitchTypeId = 99999` | HTTP 404 Not Found, Mã lỗi `3003` (`PITCH_TYPE_NOT_FOUND`) |
| **TC_PRICE_06** | Negative | Danh sách `rates` rỗng | `rates = []` | HTTP 400 Bad Request, Mã lỗi `3034` (`PRICE_ITEMS_REQUIRED`) |
| **TC_PRICE_07** | Negative | Thiếu trường `dayType` trong rate item | `dayType = null` | HTTP 400 Bad Request, Mã lỗi `3036` (`DAY_TYPE_REQUIRED`) |
| **TC_PRICE_08** | Negative | Thiếu trường `isPeakHour` trong rate item | `isPeakHour = null` | HTTP 400 Bad Request, Mã lỗi `3016` (`IS_PEAK_HOUR_REQUIRED`) |
| **TC_PRICE_09** | Boundary | Giá đúng bằng 1.000 VNĐ (Cận biên dưới) | `price = 1000.00` | HTTP 200 OK, lưu thành công |
| **TC_PRICE_10** | Boundary | Giá 999 VNĐ (Dưới cận biên tối thiểu) | `price = 999.00` | HTTP 400 Bad Request, Mã lỗi `3032` (`PRICE_INVALID`) |
| **TC_PRICE_11** | Boundary | Giá số âm | `price = -50000.00` | HTTP 400 Bad Request, Mã lỗi `3032` (`PRICE_INVALID`) |
| **TC_PRICE_12** | Boundary | Giá 100.000.000 VNĐ (Cận biên trên) | `price = 100000000.00` | HTTP 200 OK, lưu thành công |
| **TC_PRICE_13** | Boundary | Giá 100.000.001 VNĐ (Vượt cận biên trên) | `price = 100000001.00` | HTTP 400 Bad Request (Validation `@DecimalMax`) |
| **TC_PRICE_14** | Security | Khách hàng `ROLE_CUSTOMER` gọi API lưu giá | `PUT /api/v1/admin/pricing/pitch-types/1` | HTTP 403 Forbidden |
| **TC_PRICE_15** | Security | Người dùng ẩn danh (Anonymous) gọi API | Không gửi token Bearer | HTTP 401 Unauthorized / 403 Forbidden |

---

### 3.2. Phân hệ Quản lý Ngày lễ (`HolidayManagementIntegrationTest`)

| Mã TC | Phân loại | Tên kịch bản kiểm thử | Dữ liệu đầu vào | Kết quả mong đợi |
| :--- | :--- | :--- | :--- | :--- |
| **TC_HOLI_01** | Happy Path | Thêm ngày lễ mới thành công | `holidayDate: "2026-09-02"`, `name: "Quốc khánh"` | HTTP 201 Created, CSDL lưu đúng ngày |
| **TC_HOLI_02** | Happy Path | Lấy danh sách ngày lễ có lọc theo năm | `GET /api/v1/admin/holidays?year=2026` | HTTP 200 OK, danh sách chỉ chứa ngày thuộc năm 2026 |
| **TC_HOLI_03** | Happy Path | Cập nhật thông tin ngày lễ thành công | Sửa tên thành "Quốc khánh nước CHXHCNVN" | HTTP 200 OK, CSDL cập nhật tên mới |
| **TC_HOLI_04** | Happy Path | Xóa ngày lễ khỏi hệ thống | `DELETE /api/v1/admin/holidays/{id}` | HTTP 200 OK, CSDL không còn bản ghi |
| **TC_HOLI_05** | Negative | Thêm ngày lễ bị trùng ngày đã có | `holidayDate: "2026-09-02"` (đã tồn tại) | HTTP 409 Conflict, Mã lỗi `3022` (`HOLIDAY_DATE_ALREADY_EXISTS`) |
| **TC_HOLI_06** | Negative | Cập nhật/Xóa ngày lễ với ID không tồn tại | `id = 99999` | HTTP 404 Not Found, Mã lỗi `3021` (`HOLIDAY_NOT_FOUND`) |
| **TC_HOLI_07** | Negative | Bỏ trống ngày lễ | `holidayDate = null` | HTTP 400 Bad Request, Mã lỗi `3023` (`HOLIDAY_DATE_REQUIRED`) |
| **TC_HOLI_08** | Negative | Bỏ trống tên ngày lễ | `name = ""` hoặc `"   "` | HTTP 400 Bad Request, Mã lỗi `3024` (`HOLIDAY_NAME_REQUIRED`) |
| **TC_HOLI_09** | Boundary | Tên ngày lễ đúng 1 ký tự (Cận biên dưới) | `name = "A"` | HTTP 201 Created |
| **TC_HOLI_10** | Boundary | Tên ngày lễ đúng 150 ký tự (Cận biên trên) | `name = "A".repeat(150)` | HTTP 201 Created |
| **TC_HOLI_11** | Boundary | Tên ngày lễ 151 ký tự (Vượt cận biên) | `name = "A".repeat(151)` | HTTP 400 Bad Request, Mã lỗi `3025` (`HOLIDAY_NAME_MAX_LENGTH`) |
| **TC_HOLI_12** | Edge Case | Tự động trim khoảng trắng thừa | `name = "  Giải Phóng 30/4  "` | HTTP 201 Created, CSDL lưu "Giải Phóng 30/4" |
| **TC_HOLI_13** | Security | Khách hàng `ROLE_CUSTOMER` cố thêm ngày lễ | `POST /api/v1/admin/holidays` | HTTP 403 Forbidden |
| **TC_HOLI_14** | Security | Người dùng ẩn danh (Anonymous) gọi API ngày lễ | Không gửi token Bearer | HTTP 401 Unauthorized / 403 Forbidden |

---

## 4. Kế Hoạch Nghiệm Thu Bằng Chứng Thực Tế

Toàn bộ các test class trên đều phải được chạy thực tế qua Maven Wrapper:
```bash
.\mvnw.cmd test -Dtest=PriceMatrixManagementIntegrationTest,HolidayManagementIntegrationTest
```
Sau đó chạy toàn bộ test suite dự án để đối soát:
```bash
.\mvnw.cmd test
```
Kết quả phải đạt **100% BUILD SUCCESS** và **0 Failures / 0 Errors**.
