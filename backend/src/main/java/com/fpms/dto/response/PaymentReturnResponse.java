package com.fpms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentReturnResponse {
    private String status; // SUCCESS, CANCELLED, FAILED, INVALID_SIGNATURE
    private String bookingCode;
    private Long bookingId;
    private BigDecimal amount;
    private String transactionNo;
    private String bankCode;
    private String payDate;
    private String message;
}
