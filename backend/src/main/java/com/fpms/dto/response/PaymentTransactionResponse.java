package com.fpms.dto.response;

import com.fpms.entity.enums.PaymentType;
import com.fpms.entity.enums.TransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin giao dịch thanh toán con gắn liền với đơn đặt sân")
public class PaymentTransactionResponse {

    @Schema(description = "ID bản ghi giao dịch", example = "101")
    private Long id;

    @Schema(description = "Mã giao dịch duy nhất trong hệ thống", example = "TXN_1730018492015_9F2B8D1C")
    private String transactionCode;

    @Schema(description = "Số tiền giao dịch (VNĐ)", example = "150000")
    private BigDecimal amount;

    @Schema(description = "Loại giao dịch (DEPOSIT_30, REMAINING_70, FULL_PAYMENT, REFUND)", example = "DEPOSIT_30")
    private PaymentType paymentType;

    @Schema(description = "Tên phương thức thanh toán", example = "Tiền mặt (CASH)")
    private String paymentMethodName;

    @Schema(description = "Mã code phương thức thanh toán", example = "CASH")
    private String paymentMethodCode;

    @Schema(description = "Trạng thái giao dịch (SUCCESS, PENDING, FAILED)", example = "SUCCESS")
    private TransactionStatus status;

    @Schema(description = "Mã đối soát ngân hàng / cổng thanh toán", example = "VNP14567890")
    private String referenceCode;

    @Schema(description = "Ghi chú giao dịch", example = "Thu tiền cọc tại quầy")
    private String note;

    @Schema(description = "Họ tên nhân viên thu ngân trực tiếp xử lý giao dịch", example = "Nguyễn Văn Lễ Tân")
    private String processedByName;

    @Schema(description = "Thời gian tạo giao dịch")
    private LocalDateTime createdAt;
}
