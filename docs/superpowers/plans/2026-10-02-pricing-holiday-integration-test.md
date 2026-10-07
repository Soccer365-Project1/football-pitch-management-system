# [Backend Pricing & Holiday Integration Test] Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Xây dựng và thực thi 100% bộ kiểm thử tích hợp tự động kết nối CSDL PostgreSQL thật (`fpms_test`) bao phủ đủ 6 khía cạnh kiểm thử theo chuẩn `acceptance-criteria-and-test-design` cho 2 phân hệ Quản lý Bảng giá Sân (`PriceMatrix`) và Quản lý Ngày lễ (`Holiday`) thuộc Subtask `FPMS-128` (Jira Story `FPMS-104`).

**Architecture:** Sử dụng Spring Boot Test (`@SpringBootTest`, `@AutoConfigureMockMvc`), kế thừa trực tiếp từ `BaseIntegrationTest`. MockMvc giả lập gọi các HTTP REST endpoints của Admin (`/api/v1/admin/pricing/**` và `/api/v1/admin/holidays/**`), truyền dữ liệu qua Controller -> Service -> Repository -> CSDL PostgreSQL thật `fpms_test`. Dữ liệu tự động Rollback sau mỗi method qua `@Transactional`.

**Tech Stack:** Java 21, Spring Boot 3/4, Spring Security Test (`@WithMockUser`), MockMvc, Hibernate/JPA, PostgreSQL, JUnit 5, AssertJ.

**Spec:** [docs/superpowers/specs/2026-10-02-pricing-holiday-integration-test-design.md](../specs/2026-10-02-pricing-holiday-integration-test-design.md), [docs/PITCH_TIMESLOT_PRICING_HOLIDAY_VALIDATION_SPECIFICATION.md](../../PITCH_TIMESLOT_PRICING_HOLIDAY_VALIDATION_SPECIFICATION.md).

## Global Constraints

- Kế thừa 100% từ `BaseIntegrationTest` để tận dụng cấu hình CSDL `fpms_test` và `@Transactional` rollback tự động.
- Mọi test method đều phải có annotation `@DisplayName("TC_...: [Phân loại] Tên kịch bản")` theo mã test case đã chuẩn hóa trong Spec.
- Mã lỗi nghiệp vụ phản hồi phải khớp chính xác với `com.fpms.exception.ErrorCode` (`3003`, `3034`, `3036`, `3016`, `3032`, `3021`, `3022`, `3023`, `3024`, `3025`).
- Nhánh Git: `test/FPMS-104-integration-testing`.
- Chuẩn commit: `test(integration): [FPMS-104] <nội dung commit>`.

## Review Focus

- **Cơ chế Upsert Bảng giá**: Khi gửi lại mức giá mới cho ô ma trận đã có, hệ thống phải cập nhật tại chỗ (`existing.setPrice(...)`), không làm thay đổi ID bản ghi và không làm đứt gãy khóa ngoại.
- **Ràng buộc giá tối thiểu & tối đa**: Giá 1.000 VNĐ là cận biên dưới hợp lệ; 999 VNĐ hoặc số âm phải ném mã `3032` (`PRICE_INVALID`); vượt quá 100.000.000 VNĐ phải vi phạm `@DecimalMax`.
- **Chống trùng lặp ngày lễ**: Thêm ngày lễ trùng với ngày đã tồn tại trong CSDL phải trả về HTTP 409 Conflict và mã `3022` (`HOLIDAY_DATE_ALREADY_EXISTS`).
- **Data Sanitization**: Tên ngày lễ và mô tả có khoảng trắng đầu/cuối phải được trim sạch sẽ trước khi lưu vào CSDL.
- **Bảo mật phân quyền RBAC**: Các role không phải `ROLE_ADMIN` (như `ROLE_CUSTOMER` hoặc Anonymous) khi gọi các API quản trị này phải nhận phản hồi HTTP 403 Forbidden hoặc 401 Unauthorized.

---

### Task 1: Bộ Kiểm Thử Tích Hợp Quản Lý Bảng Giá Sân (`PriceMatrixManagementIntegrationTest`)

**Files:**
- Create: `backend/src/test/java/com/fpms/integration/PriceMatrixManagementIntegrationTest.java`

**Interfaces:**
- Consumes: `com.fpms.dto.request.SavePitchTypePricingRequest`, `com.fpms.dto.request.PriceRateItemRequest`, `com.fpms.entity.enums.DayType`, `com.fpms.repository.PriceMatrixRepository`, `com.fpms.repository.PitchTypeRepository`
- Endpoints: `PUT /api/v1/admin/pricing/pitch-types/{pitchTypeId}`, `GET /api/v1/admin/pricing/pitch-types/{pitchTypeId}`, `GET /api/v1/admin/pricing/matrix`

- [x] **Step 1: Khởi tạo khung file test và cài đặt dữ liệu mẫu PitchType trong @BeforeEach**
- [x] **Step 2: Viết các kịch bản Happy Path (TC_PRICE_01 đến TC_PRICE_04)**
- [x] **Step 3: Viết các kịch bản Negative Cases (TC_PRICE_05 đến TC_PRICE_08)**
- [x] **Step 4: Viết các kịch bản Boundary Value Analysis (TC_PRICE_09 đến TC_PRICE_13)**
- [x] **Step 5: Viết các kịch bản Security & RBAC (TC_PRICE_14 đến TC_PRICE_15)**
- [x] **Step 6: Chạy kiểm thử PriceMatrixManagementIntegrationTest**
- [x] **Step 7: Commit hoàn tất Task 1**

---

### Task 2: Bộ Kiểm Thử Tích Hợp Quản Lý Ngày Lễ (`HolidayManagementIntegrationTest`)

**Files:**
- Create: `backend/src/test/java/com/fpms/integration/HolidayManagementIntegrationTest.java`

**Interfaces:**
- Consumes: `com.fpms.dto.request.HolidayRequest`, `com.fpms.repository.HolidayRepository`
- Endpoints: `POST /api/v1/admin/holidays`, `GET /api/v1/admin/holidays`, `PUT /api/v1/admin/holidays/{id}`, `DELETE /api/v1/admin/holidays/{id}`

- [x] **Step 1: Khởi tạo file test HolidayManagementIntegrationTest kế thừa BaseIntegrationTest**
- [x] **Step 2: Viết các kịch bản Happy Path (TC_HOLI_01 đến TC_HOLI_04)**
- [x] **Step 3: Viết các kịch bản Negative Cases (TC_HOLI_05 đến TC_HOLI_08)**
- [x] **Step 4: Viết các kịch bản Boundary Value Analysis (TC_HOLI_09 đến TC_HOLI_11)**
- [x] **Step 5: Viết kịch bản Edge Case & Security (TC_HOLI_12 đến TC_HOLI_14)**
- [x] **Step 6: Chạy kiểm thử HolidayManagementIntegrationTest**
- [x] **Step 7: Commit hoàn tất Task 2**

---

### Task 3: Chạy Toàn Bộ Test Suite & Nghiệm Thu Bằng Chứng Thực Tế

- [x] **Step 1: Chạy đồng thời 2 file Integration Test mới tạo**
```bash
.\mvnw.cmd test "-Dtest=PriceMatrixManagementIntegrationTest,HolidayManagementIntegrationTest"
```
Kết quả: 29/29 tests PASS (15 PriceMatrix + 14 Holiday).

- [x] **Step 2: Chạy toàn bộ test suite dự án để đối soát hồi quy**
```bash
.\mvnw.cmd test
```
Kết quả: 233/233 tests PASS, BUILD SUCCESS.

- [x] **Step 3: Báo cáo bằng chứng thực thi đầy đủ cho người dùng theo chuẩn verification-before-completion**
