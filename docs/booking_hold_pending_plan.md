# Kế hoạch triển khai luồng giữ chỗ (PENDING_HOLD) trong 10 phút

Tài liệu này mô tả chi tiết các bước cần làm để hoàn thiện luồng giữ sân 10 phút. 
*Lưu ý: Luồng này chỉ dừng lại ở việc tạo đơn `PENDING_HOLD`, hiển thị đếm ngược và tự động hủy sau 10 phút. Phần tích hợp cổng thanh toán (VNPAY/Momo) sẽ được thực hiện ở giai đoạn sau.*

## 1. Backend: Cập nhật API Đặt Sân & Tự động Hủy (Auto Cancel)

### 1.1 Khởi tạo trạng thái `PENDING_HOLD` (API Create Booking)
*   Trong `BookingServiceImpl.java`, khi tạo mới đơn đặt sân (`createBooking`):
    *   Set `status` mặc định là `BookingStatus.PENDING_HOLD`.
    *   Tính toán và set giá trị cho trường `holdExpiresAt = LocalDateTime.now().plusMinutes(10)` (Thời điểm hết hạn giữ chỗ là 10 phút kể từ lúc đặt).
*   API vẫn trả về thông tin `Booking` (bao gồm `holdExpiresAt`) để Frontend có dữ liệu đếm ngược.

### 1.2 Job tự động hủy đơn hết hạn (Auto Cancel Cronjob)
Sử dụng Spring `@Scheduled` để chạy ngầm và quét các đơn chưa thanh toán cọc.
*   **Thêm annotation**: Đảm bảo class chính `BackendApplication` hoặc file cấu hình có `@EnableScheduling`.
*   **Tạo class `BookingCleanupJob.java`**:
    *   Tạo một method chạy định kỳ mỗi 1 phút: `@Scheduled(fixedRate = 60000)` hoặc `@Scheduled(cron = "0 * * * * *")`.
    *   Trong method này, gọi đến `BookingRepository` để query các Booking thỏa mãn điều kiện:
        ```sql
        SELECT * FROM bookings 
        WHERE status = 'PENDING_HOLD' 
        AND hold_expires_at <= CURRENT_TIMESTAMP
        ```
    *   Cập nhật trạng thái các đơn tìm được thành `CANCELLED`.
*   *(Tùy chọn)* Có thể thêm websocket event hoặc email thông báo cho khách hàng là đơn đã bị hủy do hết hạn giữ chỗ.

## 2. Frontend: Đồng bộ UX, Đếm ngược & Xử lý hiển thị

### 2.1 Màn hình Checkout (Trang đặt sân)
*   Khi gọi API `createBooking` thành công, lấy được thông tin đặt sân (bao gồm `bookingId`).
*   Chuyển hướng (Redirect) người dùng sang trang Đơn của tôi (`/my-bookings`).
*   Hiển thị thông báo Toast: *"Giữ chỗ thành công! Vui lòng thanh toán cọc trong vòng 10 phút."*

### 2.2 Màn hình Đơn của tôi (My Bookings)
Tại trang hiển thị chi tiết các đơn (ví dụ `BookingHistory` hoặc `MyBookings`):
*   Đối với các đơn có `status === 'PENDING_HOLD'`:
    *   Sử dụng giá trị `holdExpiresAt` trả về từ API để làm mốc tính thời gian còn lại.
    *   Xây dựng một Component `CountdownTimer` sử dụng `setInterval` (mỗi 1s) để đếm ngược từ thời gian hiện tại đến `holdExpiresAt`.
    *   **Hiển thị:** "Thời gian giữ chỗ còn lại: 09:59".
*   **Nút Hành Động:** Hiển thị nút "Thanh toán cọc". Tạm thời nút này có thể chỉ hiện một Alert "Chức năng thanh toán đang được phát triển" hoặc chuyển sang trang hướng dẫn chuyển khoản thủ công.
*   **Khi hết giờ (Đếm ngược về 0):**
    *   Ẩn nút "Thanh toán cọc".
    *   Có thể tự động gọi lại API fetch danh sách đơn (hoặc fetch chi tiết đơn đó) để cập nhật trạng thái mới nhất từ server (lúc này backend auto-cancel job chắc chắn đã cập nhật thành `CANCELLED`).
    *   Giao diện sẽ tự động đổi màu/trạng thái sang "Đã hủy do quá hạn thanh toán".
