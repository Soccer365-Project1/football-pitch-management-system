# Kế Hoạch Thực Hiện Subtask ST-05: Integration Test FE - BE - DB (FPMS-103)

> **Kỹ năng chuẩn hóa**: `acceptance-criteria-and-test-design` & `writing-plans`  
> **Dự án**: Football Pitch Management System (FPMS)  
> **Mã công việc (Jira)**: `FPMS-103` (Subtask `ST-05`: *[Testing] Thực hiện Integration Test kết nối FE - BE và nghiệm thu dữ liệu DB*)  
> **Nhánh Git**: `test/FPMS-103-integration-testing`  
> **Đặc tả tiêu chí nghiệm thu đi kèm**: [2026-09-27-admin-integration-test-design.md](../specs/2026-09-27-admin-integration-test-design.md)  
> **Trạng thái**: Đang triển khai thực thi (In-Progress)  

---

## Mục Tiêu & Công Nghệ
- **Mục tiêu**: Xây dựng và thực thi 100% bộ kiểm thử tích hợp tự động kết nối CSDL PostgreSQL thật (`fpms_test`), bao phủ đủ 6 khía cạnh kiểm thử theo chuẩn `acceptance-criteria-and-test-design` cho 2 phân hệ Sân bóng và Khung giờ.
- **Tech Stack**: Spring Boot 3/4, Spring Security, MockMvc, JPA/Hibernate, PostgreSQL, JUnit 5, React 18, TypeScript.

---

## Danh Sách Công Việc Triển Khai Chi Tiết

### Task 1: Khởi Tạo Môi Trường CSDL Test Độc Lập Tự Động (Zero-Config DB)
- [x] **Step 1.1**: Cấu hình CSDL test độc lập trong `backend/src/test/resources/application.properties` trỏ vào `jdbc:postgresql://localhost:5433/fpms_test`.
- [x] **Step 1.2**: Triển khai `AutoCreateTestDatabaseInitializer.java` tự động kết nối qua database `postgres` mặc định để kiểm tra và thực thi `CREATE DATABASE fpms_test` nếu chưa tồn tại.
- [x] **Step 1.3**: Xây dựng `BaseIntegrationTest.java` gắn `@SpringBootTest`, `@AutoConfigureMockMvc`, `@Transactional` và nạp `ContextConfiguration` cùng `ObjectMapper`.

---

### Task 2: Backend — Triển Khai Bộ Test Tích Hợp Quản Lý Sân Bóng (`PitchManagementIntegrationTest`)
*(Tham chiếu chi tiết: `TC_PITCH_CREATE_01` $\rightarrow$ `TC_PITCH_SEC_01` trong Spec)*

**File triển khai**: `backend/src/test/java/com/fpms/integration/PitchManagementIntegrationTest.java`

- [x] **Step 2.1 (Happy Path)**:
  - `TC_PITCH_CREATE_01`: Thêm sân mới tiêu chuẩn, nghiệm thu trạng thái `ACTIVE` trong DB.
  - `TC_PITCH_GET_01`: Tìm kiếm lọc theo keyword, pitchTypeId, status và phân trang.
  - `TC_PITCH_GET_02`: Lấy chi tiết sân bóng theo ID hợp lệ.
  - `TC_PITCH_UPDATE_01`: Cập nhật tên sân và mô tả thành công.
  - `TC_PITCH_DELETE_01`: Xóa cứng sân rác chưa có đơn đặt sân.
- [x] **Step 2.2 (Negative Cases)**:
  - `TC_PITCH_CREATE_02`: Chặn thêm sân trùng tên đã có (không phân biệt hoa thường - mã lỗi 3002).
  - `TC_PITCH_CREATE_03`: Thêm sân với loại sân không tồn tại (mã lỗi 3005).
  - `TC_PITCH_GET_03`: Lấy chi tiết sân với ID không tồn tại (mã lỗi 3001).
  - `TC_PITCH_UPDATE_02`: Cập nhật tên sân trùng với sân khác (mã lỗi 3002).
  - `TC_PITCH_DELETE_02`: Chặn xóa sân bóng đã có lịch sử đơn đặt sân (mã lỗi 3004).
- [x] **Step 2.3 (Boundary Value Analysis - BVA)**:
  - `TC_PITCH_CREATE_04`: Bỏ trống tên sân (Min - 1 $\rightarrow$ 400 Bad Request).
  - `TC_PITCH_CREATE_05`: Tên sân đúng 1 ký tự (Min $\rightarrow$ 201 Created).
  - `TC_PITCH_CREATE_06`: Tên sân đúng 100 ký tự (Max $\rightarrow$ 201 Created).
  - `TC_PITCH_CREATE_07`: Tên sân dài 101 ký tự (Max + 1 $\rightarrow$ 400 Bad Request).
- [x] **Step 2.4 (Edge Cases & State Handling)**:
  - `TC_PITCH_CREATE_08`: Tên sân có khoảng trắng thừa đầu cuối và tiếng Việt có dấu (tự động trim).
  - `TC_PITCH_STATUS_01`: Chuyển đổi trạng thái `ACTIVE` ➔ `MAINTENANCE` ➔ `ACTIVE`.
- [x] **Step 2.5 (Security & RBAC)**:
  - `TC_PITCH_SEC_01`: Chặn người dùng vai trò `ROLE_CUSTOMER` truy cập API quản trị sân bóng (403 Forbidden).

---

### Task 3: Backend — Triển Khai Bộ Test Tích Hợp Quản Lý Khung Giờ (`TimeSlotManagementIntegrationTest`)
*(Tham chiếu chi tiết: `TC_SLOT_CREATE_01` $\rightarrow$ `TC_SLOT_SEC_01` trong Spec)*

**File triển khai**: `backend/src/test/java/com/fpms/integration/TimeSlotManagementIntegrationTest.java`

- [x] **Step 3.1 (Happy Path)**:
  - `TC_SLOT_CREATE_01`: Tạo ca đá Giờ thường hợp lệ (`isPeakHour = false`).
  - `TC_SLOT_CREATE_02`: Tạo ca đá Giờ vàng hợp lệ (`isPeakHour = true`).
  - `TC_SLOT_GET_01`: Lấy toàn bộ danh sách ca đá sắp xếp theo `startTime ASC`.
  - `TC_SLOT_DELETE_01`: Xóa mềm ca đá chưa có đơn đặt trong tương lai (`is_active = false`).
- [x] **Step 3.2 (Negative Cases & Overlap Variants)**:
  - `TC_SLOT_CREATE_07`: Giờ kết thúc trước giờ bắt đầu (mã lỗi 3010).
  - `TC_SLOT_CREATE_08`: Giờ kết thúc bằng giờ bắt đầu (mã lỗi 3010).
  - `TC_SLOT_CREATE_09`: Trùng khít hoàn toàn thời gian với ca cũ (mã lỗi 3013).
  - `TC_SLOT_CREATE_10`: Ca mới nằm lọt thỏm bên trong ca cũ (mã lỗi 3013).
  - `TC_SLOT_CREATE_11`: Ca mới bắt đầu trước và lấn vào ca cũ (mã lỗi 3013).
  - `TC_SLOT_CREATE_12`: Ca mới bắt đầu trong ca cũ và kết thúc sau (mã lỗi 3013).
- [x] **Step 3.3 (Boundary Value Analysis - BVA)**:
  - `TC_SLOT_CREATE_03`: Thời lượng ca đúng bằng 60 phút (Min $\rightarrow$ 201 Created).
  - `TC_SLOT_CREATE_04`: Thời lượng ca đúng bằng 120 phút (Max $\rightarrow$ 201 Created).
  - `TC_SLOT_CREATE_05`: Thời lượng ca 59 phút (Min - 1 $\rightarrow$ 400 Bad Request, mã 3012).
  - `TC_SLOT_CREATE_06`: Thời lượng ca 121 phút (Max + 1 $\rightarrow$ 400 Bad Request, mã 3012).
- [x] **Step 3.4 (Edge Cases & State Handling)**:
  - `TC_SLOT_CREATE_13`: Tiếp giáp biên thời gian (ca mới bắt đầu đúng lúc ca cũ kết thúc $\rightarrow$ Hợp lệ).
  - `TC_SLOT_UPDATE_01`: Cập nhật chuyển ca đá từ Giờ thường sang Giờ vàng.
- [x] **Step 3.5 (Security & RBAC)**:
  - `TC_SLOT_SEC_01`: Chặn người dùng vai trò `ROLE_CUSTOMER` truy cập API quản trị khung giờ (403 Forbidden).

---

### Task 4: Thực Thi Kiểm Thử & Nghiệm Thu Bằng Chứng Terminal Thực Tế
- [ ] **Step 4.1**: Dọn dẹp cache compiler (`backend/target/test-classes`).
- [ ] **Step 4.2**: Chạy lệnh `.\mvnw test -Dtest=PitchManagementIntegrationTest,TimeSlotManagementIntegrationTest` đối soát toàn bộ 34 kịch bản.
- [ ] **Step 4.3**: Chạy toàn bộ test suite dự án `.\mvnw test` để đảm bảo không có regression.
- [ ] **Step 4.4**: Báo cáo nghiệm thu đầy đủ cho người dùng theo quy chuẩn Superpowers.
