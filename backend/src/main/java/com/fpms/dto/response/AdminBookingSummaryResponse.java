package com.fpms.dto.response;

import com.fpms.entity.enums.BookingStatus;
import com.fpms.entity.enums.BookingType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO tóm tắt một đơn đặt sân hiển thị trên bảng quản trị Admin/Staff")
public class AdminBookingSummaryResponse {

    @Schema(description = "ID đơn đặt sân", example = "1")
    private Long id;

    @Schema(description = "Mã đơn đặt sân", example = "#B1730018492015")
    private String bookingCode;

    @Schema(description = "Họ tên khách hàng (khách thành viên hoặc khách vãng lai)", example = "Nguyễn Văn Tuấn")
    private String customerName;

    @Schema(description = "Số điện thoại liên hệ", example = "0987654321")
    private String customerPhone;

    @Schema(description = "Khách đã đăng ký tài khoản hay là khách vãng lai", example = "true")
    private Boolean isMember;

    @Schema(description = "Tên sân bóng", example = "Sân A1")
    private String pitchName;

    @Schema(description = "Tên phân loại sân (Sân 5 người, Sân 7 người...)", example = "Sân 5 người")
    private String pitchTypeName;

    @Schema(description = "Ngày đá (YYYY-MM-DD)", example = "2026-10-28")
    private LocalDate bookingDate;

    @Schema(description = "Giờ bắt đầu ca đá", example = "17:30:00")
    private LocalTime startTime;

    @Schema(description = "Giờ kết thúc ca đá", example = "19:00:00")
    private LocalTime endTime;

    @Schema(description = "Tiền cọc 30% (VNĐ)", example = "150000")
    private BigDecimal depositAmount;

    @Schema(description = "Tiền còn lại 70% (VNĐ)", example = "350000")
    private BigDecimal remainingAmount;

    @Schema(description = "Phụ phí phát sinh (VNĐ)", example = "0")
    private BigDecimal additionalFee;

    @Schema(description = "Tổng giá trị đơn (Tiền sân + Phụ phí)", example = "500000")
    private BigDecimal totalAmount;

    @Schema(description = "Hình thức đặt sân (ONLINE hoặc AT_COUNTER)", example = "AT_COUNTER")
    private BookingType bookingType;

    @Schema(description = "Trạng thái đơn đặt sân", example = "CONFIRMED")
    private BookingStatus status;

    @Schema(description = "Họ tên nhân viên phụ trách/tạo đơn/duyệt đơn", example = "Trần Thị Lễ Tân")
    private String staffName;

    @Schema(description = "Thời gian tạo đơn")
    private LocalDateTime createdAt;
}
