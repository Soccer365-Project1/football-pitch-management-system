package com.fpms.dto.response;

import com.fpms.entity.enums.RefundStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCancellationResponse {
    private Long id;
    private Long bookingId;
    private String bookingCode;
    private String reason;
    private BigDecimal refundAmount;
    private RefundStatus refundStatus;
    private LocalDateTime requestedAt;
    private String rejectionReason;
    private LocalDateTime processedAt;
    private String note;
}
