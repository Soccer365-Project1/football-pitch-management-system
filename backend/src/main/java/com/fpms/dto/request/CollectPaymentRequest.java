package com.fpms.dto.request;

import com.fpms.entity.enums.PaymentMethodCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu thu tiền còn lại và đóng ca đá (SRS UC011.4)")
public class CollectPaymentRequest {

    @DecimalMin(value = "0.0", message = "ADDITIONAL_FEE_INVALID")
    @Builder.Default
    @Schema(description = "Phụ phí phát sinh (tiền nước uống, thuê bóng, áo tập...)", example = "50000", defaultValue = "0")
    private BigDecimal additionalFee = BigDecimal.ZERO;

    @NotNull(message = "PAYMENT_METHOD_REQUIRED")
    @Schema(description = "Phương thức thu tiền còn lại (CASH, VIETQR...)", example = "CASH", requiredMode = Schema.RequiredMode.REQUIRED)
    private PaymentMethodCode paymentMethod;

    @Schema(description = "Ghi chú thu tiền khi đóng ca", example = "Thu tiền mặt + 5 chai nước suối Lavie")
    private String paymentNote;
}
