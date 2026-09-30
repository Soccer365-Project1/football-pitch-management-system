package com.fpms.dto.response;

import com.fpms.entity.enums.DayType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin ô giá trong ma trận")
public class PriceRateResponse {

    @Schema(description = "ID của ô giá trong CSDL", example = "1")
    private Long id;

    @Schema(description = "Phân loại ngày", example = "WEEKDAY")
    private DayType dayType;

    @Schema(description = "Khung giờ vàng (true) hay thường (false)", example = "false")
    private Boolean isPeakHour;

    @Schema(description = "Mức giá thuê (VNĐ)", example = "200000.00")
    private BigDecimal price;
}
