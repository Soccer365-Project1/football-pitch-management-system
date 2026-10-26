package com.fpms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingEventDto {
    private String action;
    private Long pitchId;
    private Long timeSlotId;
    private String date;
    private String status;
}
