package com.fpms.dto.response.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dữ liệu biểu đồ tròn cơ cấu doanh thu theo loại sân")
public class RevenueByPitchTypeResponse {

    @Schema(description = "ID loại sân", example = "1")
    private Long pitchTypeId;

    @Schema(description = "Tên loại sân", example = "Sân 5 người")
    private String pitchTypeName;

    @Schema(description = "Doanh thu đóng góp (VNĐ)", example = "21000000.00")
    private BigDecimal revenue;

    @Schema(description = "Tỷ trọng doanh thu (%)", example = "60.00")
    private Double percentage;

    @Schema(description = "Tổng số ca đặt thành công của loại sân này", example = "70")
    private Long bookingCount;
}
