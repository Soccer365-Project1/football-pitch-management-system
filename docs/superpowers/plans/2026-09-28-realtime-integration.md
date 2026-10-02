# Kế hoạch Tích hợp Real-time (Thời gian thực) cho Hệ thống Đặt Sân

Để đáp ứng yêu cầu giao diện (như bảng lịch sân) tự động cập nhật ngay lập tức khi có người đặt sân thành công hoặc trạng thái đơn bị hủy mà không cần người dùng F5 tải lại trang, công nghệ phù hợp nhất là **WebSocket (với STOMP)** kết hợp với **React Query** (trên Frontend).

Dưới đây là kế hoạch chi tiết chia làm 2 giai đoạn: Backend và Frontend.

---

## Phần 1: Xây dựng cấu trúc Backend (Spring Boot)

**1. Thêm Dependency**
Bổ sung thư viện hỗ trợ WebSocket vào `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

**2. Cấu hình WebSocket (WebSocketConfig.java)**
*   Đăng ký STOMP endpoint: Mở endpoint `/ws` (có thể kết hợp SockJS để fallback nếu trình duyệt không hỗ trợ).
*   Đăng ký Message Broker: Cấu hình tiền tố `/topic` cho các luồng dữ liệu công khai (dành cho client subscribe).

**3. Gửi thông báo (Broadcast Message)**
*   Inject `SimpMessagingTemplate` vào `BookingServiceImpl`.
*   Mỗi khi một giao dịch booking thành công, hoặc bị hủy, gọi hàm:
    `messagingTemplate.convertAndSend("/topic/schedule", eventPayload);`
*   `eventPayload` sẽ là một DTO nhỏ nhắn gọn nhẹ chứa thông tin tối giản báo hiệu sự thay đổi (VD: `pitchId`, `timeSlotId`, `date`, `status`).

---

## Phần 2: Xử lý ở Frontend (React.js + Vite)

**1. Cài đặt thư viện**
Cài đặt thư viện STOMP client:
```bash
npm install @stomp/stompjs sockjs-client
```

**2. Tạo WebSocket Hook/Service (useScheduleWebSocket.ts)**
*   Tạo một custom hook chịu trách nhiệm connect tới `ws://localhost:8080/ws`.
*   Subscribe vào channel `/topic/schedule`.
*   Lắng nghe các message (sự kiện) gửi từ Backend.

**3. Tích hợp React Query (Tự động cập nhật UI)**
*   Sử dụng `useQueryClient` từ React Query.
*   Mỗi khi nhận được 1 thông báo từ WebSocket qua Hook, thực hiện **Invalidate Query**:
    ```javascript
    queryClient.invalidateQueries({ queryKey: ['scheduleGrid', selectedDate] });
    ```
*   *Tùy chọn nâng cao (Optimistic Update)*: Nếu muốn siêu mượt, thay vì invalidate để fetch lại API, ta có thể inject thẳng data mới nhận được từ WS vào cache của React Query.

---

## 🚀 Các Bước Triển Khai (Roadmap)

### Bước 1: Setup Backend
* Cài thư viện và thiết lập `WebSocketConfig` (đã cho phép CORS).
* Tạo `BookingEventDto`.

### Bước 2: Trigger Event
* Tại hàm `createBooking` (và các hàm update status sau này), gọi emit STOMP event.

### Bước 3: Setup Frontend
* Viết file config STOMP client.
* Tạo Hook gắn vào vòng đời component (chỉ mở connection khi người dùng vào trang Lịch Sân, và đóng connection khi out ra).

### Bước 4: Kiểm thử
* Mở 2 trình duyệt (1 Ẩn danh, 1 Bình thường) đứng ở màn hình Lịch Đặt Sân.
* Đặt sân ở trình duyệt 1 -> Trình duyệt 2 phải thấy ô giờ đó chuyển thành đỏ/đã đặt ngay lập tức.
