package com.fpms.dto.request;

import com.fpms.entity.enums.PaymentMethodCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCheckOutRequest {

    @NotNull(message = "CHECKOUT_PAYMENT_METHOD_REQUIRED")
    @Schema(description = "Hình thức thanh toán nốt (CASH hoặc BANK_TRANSFER)", example = "CASH")
    private PaymentMethodCode paymentMethod;

    @Size(max = 500, message = "CHECKOUT_NOTE_TOO_LONG")
    @Schema(description = "Ghi chú thu ngân khi trả sân", example = "Khách thanh toán tiền mặt đầy đủ")
    private String note;
}
