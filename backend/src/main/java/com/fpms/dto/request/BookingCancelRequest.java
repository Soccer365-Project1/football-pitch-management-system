package com.fpms.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCancelRequest {

    @Schema(description = "Lý do hủy đơn đặt sân", example = "Bận việc đột xuất không thể tham gia")
    private String reason;
}
