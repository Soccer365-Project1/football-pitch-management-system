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

