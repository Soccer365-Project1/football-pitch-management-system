package com.fpms.dto.request;

import com.fpms.entity.enums.PaymentMethodCode;
import com.fpms.entity.enums.PaymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu tạo đơn đặt sân trực tiếp tại quầy (SRS UC011.1)")
public class CounterBookingCreationRequest {

    @NotNull(message = "PITCH_ID_REQUIRED")
    @Schema(description = "ID sân bóng được chọn", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long pitchId;

    @NotNull(message = "TIME_SLOT_ID_REQUIRED")
    @Schema(description = "ID ca đá được chọn", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long timeSlotId;

    @NotNull(message = "BOOKING_DATE_REQUIRED")
    @FutureOrPresent(message = "BOOKING_DATE_INVALID")
    @Schema(description = "Ngày đá (YYYY-MM-DD)", example = "2026-10-28", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate bookingDate;

    @Schema(description = "ID khách hàng thành viên nếu có tài khoản trong hệ thống", example = "10")
    private Long customerId;

    @NotBlank(message = "GUEST_NAME_REQUIRED")
    @Size(max = 100, message = "GUEST_NAME_TOO_LONG")
    @Schema(description = "Họ tên khách đặt sân", example = "Nguyễn Văn Tuấn", requiredMode = Schema.RequiredMode.REQUIRED)
    private String guestName;

    @NotBlank(message = "GUEST_PHONE_REQUIRED")
    @Pattern(regexp = "^(0[3|5|7|8|9])[0-9]{8}$", message = "GUEST_PHONE_INVALID")
    @Schema(description = "Số điện thoại liên hệ khách đặt sân", example = "0987654321", requiredMode = Schema.RequiredMode.REQUIRED)
    private String guestPhone;

    @Schema(description = "Ghi chú của khách hàng khi đặt sân", example = "Chuẩn bị bóng thi đấu tiêu chuẩn số 5")
    private String customerNote;

    @DecimalMin(value = "0.0", message = "PAID_AMOUNT_INVALID")
    @Schema(description = "Số tiền khách thanh toán trước tại quầy (tùy chọn nếu không dùng paymentType)", example = "300000")
    private BigDecimal paidAmount;

    @Schema(description = "Hình thức thanh toán tại quầy: DEPOSIT_30 (Cọc 30%) hoặc FULL_PAYMENT (Thanh toán 100%)", example = "FULL_PAYMENT")
    private PaymentType paymentType;

    @Schema(description = "Phương thức thanh toán nếu có trả trước (CASH hoặc VIETQR)", example = "CASH")
    private PaymentMethodCode paymentMethod;
}
