package com.fpms.dto.response.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Số liệu tổng quan 4 thẻ KPI Dashboard")
public class DashboardSummaryResponse {

    // Thẻ 1: Doanh thu
    @Schema(description = "Tổng doanh thu kỳ hiện tại (VNĐ)", example = "35000000.00")
    private BigDecimal currentRevenue;

    @Schema(description = "Tổng doanh thu kỳ trước liền kề (VNĐ)", example = "28000000.00")
    private BigDecimal previousRevenue;

    @Schema(description = "Tỷ lệ tăng trưởng doanh thu so với kỳ trước (%)", example = "25.00")
    private Double revenueGrowthRate;

    // Thẻ 2: Tổng số đơn
    @Schema(description = "Tổng số lượt đặt sân trong kỳ", example = "120")
    private Long totalBookings;

    @Schema(description = "Số đơn thành công / đã hoàn tất", example = "105")
    private Long completedBookings;

    @Schema(description = "Số đơn đang xác nhận / chờ đá", example = "10")
    private Long confirmedBookings;

    // Thẻ 3: Tỉ lệ hủy đơn
    @Schema(description = "Số đơn bị hủy", example = "5")
    private Long cancelledBookings;

    @Schema(description = "Tỉ lệ hủy đơn kỳ này (%)", example = "4.17")
    private Double cancellationRate;

    @Schema(description = "Tỉ lệ hủy đơn kỳ trước (%)", example = "6.50")
    private Double previousCancellationRate;

    @Schema(description = "Mức độ thay đổi tỉ lệ hủy so với kỳ trước (% điểm)", example = "-2.33")
    private Double cancellationRateDiff;

    // Thẻ 4: Khách hàng mới & Tỷ lệ quay lại
    @Schema(description = "Số lượng khách hàng mới đăng ký trong kỳ", example = "35")
    private Long newCustomersCount;

    @Schema(description = "Tỷ lệ khách hàng quay lại đặt từ 2 lần trở lên (%)", example = "42.50")
    private Double repeatCustomerRate;
}
