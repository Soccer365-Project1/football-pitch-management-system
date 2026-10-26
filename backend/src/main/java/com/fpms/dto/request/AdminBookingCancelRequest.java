package com.fpms.dto.request;

import com.fpms.entity.enums.PaymentMethodCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu hủy đơn đặt sân và hoàn cọc dành cho Admin/Staff (SRS UC011.3)")
public class AdminBookingCancelRequest {

    @NotBlank(message = "BOOKING_CANCEL_REASON_REQUIRED")
    @Size(min = 5, max = 500, message = "BOOKING_CANCEL_REASON_LENGTH_INVALID")
    @Schema(description = "Lý do hủy đơn bắt buộc (5 - 500 ký tự)", example = "Sân ngập nước do mưa lớn bất khả kháng", requiredMode = Schema.RequiredMode.REQUIRED)
    private String cancelReason;

    @NotNull(message = "REFUND_METHOD_REQUIRED")
    @Schema(description = "Phương thức hoàn tiền (CASH, VIETQR, hoặc VNPAY nếu hoàn tự động)", example = "CASH", requiredMode = Schema.RequiredMode.REQUIRED)
    private PaymentMethodCode refundMethod;

    @DecimalMin(value = "0.0", message = "REFUND_AMOUNT_INVALID")
    @Schema(description = "Số tiền hoàn cọc (nếu không truyền sẽ mặc định hoàn 100% tiền đã cọc)", example = "150000")
    private BigDecimal refundAmount;
}
