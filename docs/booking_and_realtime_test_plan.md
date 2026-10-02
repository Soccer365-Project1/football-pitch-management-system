# Kế hoạch Kiểm thử: Chức năng Đặt Sân và Realtime (Đã cập nhật theo code thực tế)

Tài liệu này trình bày chi tiết kế hoạch viết Unit Test và Integration Test cho tính năng đặt sân (Booking) và realtime dựa vào các luồng đã được implement trong `BookingController` và `BookingServiceImpl`.

*Lưu ý: Chức năng thanh toán hiện chưa có, Booking được set cứng trạng thái là `CONFIRMED` ngay sau khi tạo.*

---

## 1. Unit Tests

### 1.1. Booking Service (`BookingServiceTest`)

Tập trung vào hàm `createBooking` và `getScheduleGrid`.

*   **`createBooking` - Thành công (Khách hàng online):**
    *   *Input:* `BookingCreationRequest` (pitchId, timeSlotId, bookingDate), UserPrincipal với role CUSTOMER.
    *   *Mock:* 
        *   `PitchRepository`, `TimeSlotRepository` trả về dữ liệu hợp lệ.
        *   `BookingRepository.existsBy...` trả về `false` (chưa ai đặt).
        *   `PriceMatrixRepository` trả về một mức giá hợp lệ.
        *   `UserRepository` trả về User tương ứng.
    *   *Expected:* Trả về `BookingResponse` với status `CONFIRMED`, `BookingType` là `ONLINE`. Đảm bảo `TransactionSynchronizationManager` đăng ký thành công một synchronization (để gửi message websocket sau commit).
*   **`createBooking` - Thành công (Nhân viên đặt tại quầy):**
    *   *Input:* `BookingCreationRequest`, UserPrincipal với role STAFF/ADMIN.
    *   *Expected:* Tương tự như trên nhưng `BookingType` là `AT_COUNTER`.
*   **`createBooking` - Lỗi Pitch/TimeSlot không tồn tại:**
    *   *Input:* `pitchId` hoặc `timeSlotId` không tồn tại.
    *   *Expected:* Ném `AppException` với `ErrorCode.PITCH_NOT_FOUND` hoặc `TIME_SLOT_NOT_FOUND`.
*   **`createBooking` - Lỗi Xung đột lịch (Trùng Slot):**
    *   *Mock:* `BookingRepository.existsBy...` trả về `true` (đã có booking với status khác CANCELLED/REFUNDED).
    *   *Expected:* Ném `AppException` với `ErrorCode.BOOKING_TIME_SLOT_ALREADY_BOOKED`.
*   **`getScheduleGrid` - Thành công:**
    *   *Input:* `date` (hiện tại hoặc tương lai), `pitchTypeId`.
    *   *Mock:* `BookingRepository.findOccupyingBookings` và `PriceMatrixRepository.findByDayType`.
    *   *Expected:* Trả về `ScheduleGridResponse` gồm danh sách lưới `bookings` (pitchId, timeSlotId, status) và `prices` tương ứng ngày đó.
*   **`getScheduleGrid` - Lỗi ngày trong quá khứ:**
    *   *Input:* `date` là ngày quá khứ.
    *   *Expected:* Ném `AppException` với `ErrorCode.PAST_DATE_NOT_ALLOWED`.

### 1.2. Booking Controller (`BookingControllerTest`)

*   Sử dụng `@WebMvcTest` kết hợp `MockMvc`.
*   **POST `/api/v1/bookings`:**
    *   Truyền payload JSON hợp lệ -> HTTP 200 OK, response chứa cấu trúc API `{"success": true, "message": "Đặt sân thành công", "data": {...}}`.
    *   Thiếu/Sai cấu trúc payload -> HTTP 400 Bad Request.
*   **GET `/api/v1/bookings/schedule-grid`:**
    *   Truyền `date` hợp lệ -> HTTP 200 OK.

---

## 2. Integration Tests

Mục tiêu kiểm thử hoạt động phối hợp giữa API, Database, và luồng Realtime (WebSocket).

### 2.1. Booking API & Database (`BookingScheduleIntegrationTest`)
*Hiện tại đã có test `getScheduleGrid` (TC-I01, TC-I02) trong `BookingScheduleIntegrationTest`. Cần bổ sung test cho API Đặt sân:*

*   **TC-I03: Integration test Đặt sân thành công (Khách hàng):**
    *   Gọi POST `/api/v1/bookings` với JSON hợp lệ bằng `MockMvc`.
    *   *Expected:* HTTP 200 OK. Kiểm tra trong DB (qua `BookingRepository`) xem bản ghi đã được lưu đúng `CONFIRMED` và `ONLINE` chưa.
*   **TC-I04: Integration test Đặt sân thất bại do trùng lịch:**
    *   Tạo trước 1 Booking cho `pitchId=1`, `timeSlotId=1` trong DB (hoặc gọi API 2 lần).
    *   Gọi POST `/api/v1/bookings` xin đặt đúng slot đó.
    *   *Expected:* Lỗi từ controller với mã lỗi HTTP phù hợp và custom error code là `BOOKING_TIME_SLOT_ALREADY_BOOKED`.

### 2.2. WebSocket Realtime Integration

*   **Test lắng nghe sự kiện `BOOKING_CREATED` trên STOMP client:**
    *   Kết nối STOMP client (sử dụng thư viện test STOMP) vào `ws://localhost:{port}/ws`.
    *   Đăng ký nghe (subscribe) trên topic `/topic/schedule`.
    *   Thực hiện thao tác gọi REST API đặt sân (`POST /api/v1/bookings`) hoặc gọi trực tiếp service.
    *   *Expected:* STOMP client nhận được payload `BookingEventDto` với `action="BOOKING_CREATED"`, chứa đúng `pitchId`, `timeSlotId`, `date` của booking vừa đặt.
    *   *(Note: Service sử dụng `TransactionSynchronization` để gửi tin nhắn sau khi commit, nên khi test qua Service/Repository trực tiếp cần lưu ý thiết lập mock transaction manager, hoặc tốt nhất là test thông qua API full-stack integration test).*

---

## 3. Lộ trình thực hiện

1.  **Hoàn thiện Unit Test cho `BookingService`:** Xây dựng `BookingServiceTest` bao phủ tất cả nhánh logic (`createBooking` thành công, check duplicate slot, bắt exception).
2.  **Cập nhật Unit Test Controller:** Bổ sung mock test POST method trong `BookingControllerTest`.
3.  **Bổ sung Integration Test cho Đặt Sân:** Thêm TC-I03, TC-I04 vào `BookingScheduleIntegrationTest`.
4.  **WebSocket Test (Tuỳ chọn nhưng ưu tiên):** Dựng class test WebSocket để đảm bảo frontend chắc chắn nhận được message mỗi khi DB có commit đặt sân thành công.
