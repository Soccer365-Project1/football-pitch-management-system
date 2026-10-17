package com.fpms.dto.response;

import com.fpms.entity.enums.BookingStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSummaryResponse {
    private Long id;
    private String bookingCode;
    private LocalDate bookingDate;
    private String pitchName;
    private LocalTime startTime;
    private LocalTime endTime;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
}
