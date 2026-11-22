# Kế hoạch Xây dựng Màn hình Quản lý Giao dịch (Transaction Management)

Tài liệu này xác định kế hoạch phân tích, thiết kế và lộ trình triển khai tính năng **Quản lý Giao dịch Thanh toán** (`PaymentTransaction`) dành cho Quản trị viên (`ROLE_ADMIN`) và Nhân viên thu ngân (`ROLE_STAFF`).

---

## 1. Mục Tiêu & Yêu Cầu Nghiệp Vụ

### 1.1 Mục tiêu
- **Minh bạch tài chính**: Quản lý tập trung toàn bộ các luồng tiền ra/vào hệ thống (tiền cọc 30%, thanh toán nốt 70%, thanh toán toàn bộ 100%, hoàn tiền cọc REFUND).
- **Đối soát nhanh chóng**: Tra cứu và đối soát mã giao dịch (`transactionCode`), mã ngân hàng/VNPAY (`referenceCode`) gắn với từng đơn đặt sân cụ thể (`bookingCode`).
- **Tra cứu linh hoạt**: Áp dụng chuẩn tìm kiếm đa tiêu chí, phân trang động kế thừa từ `BaseSearchRequest`.

### 1.2 Phân quyền người dùng (RBAC)
- `ROLE_ADMIN`: Toàn quyền xem thống kê tổng tiền giao dịch, xem toàn bộ danh sách và chi tiết từng giao dịch.
- `ROLE_STAFF`: Xem danh sách và chi tiết giao dịch để đối soát khi làm ca tại quầy.

---

## 2. Thiết Kế Tầng Backend (Spring Boot)

### 2.1 Cấu trúc Entity Hiện Tại (`PaymentTransaction`)
Bảng `payment_transactions` trong cơ sở dữ liệu đã có sẵn các trường dữ liệu:
- `id` (Long, PK)
- `transaction_code` (String, Unique)
- `booking_id` (FK -> `bookings`)
- `payment_method_id` (FK -> `payment_methods`)
- `processed_by` (FK -> `users`, NULL nếu thanh toán online VNPAY)
- `amount` (BigDecimal, số tiền)
- `payment_type` (Enum: `DEPOSIT_30`, `REMAINING_70`, `FULL_PAYMENT`, `REFUND`)
- `status` (Enum: `PENDING`, `SUCCESS`, `FAILED`)
- `reference_code` (String, mã đối soát VNPAY / Ngân hàng)
- `note` (Text, ghi chú thu/hoàn tiền)
- `created_at` (LocalDateTime)

---

### 2.2 DTO Request & Response

#### A. DTO Tìm kiếm & Phân trang: `TransactionSearchRequest`
Kế thừa trực tiếp từ `BaseSearchRequest` để tái sử dụng chuẩn phân trang (`page`, `pageSize`, `sortBy`, `sortDir`, `keyword`, `searchField`):

```java
package com.fpms.dto.request;

import com.fpms.entity.enums.PaymentMethodCode;
import com.fpms.entity.enums.PaymentType;
import com.fpms.entity.enums.TransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Schema(description = "Yêu cầu tìm kiếm và lọc danh sách giao dịch thanh toán")
public class TransactionSearchRequest extends BaseSearchRequest {

    @Schema(description = "Lọc theo trạng thái giao dịch (SUCCESS, PENDING, FAILED)", example = "SUCCESS")
    private TransactionStatus status;

    @Schema(description = "Lọc theo loại thanh toán (DEPOSIT_30, REMAINING_70, FULL_PAYMENT, REFUND)", example = "DEPOSIT_30")
    private PaymentType paymentType;

    @Schema(description = "Lọc theo mã code phương thức thanh toán (VNPAY, CASH, MOMO)", example = "VNPAY")
    private PaymentMethodCode paymentMethodCode;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Lọc giao dịch từ ngày (YYYY-MM-DD)", example = "2026-11-01")
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Lọc giao dịch đến ngày (YYYY-MM-DD)", example = "2026-11-30")
    private LocalDate toDate;

    @Schema(description = "Số tiền giao dịch tối thiểu", example = "50000")
    private BigDecimal minAmount;

    @Schema(description = "Số tiền giao dịch tối đa", example = "1000000")
    private BigDecimal maxAmount;
}
```

*Các tùy chọn `searchField` hợp lệ:*
- `ALL` (Mặc định): Tìm kiếm gần đúng trên `transactionCode`, `bookingCode`, `referenceCode`, `customerPhone`.
- `TRANSACTION_CODE`: Chỉ tìm theo mã giao dịch hệ thống (VD: `TXN_...`).
- `BOOKING_CODE`: Chỉ tìm theo mã đơn đặt sân liên quan (VD: `#B179...`).
- `REFERENCE_CODE`: Tìm theo mã đối soát ngân hàng / VNPAY (VD: `14567890`).
- `PHONE`: Tìm theo số điện thoại khách hàng.

---

#### B. DTO Danh Sách Tóm Tắt (Get All): `TransactionSummaryResponse`
Được tối ưu nhẹ nhàng để hiển thị bảng dữ liệu quản lý giao dịch:

```java
package com.fpms.dto.response;

import com.fpms.entity.enums.PaymentMethodCode;
import com.fpms.entity.enums.PaymentType;
import com.fpms.entity.enums.TransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin tóm tắt giao dịch thanh toán trong danh sách")
public class TransactionSummaryResponse {

    @Schema(description = "ID bản ghi giao dịch", example = "105")
    private Long id;

    @Schema(description = "Mã giao dịch duy nhất trong hệ thống", example = "TXN_1730018492015_9F2B8D1C")
    private String transactionCode;

    @Schema(description = "ID đơn đặt sân liên kết", example = "52")
    private Long bookingId;

    @Schema(description = "Số tiền giao dịch (VNĐ)", example = "150000")
    private BigDecimal amount;

    @Schema(description = "Mã phương thức thanh toán", example = "VNPAY")
    private PaymentMethodCode paymentMethodCode;

    @Schema(description = "Tên hiển thị phương thức thanh toán", example = "Cổng thanh toán VNPAY")
    private String paymentMethodName;

    @Schema(description = "Loại giao dịch", example = "DEPOSIT_30")
    private PaymentType paymentType;

    @Schema(description = "Trạng thái giao dịch", example = "SUCCESS")
    private TransactionStatus status;

    @Schema(description = "Thời điểm phát sinh giao dịch")
    private LocalDateTime createdAt;
}
```

---

#### C. DTO Chi Tiết Đầy Đủ 1 Bản Ghi (Get Detail): `TransactionDetailResponse`
Chứa đầy đủ thông tin giao dịch, thông tin đối soát, nhân viên xử lý và thông tin tóm tắt của ca đá liên kết:

```java
package com.fpms.dto.response;

import com.fpms.entity.enums.BookingStatus;
import com.fpms.entity.enums.PaymentMethodCode;
import com.fpms.entity.enums.PaymentType;
import com.fpms.entity.enums.TransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Chi tiết toàn diện của một giao dịch thanh toán")
public class TransactionDetailResponse {

    // 1. Thông tin giao dịch
    @Schema(description = "ID bản ghi giao dịch", example = "105")
    private Long id;

    @Schema(description = "Mã giao dịch duy nhất trong hệ thống", example = "TXN_1730018492015_9F2B8D1C")
    private String transactionCode;

    @Schema(description = "Số tiền giao dịch (VNĐ)", example = "150000")
    private BigDecimal amount;

    @Schema(description = "Loại giao dịch (DEPOSIT_30, REMAINING_70, FULL_PAYMENT, REFUND)", example = "DEPOSIT_30")
    private PaymentType paymentType;

    @Schema(description = "Trạng thái giao dịch (SUCCESS, PENDING, FAILED)", example = "SUCCESS")
    private TransactionStatus status;

    @Schema(description = "Mã đối soát ngân hàng / VNPAY / MoMo", example = "VNP14892012")
    private String referenceCode;

    @Schema(description = "Ghi chú giao dịch", example = "Thanh toán cọc 30% qua VNPAY")
    private String note;

    @Schema(description = "Thời điểm tạo giao dịch")
    private LocalDateTime createdAt;

    // 2. Phương thức thanh toán
    @Schema(description = "ID phương thức thanh toán", example = "2")
    private Long paymentMethodId;

    @Schema(description = "Mã code phương thức thanh toán", example = "VNPAY")
    private PaymentMethodCode paymentMethodCode;

    @Schema(description = "Tên phương thức thanh toán", example = "Cổng thanh toán VNPAY")
    private String paymentMethodName;

    // 3. Nhân viên xử lý (nếu có)
    @Schema(description = "ID nhân viên thu ngân xử lý", example = "10")
    private Long processedById;

    @Schema(description = "Họ tên nhân viên thu ngân", example = "Nguyễn Văn Lễ Tân")
    private String processedByName;

    @Schema(description = "Email nhân viên thu ngân", example = "staff@fpms.vn")
    private String processedByEmail;

    // 4. Thông tin đơn đặt sân liên quan (Booking snapshot)
    @Schema(description = "ID đơn đặt sân liên quan", example = "52")
    private Long bookingId;

    @Schema(description = "Mã đơn đặt sân", example = "#B17933786401")
    private String bookingCode;

    @Schema(description = "Trạng thái hiện tại của đơn đặt sân", example = "CONFIRMED")
    private BookingStatus bookingStatus;

    @Schema(description = "Họ tên khách hàng", example = "Phạm Đình Nghĩa")
    private String customerName;

    @Schema(description = "Số điện thoại khách hàng", example = "0987654321")
    private String customerPhone;

    @Schema(description = "Tên sân bóng", example = "Sân 5A")
    private String pitchName;

    @Schema(description = "Ngày đá", example = "2026-11-15")
    private LocalDate bookingDate;

    @Schema(description = "Khung giờ đá", example = "17:30 - 19:00")
    private String timeSlot;

    @Schema(description = "Tổng giá trị đơn đặt sân", example = "300000")
    private BigDecimal bookingTotalAmount;

    @Schema(description = "Tiền đã cọc", example = "90000")
    private BigDecimal bookingDepositAmount;

    @Schema(description = "Tiền còn lại cần thanh toán", example = "210000")
    private BigDecimal bookingRemainingAmount;
}
```

---

### 2.3 JPA Specification: `PaymentTransactionSpecification`
Xây dựng lớp lọc linh hoạt, tự động kết nối các bảng `Booking`, `PaymentMethod`, `User` để lọc theo từ khóa:

```java
public class PaymentTransactionSpecification {

    public static Specification<PaymentTransaction> filterTransactions(TransactionSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Lọc theo trạng thái
            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            // 2. Lọc theo loại thanh toán
            if (request.getPaymentType() != null) {
                predicates.add(cb.equal(root.get("paymentType"), request.getPaymentType()));
            }

            // 3. Lọc theo phương thức thanh toán
            if (request.getPaymentMethodCode() != null) {
                predicates.add(cb.equal(root.get("paymentMethod").get("code"), request.getPaymentMethodCode()));
            }

            // 4. Lọc theo khoảng ngày tạo giao dịch (createdAt)
            if (request.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.getFromDate().atStartOfDay()));
            }
            if (request.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.getToDate().atTime(LocalTime.MAX)));
            }

            // 5. Lọc theo khoảng số tiền
            if (request.getMinAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), request.getMinAmount()));
            }
            if (request.getMaxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), request.getMaxAmount()));
            }

            // 6. Tìm kiếm theo từ khóa & searchField
            String keyword = request.getCleanKeyword();
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                String field = request.getSearchField() != null ? request.getSearchField().toUpperCase() : "ALL";

                Join<PaymentTransaction, Booking> bookingJoin = root.join("booking", JoinType.LEFT);
                Join<Booking, User> customerJoin = bookingJoin.join("customer", JoinType.LEFT);

                switch (field) {
                    case "TRANSACTION_CODE":
                        predicates.add(cb.like(cb.lower(root.get("transactionCode")), pattern));
                        break;
                    case "BOOKING_CODE":
                        predicates.add(cb.like(cb.lower(bookingJoin.get("bookingCode")), pattern));
                        break;
                    case "REFERENCE_CODE":
                        predicates.add(cb.like(cb.lower(root.get("referenceCode")), pattern));
                        break;
                    case "PHONE":
                        predicates.add(cb.or(
                            cb.like(cb.lower(customerJoin.get("phone")), pattern),
                            cb.like(cb.lower(bookingJoin.get("guestPhone")), pattern)
                        ));
                        break;
                    case "ALL":
                    default:
                        predicates.add(cb.or(
                            cb.like(cb.lower(root.get("transactionCode")), pattern),
                            cb.like(cb.lower(bookingJoin.get("bookingCode")), pattern),
                            cb.like(cb.lower(root.get("referenceCode")), pattern),
                            cb.like(cb.lower(customerJoin.get("phone")), pattern),
                            cb.like(cb.lower(bookingJoin.get("guestPhone")), pattern)
                        ));
                        break;
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
```

---

### 2.4 Cập nhật Repository & Service
1. **`PaymentTransactionRepository.java`**:
   - Mở rộng thêm `JpaSpecificationExecutor<PaymentTransaction>`:
     ```java
     public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long>, JpaSpecificationExecutor<PaymentTransaction> {
         Optional<PaymentTransaction> findByTransactionCode(String transactionCode);
         List<PaymentTransaction> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
     }
     ```
2. **`PaymentTransactionService.java`**:
   - Thêm 2 method mới:
     ```java
     PageResponse<TransactionSummaryResponse> getAllTransactions(TransactionSearchRequest request);
     TransactionDetailResponse getTransactionDetail(Long id);
     byte[] exportTransactionsExcel(TransactionSearchRequest request);
     ```
3. **`PaymentTransactionServiceImpl.java`**:
   - Triển khai logic truy vấn phân trang với `Pageable` được sinh từ `request.toPageable()`.
   - Validate `fromDate <= toDate` (ném `INVALID_DATE_RANGE` nếu không hợp lệ).
   - Mapping thực thể sang DTO `TransactionSummaryResponse` và `TransactionDetailResponse`.
   - Xuất toàn bộ danh sách giao dịch thỏa mãn bộ lọc (bỏ phân trang) ra file Excel (.xlsx) với Apache POI.

---

### 2.5 REST API Controller: `AdminTransactionController`
- Base URL: `/api/v1/admin/transactions`
- Bảo vệ: `@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_STAFF')")`

| Method | Endpoint | Mô Tả | Tham Số |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/admin/transactions` | Lấy danh sách giao dịch phân trang & lọc | `@Valid @ModelAttribute TransactionSearchRequest request` |
| `GET` | `/api/v1/admin/transactions/{id}` | Lấy thông tin chi tiết một giao dịch | `@PathVariable Long id` |
| `GET` | `/api/v1/admin/transactions/export-excel` | Xuất toàn bộ giao dịch thỏa mãn bộ lọc ra Excel (.xlsx) | `@Valid @ModelAttribute TransactionSearchRequest request` |

---

## 3. Thiết Kế Giao Diện Frontend (React + Tailwind + Lucide)

### 3.1 Dịch Vụ API: `transactionService.ts`
Tạo file `frontend/src/services/transactionService.ts`:
- Định nghĩa các interface: `TransactionSummaryResponse`, `TransactionDetailResponse`, `TransactionFilterParams`.
- Hàm `getTransactions(params: TransactionFilterParams)`: gọi `GET /api/v1/admin/transactions`.
- Hàm `getTransactionDetail(id: number)`: gọi `GET /api/v1/admin/transactions/${id}`.

---

### 3.2 Màn Hình Quản Lý Giao Dịch: `AdminTransactions.tsx`
Thay thế file placeholder `frontend/src/pages/admin/AdminTransactions.tsx`:

1. **Header & Thẻ KPI Tóm Tắt Nhanh (Top Cards)**:
   - Tổng số giao dịch tìm thấy.
   - Tổng tiền thành công (VNĐ).
   - Số giao dịch thành công / chờ xử lý / thất bại.
2. **Thanh Công Cụ Tìm Kiếm & Bộ Lọc Đa Năng (Hybrid Toolbar)**:
   - **Dropdown Trạng thái** (`status`): Tất cả / Thành công (`SUCCESS`) / Đang chờ (`PENDING`) / Thất bại (`FAILED`).
   - **Dropdown Loại giao dịch** (`paymentType`): Tất cả / Tiền cọc 30% / Thanh toán nốt 70% / Toàn bộ 100% / Hoàn tiền.
   - **Dropdown Phương thức** (`paymentMethodCode`): Tất cả / VNPAY / Tiền mặt (`CASH`).
   - **Bộ lọc ngày đôi (`DateInputWithPicker`)**:
     - "Từ ngày" - "Đến ngày" hỗ trợ gõ phím `dd/mm/yyyy`, mở popup lịch, tự động chặn `Từ ngày > Đến ngày`.
   - **Ô nhập từ khóa + Dropdown chọn trường tìm kiếm (`searchField`)**:
     - Debounce 500ms khi gõ phím, kích hoạt ngay khi nhấn phím `Enter`.
     - Lựa chọn tìm kiếm: *Tất cả*, *Mã giao dịch*, *Mã đơn*, *Mã đối soát*, *SĐT*.
   - Nút "Xóa bộ lọc" (`Clear filter`).
3. **Bảng Danh Sách Giao Dịch (Responsive Table)**:
   - Các cột:
     - `Mã giao dịch` (Font mono, có nút copy nhanh).
     - `Mã đơn đặt` (Link hoặc badge hiển thị `#B...`).
     - `Loại giao dịch` (Badge: Cọc 30%, Thanh toán nốt, Hoàn tiền...).
     - `Phương thức` (Badge kèm logo/icon: VNPAY, Tiền mặt).
     - `Số tiền` (Định dạng tiền tệ VNĐ `150.000 đ`, đổi màu xanh lá nếu thành công, đỏ nếu hoàn tiền).
     - `Trạng thái` (Badge: Xanh lá = SUCCESS, Vàng = PENDING, Đỏ = FAILED).
     - `Người xử lý` (Tên nhân viên hoặc "Cổng trực tuyến").
     - `Thời gian` (Format `dd/MM/yyyy HH:mm`).
     - `Thao tác` (Nút con mắt 👁️ "Xem chi tiết").
4. **Phân Trang Động (Pagination Bar)**:
   - Chọn kích thước trang (`10`, `20`, `50`).
   - Nút Trước (`Previous`), số trang, Nút Sau (`Next`).

---

### 3.3 Modal Xem Chi Tiết Giao Dịch: `TransactionDetailModal.tsx`
Tạo mới file `frontend/src/components/admin/transaction/TransactionDetailModal.tsx`:
- Mở khi người dùng bấm vào hàng hoặc icon con mắt "Xem chi tiết".
- Trình bày dạng 3 khối thông tin khoa học:
  1. **Khối 1: Thông tin Giao dịch & Đối soát**:
     - Mã giao dịch, Mã đối soát ngân hàng (`referenceCode`), Số tiền, Loại giao dịch, Trạng thái, Phương thức, Ghi chú, Thời gian tạo.
  2. **Khối 2: Thông tin Đơn đặt sân liên kết (`Booking Snapshot`)**:
     - Mã đơn `#B...`, Trạng thái đơn, Tên sân bóng, Ngày đá, Khung giờ đá.
     - Tên khách hàng, Số điện thoại.
     - Tổng tiền ca đá, Tiền cọc, Tiền còn lại.
  3. **Khối 3: Thông tin Xử lý (`Staff / System`)**:
     - Người xử lý trực tiếp tại quầy hoặc Cổng VNPAY tự động.

---

## 4. Kế Hoạch & Lộ Trình Triển Khai (Roadmap)

### Giai đoạn 1: Backend DTOs, Specification & Service
- [ ] Tạo `TransactionSearchRequest.java` kế thừa `BaseSearchRequest`.
- [ ] Tạo `TransactionSummaryResponse.java` và `TransactionDetailResponse.java`.
- [ ] Cập nhật `PaymentTransactionRepository` mở rộng `JpaSpecificationExecutor`.
- [ ] Viết `PaymentTransactionSpecification.java` hỗ trợ lọc đa tiêu chí và `searchField`.
- [ ] Khai báo và triển khai `getAllTransactions` và `getTransactionDetail` trong `PaymentTransactionService` & `PaymentTransactionServiceImpl`.
- [ ] Viết `AdminTransactionController.java` exposing API `/api/v1/admin/transactions`.
- [ ] Viết Unit Test cho Service và Controller kiểm thử logic lọc và quyền hạn.

### Giai đoạn 2: Frontend Service & Màn hình Quản lý
- [ ] Tạo `frontend/src/services/transactionService.ts`.
- [ ] Tạo `TransactionDetailModal.tsx` hiển thị chi tiết đối soát và ca đá liên kết.
- [ ] Hoàn thiện `AdminTransactions.tsx` với đầy đủ KPI Cards, Toolbar Filter Hybrid, Date Picker range và Table.
- [ ] Kiểm tra tích hợp router và menu sidebar trong `AdminLayout`.

### Giai đoạn 3: Kiểm Thử & Tối Ưu Hóa
- [ ] Kiểm thử luồng tìm kiếm theo từng `searchField`: Mã giao dịch, Mã đơn, SĐT.
- [ ] Kiểm thử bộ lọc ngày đôi `fromDate` - `toDate` với `DateInputWithPicker`.
- [ ] Kiểm tra phân quyền truy cập: `ROLE_ADMIN`, `ROLE_STAFF`, chặn `ROLE_CUSTOMER`.
- [ ] Đóng gói và cập nhật tài liệu API Swagger.

