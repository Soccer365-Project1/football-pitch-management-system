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
@Schema(description = "Thông tin chi tiết một ô cấu hình giá phẳng")
public class PriceMatrixResponse {

    @Schema(description = "ID ô giá", example = "1")
    private Long id;

    @Schema(description = "ID loại sân", example = "1")
    private Long pitchTypeId;

    @Schema(description = "Tên loại sân", example = "Sân 5 người")
    private String pitchTypeName;

    @Schema(description = "Sức chứa tối đa (người)", example = "10")
    private Integer playerCapacity;

    @Schema(description = "Khung giờ vàng hay thường", example = "false")
    private Boolean isPeakHour;

    @Schema(description = "Phân loại ngày", example = "WEEKDAY")
    private DayType dayType;

    @Schema(description = "Mức giá thuê (VNĐ)", example = "200000.00")
    private BigDecimal price;
}
