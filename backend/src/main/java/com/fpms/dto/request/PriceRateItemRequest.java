package com.fpms.dto.request;

import com.fpms.entity.enums.DayType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin một ô cấu hình mức giá theo phân loại ngày và khung giờ")
public class PriceRateItemRequest {

    @NotNull(message = "DAY_TYPE_REQUIRED")
    @Schema(description = "Phân loại ngày (WEEKDAY, WEEKEND, HOLIDAY)", example = "WEEKDAY", requiredMode = Schema.RequiredMode.REQUIRED)
    private DayType dayType;

    @NotNull(message = "IS_PEAK_HOUR_REQUIRED")
    @Schema(description = "Có phải khung giờ vàng hay thường (true: giờ vàng, false: giờ thường)", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isPeakHour;

    @NotNull(message = "PRICE_REQUIRED")
    @DecimalMin(value = "1000", message = "PRICE_INVALID")
    @Schema(description = "Mức giá thuê (VNĐ, tối thiểu 1.000)", example = "200000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;
}
