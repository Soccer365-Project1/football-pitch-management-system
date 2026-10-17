# Kế Hoạch Triển Khai: Yêu Cầu Hủy Đặt Sân & Tách Riêng Module BookingCancellation

## 1. Bối Cảnh & Định Hướng Nghiệp Vụ

### 1.1. Nghiệp vụ yêu cầu hủy từ phía Khách Hàng (Customer API)
- Đây là **API phía Khách hàng (User-facing API)** thao tác trên ứng dụng di động / web cá nhân:
  - **Quyền hạn:** **Chỉ kiểm tra đơn có thuộc sở hữu của chính người dùng hiện tại hay không** (`booking.getCustomer().getId().equals(currentUser.getId())`). Loại bỏ logic kiểm tra `ROLE_ADMIN` / `ROLE_STAFF` ở API này.
  - **Điều kiện cho phép hủy:**
    1. Trạng thái đơn phải là `WAITING_APPROVAL` (Chờ duyệt) hoặc `CONFIRMED` (Đã xác nhận).
    2. Thời điểm gửi yêu cầu phải **trước thời gian bắt đầu ca đá** (`LocalDateTime.now().isBefore(bookingStartDateTime)`).
- **Chuyển đổi trạng thái khi yêu cầu hủy:**
  - Khách hàng không thể trực tiếp chuyển đơn sang `CANCELLED` (vì đơn đã có cọc/đã được giữ chỗ, cần Admin/Chủ sân rà soát và thực hiện hoàn tiền).
  - Trạng thái của đơn đặt sân sẽ chuyển sang: **`BookingStatus.WAITING_CANCELLATION`** (*"Khách hàng gửi yêu cầu hủy đơn và xin hoàn cọc"*).
  - Trạng thái hoàn tiền trong bản ghi hủy ban đầu là: **`RefundStatus.PENDING`** (*"Chờ duyệt"*).

### 1.2. Kiến trúc & Thiết kế mã nguồn (Tách riêng Service & Mapper)
- **Không dồn tất cả logic xử lý `BookingCancellation` vào `BookingServiceImpl`** nhằm tuân thủ nguyên lý Đơn trách nhiệm (*Single Responsibility Principle - SRP*).
- Tách riêng thành một module quản lý hủy đơn chuyên biệt:
  - **Repository:** `BookingCancellationRepository`
  - **Mapper:** `BookingCancellationMapper`
  - **Service:** `BookingCancellationService` & `BookingCancellationServiceImpl`
  - **DTOs:** `BookingCancellationResponse` (kèm DTO `BookingCancelRequest` đã tạo)
- `BookingServiceImpl` đóng vai trò điều phối: Validate đơn đặt -> Cập nhật trạng thái Booking sang `WAITING_CANCELLATION` -> Ủy quyền (delegate) tạo bản ghi hủy sang `BookingCancellationService`.

---

## 2. Thiết Kế Kiến Trúc & Các Thành Phần Mới

```
[Client (MyBookings.tsx)] 
       │ 
       ▼ PUT /api/v1/bookings/{id}/cancel
[BookingController]
       │
       ▼
[BookingServiceImpl] ──────────┐
  1. Kiểm tra chính chủ        │
  2. Kiểm tra trạng thái       │
  3. Kiểm tra giờ bắt đầu      │
  4. Set WAITING_CANCELLATION  │ 5. Gọi tạo bản ghi hủy
                               ▼
                   [BookingCancellationService]
                               │
                ┌──────────────┴──────────────┐
                ▼                             ▼
   [BookingCancellationMapper]   [BookingCancellationRepository]
                │                             │
                ▼                             ▼
     Map Entity / Response          Lưu bảng booking_cancellations
```

---

## 3. Chi Tiết Các Thành Phần Cần Triển Khai

### 3.1. DTO: `BookingCancellationResponse`
- **File:** `backend/src/main/java/com/fpms/dto/response/BookingCancellationResponse.java`
- **Mục đích:** Trả về chi tiết bản ghi yêu cầu hủy đơn cho khách hàng / quản trị viên.
- **Các trường:**
  - `Long id`
  - `Long bookingId`
  - `String bookingCode`
  - `String reason`
  - `BigDecimal refundAmount`
  - `RefundStatus refundStatus`
  - `LocalDateTime requestedAt`
  - `String note`

### 3.2. Repository: `BookingCancellationRepository`
- **File:** `backend/src/main/java/com/fpms/repository/BookingCancellationRepository.java`
- **Nội dung:**
  ```java
  package com.fpms.repository;

  import com.fpms.entity.BookingCancellation;
  import org.springframework.data.jpa.repository.JpaRepository;
  import org.springframework.stereotype.Repository;
  import java.util.Optional;

  @Repository
  public interface BookingCancellationRepository extends JpaRepository<BookingCancellation, Long> {
      Optional<BookingCancellation> findByBookingId(Long bookingId);
      boolean existsByBookingId(Long bookingId);
  }
  ```

### 3.3. Mapper: `BookingCancellationMapper`
- **File:** `backend/src/main/java/com/fpms/mapper/BookingCancellationMapper.java`
- **Công nghệ:** MapStruct (đồng bộ với các Mapper hiện có của dự án).
- **Phương thức:**
  ```java
  @Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
  public interface BookingCancellationMapper {

      @Mapping(source = "booking.id", target = "bookingId")
      @Mapping(source = "booking.bookingCode", target = "bookingCode")
      BookingCancellationResponse toResponse(BookingCancellation cancellation);

      @Mapping(target = "id", ignore = true)
      @Mapping(target = "approvedBy", ignore = true)
      @Mapping(target = "refundMethod", ignore = true)
      @Mapping(target = "rejectionReason", ignore = true)
      @Mapping(target = "processedAt", ignore = true)
      @Mapping(target = "note", ignore = true)
      @Mapping(target = "refundStatus", constant = "PENDING")
      @Mapping(target = "requestedAt", expression = "java(java.time.LocalDateTime.now())")
      @Mapping(source = "booking", target = "booking")
      @Mapping(source = "requestedBy", target = "requestedBy")
      @Mapping(source = "reason", target = "reason")
      @Mapping(source = "refundAmount", target = "refundAmount")
      BookingCancellation toEntity(Booking booking, User requestedBy, String reason, BigDecimal refundAmount);
  }
  ```

### 3.4. Service: `BookingCancellationService` & `BookingCancellationServiceImpl`
- **Interface:** `backend/src/main/java/com/fpms/service/BookingCancellationService.java`
  ```java
  public interface BookingCancellationService {
      BookingCancellation createCancellation(Booking booking, User requestedBy, BookingCancelRequest request);
  }
  ```
- **Implementation:** `backend/src/main/java/com/fpms/service/impl/BookingCancellationServiceImpl.java`
  - Tiếp nhận `Booking`, `User requestedBy`, `BookingCancelRequest`.
  - Chuẩn hóa lý do hủy (nếu rỗng mặc định: `"Khách hàng yêu cầu hủy đơn"`).
  - Lấy số tiền hoàn cọc `refundAmount = booking.getDepositAmount() != null ? booking.getDepositAmount() : BigDecimal.ZERO`.
  - Sử dụng `BookingCancellationMapper` để tạo `BookingCancellation`.
  - Lưu vào CSDL qua `BookingCancellationRepository.save(cancellation)`.

### 3.5. Cập nhật `BookingServiceImpl.cancelBooking()`
- **File:** `backend/src/main/java/com/fpms/service/impl/BookingServiceImpl.java`
- **Các điều chỉnh:**
  1. **Bỏ check Role ADMIN/STAFF:** Chỉ cho phép khách hàng hủy đơn của chính mình:
     ```java
     if (booking.getCustomer() == null || !booking.getCustomer().getId().equals(currentUser.getId())) {
         throw new AppException(ErrorCode.UNAUTHORIZED);
     }
     ```
  2. **Kiểm tra trạng thái hợp lệ:** Phải là `WAITING_APPROVAL` hoặc `CONFIRMED`.
  3. **Kiểm tra thời gian:** Phải trước thời điểm bắt đầu ca đá.
  4. **Chuyển trạng thái sang `WAITING_CANCELLATION`:**
     ```java
     booking.setStatus(BookingStatus.WAITING_CANCELLATION);
     ```
  5. **Ủy quyền tạo bản ghi hủy sang `BookingCancellationService`:**
     ```java
     bookingCancellationService.createCancellation(booking, booking.getCustomer(), request);
     ```
  6. **Lưu cập nhật Booking:** `bookingRepository.save(booking)`.
  7. **WebSocket:** Phát sự kiện thông báo trạng thái `WAITING_CANCELLATION` cho bảng lịch/quản trị viên qua `/topic/schedule`.

---

## 4. Kế Hoạch Kiểm Thử (Unit Tests & Integration)

### 4.1. Unit Test cho `BookingCancellationServiceTest` (Mới)
- Kiểm tra việc tạo thành công bản ghi `BookingCancellation` với đúng `reason`, `refundAmount`, `refundStatus = PENDING`.
- Kiểm tra fallback lý do hủy mặc định khi request để trống.

### 4.2. Cập nhật `BookingServiceTest`
- Bỏ các case liên quan đến Admin/Staff hủy đơn tại API này.
- Thêm Mock `BookingCancellationService`.
- Kiểm tra `cancelBooking_Success`:
  - `booking.getStatus()` sau khi hủy phải là `BookingStatus.WAITING_CANCELLATION`.
  - `verify(bookingCancellationService, times(1)).createCancellation(eq(booking), any(), any());`
- Kiểm tra thất bại khi người gọi không phải chủ đơn (`ErrorCode.UNAUTHORIZED`).
- Kiểm tra thất bại khi đơn ở trạng thái khác `WAITING_APPROVAL` / `CONFIRMED` (`ErrorCode.CANNOT_CANCEL_BOOKING_STATUS`).
- Kiểm tra thất bại khi ca đá đã bắt đầu (`ErrorCode.BOOKING_ALREADY_STARTED`).

---

## 5. Danh Sách File Triển Khai

| Thao tác | Tệp tin | Vai trò |
| :--- | :--- | :--- |
| **Tạo mới** | `backend/src/main/java/com/fpms/dto/response/BookingCancellationResponse.java` | DTO trả về thông tin hủy đơn |
| **Tạo mới** | `backend/src/main/java/com/fpms/repository/BookingCancellationRepository.java` | Repository thao tác bảng `booking_cancellations` |
| **Tạo mới** | `backend/src/main/java/com/fpms/mapper/BookingCancellationMapper.java` | MapStruct mapper chuyển đổi Entity/DTO |
| **Tạo mới** | `backend/src/main/java/com/fpms/service/BookingCancellationService.java` | Interface xử lý nghiệp vụ hủy đơn |
| **Tạo mới** | `backend/src/main/java/com/fpms/service/impl/BookingCancellationServiceImpl.java` | Implementation lưu bản ghi hủy |
| **Chỉnh sửa** | `backend/src/main/java/com/fpms/service/impl/BookingServiceImpl.java` | Bỏ check admin, set `WAITING_CANCELLATION`, gọi `BookingCancellationService` |
| **Chỉnh sửa** | `backend/src/test/java/com/fpms/service/BookingServiceTest.java` | Cập nhật unit test theo trạng thái mới và mock service |
| **Tạo mới** | `backend/src/test/java/com/fpms/service/BookingCancellationServiceTest.java` | Unit test cho `BookingCancellationService` |
