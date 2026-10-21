package com.fpms.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCancelRequest {

    @NotBlank(message = "BOOKING_CANCEL_REASON_REQUIRED")
    @Size(max = 500, message = "BOOKING_CANCEL_REASON_TOO_LONG")
    @Schema(description = "Lý do hủy đơn đặt sân", example = "Bận việc đột xuất không thể tham gia")
    private String reason;
}
