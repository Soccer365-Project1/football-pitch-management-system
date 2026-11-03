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

    @Schema(description = "Tổng giá trị tiền sân", example = "300000")
    private BigDecimal bookingTotalAmount;

    @Schema(description = "Phụ phí phát sinh", example = "50000")
    private BigDecimal bookingAdditionalFee;

    @Schema(description = "Tiền đã cọc", example = "90000")
    private BigDecimal bookingDepositAmount;

    @Schema(description = "Tiền còn lại cần thanh toán", example = "210000")
    private BigDecimal bookingRemainingAmount;
}

