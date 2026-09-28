package com.fpms.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSlotRequest {

    @NotNull(message = "Giờ bắt đầu không được để trống")
    @Schema(type = "string", example = "06:00", description = "Giờ bắt đầu khung giờ (HH:mm)")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "Giờ kết thúc không được để trống")
    @Schema(type = "string", example = "07:30", description = "Giờ kết thúc khung giờ (HH:mm)")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @Schema(description = "Cờ đánh dấu Giờ vàng / Giờ cao điểm (true: Giờ vàng, false: Giờ thường)", example = "false")
    @Builder.Default
    private Boolean isPeakHour = false;
}
