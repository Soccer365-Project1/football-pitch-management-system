package com.fpms.dto.response;

import com.fpms.entity.enums.BookingStatus;
import com.fpms.entity.enums.BookingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private Long id;
   
    private String bookingCode;

    private String guestName;

    private String guestPhone;

    private String staffName; 

    private LocalDate bookingDate;

    private BigDecimal priceSnapshot; 

    private LocalTime startTimeSnapshot; 

    private LocalTime endTimeSnapshot; 

    private String pitchNameSnapshot; 

    private BigDecimal totalPitchAmount; 

    private BigDecimal depositAmount; 

    private BigDecimal additionalFee; 

    private BigDecimal remainingAmount; 

    private BookingType bookingType;

    private BookingStatus status;
    
    private LocalDateTime holdExpiresAt; 

    private String customerNote;
}
