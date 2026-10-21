# Kế hoạch Tích hợp Tìm kiếm & Lọc Đơn Đặt Sân Cá Nhân ở Backend (Cơ chế Hybrid & Tùy chọn Trường Tìm Kiếm)

Tài liệu này xác định kế hoạch chi tiết để chuyển đổi cơ chế lọc danh sách đơn đặt sân cá nhân của khách hàng từ **Lọc cục bộ tại Frontend** sang **Lọc và phân trang trực tiếp tại Backend** bằng `BookingSearchRequest` (kế thừa `BaseSearchRequest`).

Kế hoạch áp dụng mô hình tương tác **Hybrid UX** (Trạng thái lọc ngay, Từ khóa lọc sau 500ms debounce) kèm **Dropdown tùy chọn trường tìm kiếm theo từ khóa**.

---

## 1. Định Hướng Trải Nghiệm Người Dùng (Hybrid UX Interaction)

Theo yêu cầu, hệ thống sẽ kết hợp trải nghiệm tìm kiếm tối ưu nhất:

1. **Lọc theo Trạng thái (`status`) $\rightarrow$ Tìm kiếm tức thì (Instant Filter)**:
   - Người dùng click chọn bất kỳ trạng thái nào (*Tất cả*, *Chờ đặt cọc*, *Chờ duyệt*, *Đã xác nhận*, *Đang sử dụng*, *Hoàn thành*, *Chờ hủy*, *Đã hủy*, *Đã hoàn tiền*) $\rightarrow$ **Hệ thống gọi API lọc và cập nhật dữ liệu ngay lập tức**, không bắt người dùng phải bấm thêm nút phụ.
   - Tự động đặt lại trang về `page = 1`.

2. **Tìm kiếm theo Từ khóa (`keyword`) $\rightarrow$ Debounce 500ms & Hỗ trợ phím Enter**:
   - Khi người dùng gõ vào ô tìm kiếm, hệ thống đợi **500ms** sau khi người dùng dừng gõ mới kích hoạt gọi API (tránh spam request khi đang gõ dở).
   - Nếu người dùng ấn phím **Enter**, hệ thống hủy thời gian chờ 500ms và gửi request tìm kiếm ngay lập tức.
   - Tự động đặt lại trang về `page = 1`.

3. **Chọn trường tìm kiếm theo từ khóa (`searchField`)**:
   - Bên cạnh ô nhập từ khóa, cung cấp 1 Dropdown lựa chọn trường muốn tìm kiếm:
     - **Tất cả các trường** (`ALL` - Mặc định): Tìm trong cả Mã đơn, Tên sân, Số điện thoại.
     - **Mã đơn** (`BOOKING_CODE`): Chỉ tìm kiếm theo mã đơn (ví dụ: `#B1792...`).
     - **Tên sân** (`PITCH_NAME`): Chỉ tìm kiếm theo tên sân bóng (ví dụ: `Sân 5A`, `Sân 7B`).
     - **Số điện thoại** (`PHONE`): Chỉ tìm kiếm theo số điện thoại khách đặt hoặc khách vãng lai.
   - Khi thay đổi trường tìm kiếm: nếu ô từ khóa đang có nội dung thì tự động áp dụng tìm kiếm lại ngay.

4. **Lọc theo khoảng ngày đá (`fromDate`, `toDate`)**:
   - Tự động cập nhật khi người dùng chọn ngày hợp lệ.

---

## 2. Thiết kế Tầng Backend (Spring Boot)

### 2.1 Cập nhật DTO: `BaseSearchRequest` & `BookingSearchRequest`

* **`BaseSearchRequest.java`** (Đã bổ sung `searchField`):
  ```java
  public class BaseSearchRequest {
      private Integer page = 1;
      private Integer pageSize = 10;
      private String keyword;
      private String searchField = "ALL"; // Mặc định là ALL
      private String sortBy = "createdAt";
      private String sortDir = "DESC";
      
      // Helper methods:
      public Pageable toPageable();
      public int getZeroBasedPage();
      public String getCleanKeyword();
  }
  ```

* **`BookingSearchRequest.java`**:
  ```java
  public class BookingSearchRequest extends BaseSearchRequest {
      private BookingStatus status;
      private LocalDate fromDate;
      private LocalDate toDate;
      private Long pitchId;
      private Long pitchTypeId;
      private Long timeSlotId;
      private BookingType bookingType;
      private Long customerId;
  }
  ```

### 2.2 Repository: `BookingRepository.java`
* Mở rộng `BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking>`.

### 2.3 JPA Specification: `BookingSpecification.java`
Xây dựng logic tạo `Predicate` động căn cứ vào `searchField`:

```java
public class BookingSpecification {

    public static Specification<Booking> filterBookings(BookingSearchRequest request, Long customerId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Bảo mật bắt buộc: Đúng khách hàng và chưa xóa mềm
            predicates.add(cb.isFalse(root.get("isDeleted")));
            predicates.add(cb.equal(root.get("customer").get("id"), customerId));

            // 2. Lọc theo trạng thái
            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            // 3. Lọc theo khoảng ngày đá
            if (request.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bookingDate"), request.getFromDate()));
            }
            if (request.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("bookingDate"), request.getToDate()));
            }

            // 4. Tìm kiếm từ khóa theo searchField
            String keyword = request.getCleanKeyword();
            if (keyword != null) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                String field = request.getSearchField() != null ? request.getSearchField().toUpperCase() : "ALL";

                Predicate codeMatch = cb.like(cb.lower(root.get("bookingCode")), pattern);
                Predicate pitchMatch = cb.like(cb.lower(root.get("pitchNameSnapshot")), pattern);
                Predicate phoneMatch = cb.or(
                        cb.like(root.get("guestPhone"), "%" + keyword + "%"),
                        cb.like(root.get("customer").get("phoneNumber"), "%" + keyword + "%")
                );

                switch (field) {
                    case "BOOKING_CODE":
                        predicates.add(codeMatch);
                        break;
                    case "PITCH_NAME":
                        predicates.add(pitchMatch);
                        break;
                    case "PHONE":
                        predicates.add(phoneMatch);
                        break;
                    case "ALL":
                    default:
                        predicates.add(cb.or(codeMatch, pitchMatch, phoneMatch));
                        break;
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
```

### 2.4 Service & Controller Layer
* **`BookingServiceImpl.java`**:
  ```java
  @Override
  public PageResponse<BookingSummaryResponse> getMyBookings(
          BookingSearchRequest request, 
          UserPrincipal currentUser
  ) {
      if (currentUser == null) {
          throw new AppException(ErrorCode.UNAUTHORIZED);
      }
      
      Specification<Booking> spec = BookingSpecification.filterBookings(request, currentUser.getId());
      Pageable pageable = request.toPageable("createdAt", Sort.Direction.DESC);
      Page<Booking> bookingPage = bookingRepository.findAll(spec, pageable);
      
      return PageResponse.from(bookingPage.map(bookingMapper::toBookingSummaryResponse));
  }
  ```
* **`BookingController.java`**:
  ```java
  @GetMapping("/my-bookings")
  public ResponseEntity<ApiResponse<PageResponse<BookingSummaryResponse>>> getMyBookings(
          @AuthenticationPrincipal UserPrincipal userPrincipal,
          @Valid @ModelAttribute BookingSearchRequest request
  ) {
      PageResponse<BookingSummaryResponse> response = bookingService.getMyBookings(request, userPrincipal);
      return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đặt sân thành công", response));
  }
  ```

---

## 3. Thiết kế Tầng Frontend (React + TypeScript)

### 3.1 Dịch Vụ & Kiểu Dữ Liệu (`bookingService.ts`)
```typescript
export type SearchFieldType = 'ALL' | 'BOOKING_CODE' | 'PITCH_NAME' | 'PHONE';

export interface BookingFilterParams {
  status?: string;
  keyword?: string;
  searchField?: SearchFieldType;
  fromDate?: string;
  toDate?: string;
  page?: number;
  pageSize?: number;
  sortBy?: string;
  sortDir?: 'ASC' | 'DESC';
}
```

### 3.2 Giao Diện Thanh Công Cụ [MyBookings.tsx](file:///c:/Users/Admin/Desktop/Football/football-pitch-management-system/frontend/src/pages/MyBookings.tsx)

```
+---------------------------------------------------------------------------------------------------+
|  TOOLBAR TÌM KIẾM & BỘ LỌC HYBRID                                                                 |
+---------------------------------------------------------------------------------------------------+
|  [ Bộ lọc trạng thái: Tất cả v ]          [ Cụm tìm kiếm từ khóa ]               [ Khoảng ngày ]  |
|  - Tất cả                                 +------------------+------------------+ [ Từ ngày     ] |
|  - Chờ đặt cọc                            | Trường tìm kiếm  | Ô nhập từ khóa   | [ Đến ngày    ] |
|  - Chờ duyệt                              | (Tất cả / Mã đơn | (Debounce 500ms) |                 |
|  - Đã xác nhận                            | / Sân / SĐT)     |                  |                 |
|  - Đang sử dụng                           +------------------+------------------+                 |
|  - Hoàn thành                             (Chọn đổi: Lọc ngay - Gõ phím: Chờ 500ms)               |
|  - Chờ hủy / Đã hủy                                                                               |
|  (Click chọn là lọc ngay)                                                                         |
+---------------------------------------------------------------------------------------------------+
|  BẢNG DỮ LIỆU ĐƠN ĐẶT SÂN (Lấy trực tiếp từ Server theo trang)                                    |
+---------------------------------------------------------------------------------------------------+
|  THANH PHÂN TRANG:                                                                                |
|  Hiển thị trang 1 / 4 (Tổng 38 đơn)            [ < Trang trước ]  [ 1 ]  [ 2 ]  [ 3 ]  [ Trang sau > ]|
+---------------------------------------------------------------------------------------------------+
```

### 3.3 Chi Tiết Xử Lý State & Debounce ở Frontend
1. **Quản lý state tìm kiếm**:
   - `status`: String (`""` là tất cả). Thay đổi $\rightarrow$ set `page = 1` $\rightarrow$ trigger API ngay.
   - `searchField`: `'ALL' | 'BOOKING_CODE' | 'PITCH_NAME' | 'PHONE'`. Thay đổi $\rightarrow$ set `page = 1` $\rightarrow$ trigger API ngay.
   - `searchInput`: Nội dung người dùng gõ trong ô input.
   - `debouncedKeyword`: Giá trị từ khóa sau khi debounce 500ms.
   - `page`: Trang hiện tại (1, 2, 3...).
2. **Hook Debounce (500ms)**:
   ```typescript
   useEffect(() => {
     const timer = setTimeout(() => {
       setDebouncedKeyword(searchInput.trim());
       setPage(1); // Reset về trang 1 khi từ khóa thay đổi
     }, 500);

     return () => clearTimeout(timer);
   }, [searchInput]);
   ```
3. **Sự kiện phím Enter**:
   ```typescript
   const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
     if (e.key === 'Enter') {
       setDebouncedKeyword(searchInput.trim());
       setPage(1);
     }
   };
   ```
4. **React Query Caching & Seamless Transition**:
   - Dùng `placeholderData: (prev) => prev` để khi người dùng chuyển trang hoặc đổi filter, bảng cũ vẫn hiển thị mờ nhẹ kèm loader nhỏ, không bị giật nhấp nháy màn hình trắng.
   - Giữ nguyên kết nối **WebSocket**: khi đơn bị hủy hoặc cập nhật trạng thái, React Query tự động refetch theo đúng tiêu chí lọc hiện tại.

---

## 4. Kế Hoạch Triển Khai Từng Bước

### Bước 1: Backend - Specification & Service
- Thêm `JpaSpecificationExecutor<Booking>` vào `BookingRepository.java`.
- Tạo `BookingSpecification.java` hỗ trợ lọc theo `status`, `searchField`, `keyword`, `fromDate`, `toDate`.
- Cập nhật `BookingServiceImpl.getMyBookings` và `BookingController.getMyBookings`.
- Chạy unit tests: `BookingServiceTest`, `BookingControllerTest`.

### Bước 2: Frontend - Service & Query Hook
- Cập nhật `BookingFilterParams` và `SearchFieldType` trong `bookingService.ts`.
- Cập nhật `useMyBookings(params)` trong `useBookingQueries.ts`.

### Bước 3: Frontend - Giao diện `MyBookings.tsx`
- Bỏ các tab nhóm cũ (`ALL`, `UPCOMING`, `HISTORY`).
- Xây dựng component bộ lọc trạng thái (Dropdown/Pills chọn trạng thái, click là đổi ngay).
- Xây dựng cụm tìm kiếm kết hợp: Dropdown chọn `searchField` + Input từ khóa (Debounce 500ms + Enter).
- Bổ sung bộ lọc khoảng ngày (Từ ngày, Đến ngày).
- Thêm thanh phân trang (Pagination Bar) phía dưới bảng.
- Loại bỏ hoàn toàn logic lọc tĩnh `Array.filter` phía client.

### Bước 4: Kiểm thử Toàn diện & Nghiệm thu
- Kiểm thử lọc nhanh trạng thái: click từng trạng thái thấy gọi API ngay lập tức.
- Kiểm thử debounce: gõ liên tục vào ô tìm kiếm, kiểm tra tab Network chỉ bắn 1 request sau 500ms khi ngừng gõ.
- Kiểm thử chọn trường tìm kiếm: chọn `Mã đơn` gõ mã -> tìm đúng; chọn `Tên sân` gõ tên sân -> tìm đúng; chọn `Số điện thoại` -> tìm đúng.
- Kiểm thử phân trang: sang trang 2, 3 mượt mà.
- Kiểm thử realtime: khi có đơn bị hủy/hết hạn, WebSocket báo về, bảng tự động đồng bộ.
