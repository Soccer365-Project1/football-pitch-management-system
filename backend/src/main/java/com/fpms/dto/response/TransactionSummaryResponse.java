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

    @Schema(description = "Mã code phương thức thanh toán", example = "VNPAY")
    private PaymentMethodCode paymentMethodCode;

    @Schema(description = "Tên hiển thị phương thức thanh toán", example = "Cổng thanh toán VNPAY")
    private String paymentMethodName;

    @Schema(description = "Loại giao dịch (DEPOSIT_30, REMAINING_70, FULL_PAYMENT, REFUND)", example = "DEPOSIT_30")
    private PaymentType paymentType;

    @Schema(description = "Trạng thái giao dịch (SUCCESS, PENDING, FAILED)", example = "SUCCESS")
    private TransactionStatus status;

    @Schema(description = "Thời điểm phát sinh giao dịch")
    private LocalDateTime createdAt;
}

