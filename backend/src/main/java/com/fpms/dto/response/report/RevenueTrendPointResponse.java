package com.fpms.dto.response.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Điểm dữ liệu biểu đồ đường doanh thu theo ngày")
public class RevenueTrendPointResponse {

    @Schema(description = "Ngày thống kê", example = "2026-08-17")
    private LocalDate date;

    @Schema(description = "Thứ trong tuần (Thứ 2, Thứ 3, ..., Chủ Nhật)", example = "Thứ 2")
    private String dayName;

    @Schema(description = "Doanh thu trong ngày (VNĐ)", example = "4500000.00")
    private BigDecimal revenue;

    @Schema(description = "Số lượng đơn đặt hoàn thành trong ngày", example = "6")
    private Long bookingCount;
}
