package com.fpms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceItemResponse {
    private Long pitchTypeId;
    private Boolean isPeakHour;
    private BigDecimal price;
}
