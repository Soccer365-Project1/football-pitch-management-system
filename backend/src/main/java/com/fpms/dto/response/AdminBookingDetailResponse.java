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
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO chi tiết đơn đặt sân hiển thị trên Modal chi tiết quản trị")
public class AdminBookingDetailResponse {

    @Schema(description = "ID đơn đặt sân", example = "1")
    private Long id;

    @Schema(description = "Mã đơn đặt sân", example = "#B1730018492015")
    private String bookingCode;

    // 1. Thông tin khách hàng
    @Schema(description = "ID tài khoản khách hàng nếu có", example = "10")
    private Long customerId;

    @Schema(description = "Họ tên khách hàng (khách thành viên hoặc khách vãng lai)", example = "Nguyễn Văn Tuấn")
    private String customerName;

    @Schema(description = "Số điện thoại liên hệ", example = "0987654321")
    private String customerPhone;

    @Schema(description = "Email khách hàng nếu có", example = "tuan.nv@gmail.com")
    private String customerEmail;

    // 2. Nhân viên phụ trách / duyệt đơn
    @Schema(description = "Họ tên nhân viên phụ trách hoặc duyệt đơn", example = "Trần Thị Lễ Tân")
    private String staffName;

    // 3. Thông tin sân & ca đá
    @Schema(description = "Tên sân bóng", example = "Sân A1")
    private String pitchName;

    @Schema(description = "Tên phân loại sân bóng (Sân 5, Sân 7...)", example = "Sân 5 người")
    private String pitchTypeName;

    @Schema(description = "Ngày đá (YYYY-MM-DD)", example = "2026-10-28")
    private LocalDate bookingDate;

    @Schema(description = "Giờ bắt đầu ca đá", example = "17:30:00")
    private LocalTime startTime;

    @Schema(description = "Giờ kết thúc ca đá", example = "19:00:00")
    private LocalTime endTime;

    // 4. Cơ cấu tài chính & thanh toán
    @Schema(description = "Tổng tiền sân bóng", example = "500000")
    private BigDecimal totalPitchAmount;

    @Schema(description = "Tiền đã cọc 30%", example = "150000")
    private BigDecimal depositAmount;

    @Schema(description = "Phụ phí phát sinh", example = "50000")
    private BigDecimal additionalFee;

    @Schema(description = "Tiền còn lại phải thu khi đóng ca", example = "400000")
    private BigDecimal remainingAmount;

    @Schema(description = "Tổng giá trị quyết toán (Tổng tiền sân + Phụ phí)", example = "550000")
    private BigDecimal grandTotal;

    // 5. Trạng thái & ghi chú
    @Schema(description = "Hình thức đặt (ONLINE hoặc AT_COUNTER)", example = "ONLINE")
    private BookingType bookingType;

    @Schema(description = "Trạng thái đơn đặt sân", example = "CONFIRMED")
    private BookingStatus status;

    @Schema(description = "Ghi chú của khách hàng hoặc lý do hủy đơn")
    private String customerNote;

    @Schema(description = "Thời gian tạo đơn")
    private LocalDateTime createdAt;

    @Schema(description = "Thời gian cập nhật gần nhất")
    private LocalDateTime updatedAt;

    // 6. Lịch sử giao dịch thanh toán
    @Schema(description = "Lịch sử các giao dịch thanh toán con gắn liền với đơn")
    private List<PaymentTransactionResponse> transactions;

    /**
     * Tiện ích kiểm tra khách có tài khoản thành viên hay là khách vãng lai.
     */
    public boolean isMember() {
        return customerId != null;
    }
}
