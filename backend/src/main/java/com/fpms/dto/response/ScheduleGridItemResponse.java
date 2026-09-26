package com.fpms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleGridItemResponse {
    private Long pitchId;
    private Long timeSlotId;
    private String status;
}
