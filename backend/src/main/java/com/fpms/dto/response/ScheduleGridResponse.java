package com.fpms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleGridResponse {
    private List<ScheduleGridItemResponse> bookings;
    private List<PriceItemResponse> prices;
}
