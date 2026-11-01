package com.fpms.dto.request;

import com.fpms.entity.Booking;
import com.fpms.entity.PaymentMethod;
import com.fpms.entity.User;
import com.fpms.entity.enums.PaymentMethodCode;
import com.fpms.entity.enums.PaymentType;
import com.fpms.entity.enums.TransactionStatus;
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
public class TransactionRequest {

    @NotNull(message = "TRANSACTION_BOOKING_REQUIRED")
    private Booking booking;

    private PaymentMethod paymentMethod;

    private PaymentMethodCode paymentMethodCode;

    @NotNull(message = "TRANSACTION_AMOUNT_REQUIRED")
    private BigDecimal amount;

    @NotNull(message = "TRANSACTION_PAYMENT_TYPE_REQUIRED")
    private PaymentType paymentType;

    private TransactionStatus status;

    private User processedBy;

    @Size(max = 1000, message = "TRANSACTION_NOTE_TOO_LONG")
    private String note;
}
