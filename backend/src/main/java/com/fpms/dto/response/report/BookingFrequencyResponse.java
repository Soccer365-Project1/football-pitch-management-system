package com.fpms.dto.response.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dữ liệu biểu đồ cột tần suất đặt sân theo thứ trong tuần")
public class BookingFrequencyResponse {

    @Schema(description = "Thứ tự trong tuần (1 = Thứ 2, ..., 7 = Chủ Nhật)", example = "1")
    private Integer dayOfWeekIndex;

    @Schema(description = "Tên thứ hiển thị", example = "Thứ 2")
    private String dayOfWeekName;

    @Schema(description = "Tổng số lượt đặt sân trong thứ này", example = "28")
    private Long bookingCount;
}
