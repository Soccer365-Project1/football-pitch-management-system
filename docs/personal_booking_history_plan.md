# Kế hoạch triển khai chức năng Xem lịch sử đặt sân cá nhân (Personal Booking History)

Tài liệu này mô tả chi tiết thiết kế và kế hoạch xây dựng tính năng xem lịch sử đặt sân cá nhân dành cho khách hàng trong hệ thống FPMS (Football Pitch Management System). 

---

## 1. Mục tiêu & Nguyên tắc bảo mật (Security by Design)

* **Xác thực qua JWT Token**:
  * Người dùng bắt buộc phải đăng nhập. Request gửi lên bắt buộc có header `Authorization: Bearer <token>`.
  * Tuyệt đối **không nhận `userId` hay `customerId`** từ query param hoặc request body. Backend tự động trích xuất thông tin người dùng từ `SecurityContext` thông qua `@AuthenticationPrincipal UserPrincipal userPrincipal`.
  * Ngăn chặn triệt để lỗ hổng **IDOR (Insecure Direct Object Reference)** — Người dùng chỉ xem được các đơn do chính tài khoản của mình tạo.
* **Bộ lọc & Phân trang**:
  * Hỗ trợ phân trang chuẩn qua `PageResponse<T>` (`page`, `size`).
  * Lọc linh hoạt theo trạng thái: `status` (`ALL`, `PENDING_HOLD`, `CONFIRMED`, `COMPLETED`, `CANCELLED`, `REFUNDED`).
  * Lọc theo khoảng thời gian: `fromDate`, `toDate`.
  * Sắp xếp mặc định: Mới nhất lên đầu (`createdAt DESC` hoặc `bookingDate DESC, startTimeSnapshot DESC`).
* **Chi tiết & Hành động nhanh**:
  * Xem chi tiết từng đơn (mã đơn, sân, khung giờ, lịch sử thanh toán, ghi chú).
  * Hỗ trợ thanh toán tiếp cọc đối với các đơn `PENDING_HOLD` còn trong thời hạn 10 phút tạm giữ.

---

## 2. Thiết kế tầng Backend (Spring Boot)

### 2.1 Repository Layer (`BookingRepository.java`)
Bổ sung các phương thức truy vấn tối ưu và bảo đảm tính toàn vẹn dữ liệu:

1. **Truy vấn danh sách đơn phân trang kèm bộ lọc**:
   ```java
   @Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId " +
          "AND b.isDeleted = false " +
          "AND (:status IS NULL OR b.status = :status) " +
          "AND (:fromDate IS NULL OR b.bookingDate >= :fromDate) " +
          "AND (:toDate IS NULL OR b.bookingDate <= :toDate)")
   Page<Booking> findMyBookings(
           @Param("customerId") Long customerId,
           @Param("status") BookingStatus status,
           @Param("fromDate") LocalDate fromDate,
           @Param("toDate") LocalDate toDate,
           Pageable pageable
   );
   ```
2. **Truy vấn chi tiết 1 đơn thuộc sở hữu của người dùng**:
   ```java
   Optional<Booking> findByIdAndCustomerIdAndIsDeletedFalse(Long id, Long customerId);
   ```

---

### 2.2 DTO & Mapper Layer (`BookingResponse.java`, `BookingMapper.java`)
- Bổ sung trường `createdAt` vào `BookingResponse` để hiển thị thời điểm tạo đơn.
- Bổ sung thông tin loại sân (`pitchTypeName`), tên cơ sở/địa điểm nếu có.
- Cập nhật MapStruct trong `BookingMapper`:
  ```java
  @Mapping(source = "staff.fullName", target = "staffName")
  @Mapping(source = "createdAt", target = "createdAt")
  BookingResponse toBookingResponse(Booking booking);
  ```

---

### 2.3 Service Layer (`BookingService.java` & `BookingServiceImpl.java`)

1. **Định nghĩa Interface (`BookingService.java`)**:
   ```java
   PageResponse<BookingResponse> getMyBookings(
           UserPrincipal currentUser,
           BookingStatus status,
           LocalDate fromDate,
           LocalDate toDate,
           int page,
           int size
   );

   BookingResponse getMyBookingDetail(Long bookingId, UserPrincipal currentUser);
   ```

2. **Cài đặt Logic (`BookingServiceImpl.java`)**:
   * **Kiểm tra xác thực**:
     ```java
     if (currentUser == null) {
         throw new AppException(ErrorCode.UNAUTHORIZED);
     }
     ```
   * **Xử lý phân trang & sắp xếp**:
     * Khởi tạo `Pageable`: `PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"))`.
     * Gọi `bookingRepository.findMyBookings(...)`.
     * Chuyển đổi `Page<Booking>` sang `PageResponse<BookingResponse>` thông qua `bookingMapper.toBookingResponse()`.
   * **Lấy chi tiết đơn**:
     * Gọi `bookingRepository.findByIdAndCustomerIdAndIsDeletedFalse(bookingId, currentUser.getId())`.
     * Nếu không tìm thấy, ném ngoại lệ `new AppException(ErrorCode.BOOKING_NOT_FOUND)`.

---

### 2.4 Controller Layer (`BookingController.java`)

Bổ sung 2 endpoint mới, áp dụng `@PreAuthorize("isAuthenticated()")` và Swagger OpenAPI:

```java
@Operation(summary = "Lấy lịch sử đặt sân cá nhân", description = "Lấy danh sách các đơn đặt sân của tài khoản đang đăng nhập từ JWT Token")
@GetMapping("/my-bookings")
public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getMyBookings(
        @AuthenticationPrincipal UserPrincipal userPrincipal,
        @RequestParam(required = false) BookingStatus status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size
) {
    PageResponse<BookingResponse> response = bookingService.getMyBookings(
            userPrincipal, status, fromDate, toDate, page, size
    );
    return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử đặt sân thành công", response));
}

@Operation(summary = "Chi tiết đơn đặt sân cá nhân", description = "Xem chi tiết một đơn đặt sân của tài khoản đang đăng nhập")
@GetMapping("/my-bookings/{id}")
public ResponseEntity<ApiResponse<BookingResponse>> getMyBookingDetail(
        @PathVariable Long id,
        @AuthenticationPrincipal UserPrincipal userPrincipal
) {
    BookingResponse response = bookingService.getMyBookingDetail(id, userPrincipal);
    return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chi tiết đơn thành công", response));
}
```

---

## 3. Thiết kế tầng Frontend (React + Vite)

### 3.1 Service Layer (`frontend/src/services/bookingService.ts`)
Bổ sung các kiểu dữ liệu và hàm gọi API:
```typescript
export interface BookingFilterParams {
  status?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

export const bookingService = {
  // ... các phương thức hiện có ...

  getMyBookings: async (params?: BookingFilterParams) => {
    const response = await api.get<{ code: number; message: string; data: PageResponse<BookingResponse> }>(
      '/bookings/my-bookings',
      { params }
    );
    return response.data;
  },

  getMyBookingDetail: async (id: number) => {
    const response = await api.get<{ code: number; message: string; data: BookingResponse }>(
      `/bookings/my-bookings/${id}`
    );
    return response.data;
  }
};
```

---

### 3.2 Màn hình Lịch đặt của tôi (`frontend/src/pages/MyBookings.tsx`)

Nâng cấp trang từ placeholder hiện tại thành giao diện hoàn thiện với các khối chức năng:

1. **Bộ lọc Trạng thái (Tabs/Badges)**:
   * `Tất cả` | `Chờ cọc (PENDING_HOLD)` | `Đã xác nhận (CONFIRMED)` | `Hoàn thành (COMPLETED)` | `Đã hủy (CANCELLED)`.
   * Hỗ trợ lọc theo khoảng ngày đặt sân.
2. **Danh sách thẻ đơn (Booking Cards)**:
   * **Header Thẻ**: Mã đơn hàng `#B...`, Ngày tạo đơn, Huy hiệu trạng thái (Badge màu: Xanh dương cho `PENDING_HOLD`, Xanh lá cho `CONFIRMED`, Xám/Đỏ cho `CANCELLED`).
   * **Nội dung thẻ**:
     * Tên sân & Loại sân (`Sân 5A - Sân cỏ nhân tạo 5 người`).
     * Ngày thi đấu & Khung giờ (`08:00 - 09:30`).
     * Tiền cọc đã trả (`depositAmount`), Số tiền còn lại thu tại sân (`remainingAmount`).
     * Bộ đếm ngược 10 phút (`CountdownTimer`) nếu đơn đang ở trạng thái `PENDING_HOLD`.
   * **Hành động (Action Buttons)**:
     * Nút **"Xem chi tiết"**: Mở Modal xem đầy đủ thông tin hóa đơn.
     * Nút **"Thanh toán ngay"**: Dẫn link thanh toán VNPAY nếu đơn `PENDING_HOLD` chưa hết hạn.
3. **Phân trang (Pagination Controls)**:
   * Điều hướng trang trước / trang sau, chọn số trang, hiển thị tổng số kết quả.
4. **Trạng thái trống (Empty State)**:
   * Khi chưa có đơn: Hiển thị hình ảnh minh họa thân thiện cùng nút bấm *"Đặt sân ngay"* chuyển hướng tới trang chọn sân.

---

## 4. Kế hoạch kiểm thử (Test Plan)

### 4.1 Unit Test Backend (`BookingServiceTest.java`)
- `getMyBookings_Success`: Kiểm tra phân trang và lọc dữ liệu thành công của đúng `customerId`.
- `getMyBookings_Unauthenticated`: Kiểm tra trả về lỗi `UNAUTHORIZED` khi `userPrincipal == null`.
- `getMyBookingDetail_Success`: Kiểm tra lấy đúng thông tin đơn khi `id` và `customerId` trùng khớp.
- `getMyBookingDetail_NotOwner`: Kiểm tra ném lỗi `BOOKING_NOT_FOUND` khi đơn thuộc về người dùng khác.

### 4.2 Integration & Edge Cases
- Thử nghiệm gọi API khi Token hết hạn hoặc không truyền Token -> Trả về `401 Unauthorized`.
- Người dùng A cố tình truyền ID đơn của người dùng B vào endpoint chi tiết -> Nhận lỗi `404 Booking Not Found` (không làm lộ sự tồn tại của đơn hàng người khác).
- Kiểm tra tính đúng đắn của phân trang khi số lượng đơn vượt quá `pageSize`.

---

## 5. Lộ trình thực hiện từng bước

| Giai đoạn | Nội dung công việc | File liên quan |
| :--- | :--- | :--- |
| **Giai đoạn 1** | Cập nhật Repository & DTO & Mapper | `BookingRepository.java`, `BookingResponse.java`, `BookingMapper.java` |
| **Giai đoạn 2** | Triển khai Service & Viết Unit Test | `BookingService.java`, `BookingServiceImpl.java`, `BookingServiceTest.java` |
| **Giai đoạn 3** | Cài đặt Controller & OpenAPI docs | `BookingController.java` |
| **Giai đoạn 4** | Cập nhật Frontend Service | `frontend/src/services/bookingService.ts` |
| **Giai đoạn 5** | Xây dựng UI `MyBookings.tsx` & Modal chi tiết | `frontend/src/pages/MyBookings.tsx` |
| **Giai đoạn 6** | Kiểm thử E2E tích hợp & Rà soát | Frontend + Backend toàn diện |
