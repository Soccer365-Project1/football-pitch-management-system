package com.fpms.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu từ chối yêu cầu xin hủy đơn của khách hàng dành cho Admin/Staff (SRS UC011.3 Luồng thay thế 3a)")
public class AdminBookingRejectCancelRequest {

    @NotBlank(message = "REJECT_CANCEL_REASON_REQUIRED")
    @Size(min = 5, max = 500, message = "REJECT_CANCEL_REASON_LENGTH_INVALID")
    @Schema(description = "Lý do từ chối yêu cầu hủy (bắt buộc từ 5 - 500 ký tự)", example = "Khách báo hủy trước giờ đá dưới 24h, sân đã chuẩn bị nhân sự và mặt sân", requiredMode = Schema.RequiredMode.REQUIRED)
    private String rejectionReason;
}

