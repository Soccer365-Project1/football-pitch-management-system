# Admin Report & Statistics Implementation Plan

- **Jira Task**: `FPMS-156` (Subtask `ST-03` of `FPMS-150` / Sprint 5)
- **Branch**: `feature/FPMS-150-backend-admin-reports`
- **Title**: `ST-03: [Backend] Xây dựng API Báo cáo Thống kê Doanh thu, Đơn hàng, Tần suất sân & Xuất file Excel`
- **Spec / Full Plan**: [`docs/development/user-management-module/03_PLAN_TRIEN_KHAI_ST03_BACKEND_REPORT_STATISTICS.md`](file:///f:/Working/JavaBackend/football-pitch-management-system/docs/development/user-management-module/03_PLAN_TRIEN_KHAI_ST03_BACKEND_REPORT_STATISTICS.md)
- **SRS Reference**: [01. SRS - Đặc tả yêu cầu - Group-05.pdf](../../srs/01.%20SRS%20-%20D?c%20t?%20y%C3%AAu%20c?u%20-%20Group-05.pdf) (Section 3.4.17 UC018.1 & UC018.2)

---

## Implementation Progress

### 1. Dependencies & Error Codes
- [ ] Add `poi-ooxml` (5.2.5) to [`backend/pom.xml`](file:///f:/Working/JavaBackend/football-pitch-management-system/backend/pom.xml).
- [ ] Add error codes 5001–5003 (`INVALID_DATE_RANGE`, `REPORT_DATA_EMPTY`, `EXCEL_EXPORT_FAILED`) to [`ErrorCode.java`](file:///f:/Working/JavaBackend/football-pitch-management-system/backend/src/main/java/com/fpms/exception/ErrorCode.java).

### 2. DTOs (Request / Response)
- [ ] Create `ReportFilterRequest.java` in `com.fpms.dto.request`.
- [ ] Create response DTOs in `com.fpms.dto.response.report`:
  - `DashboardSummaryResponse.java` (4 KPI cards: revenue, bookings, cancellation rate, new customers)
  - `RevenueTrendPointResponse.java` (line chart points)
  - `BookingFrequencyResponse.java` (bar chart frequency by day of week)
  - `RevenueByPitchTypeResponse.java` (donut chart by pitch type)
  - `UserStatisticResponse.java` (system user counts)

### 3. Repository Layer
- [ ] Add aggregate queries to [`BookingRepository.java`](file:///f:/Working/JavaBackend/football-pitch-management-system/backend/src/main/java/com/fpms/repository/BookingRepository.java).
- [ ] Add user count queries to [`UserRepository.java`](file:///f:/Working/JavaBackend/football-pitch-management-system/backend/src/main/java/com/fpms/repository/UserRepository.java).

### 4. Service Layer (TDD)
- [ ] Create `AdminReportService.java` & `AdminReportServiceImpl.java`.
- [ ] Create `ExcelExportService.java` & `ExcelExportServiceImpl.java`.
- [ ] Unit tests: `AdminReportServiceTest.java` & `ExcelExportServiceTest.java`.

### 5. Controller Layer (TDD)
- [ ] Create `AdminReportController.java` (`/api/v1/admin/reports/**`, `@PreAuthorize("hasRole('ADMIN')")`).
- [ ] MockMvc tests: `AdminReportControllerTest.java`.

---

## Verification Plan

### Automated Tests
- [ ] Run maven tests: `./mvnw test "-Dtest=AdminReportControllerTest,AdminReportServiceTest,ExcelExportServiceTest"`
- [ ] Run all backend tests: `./mvnw test`
