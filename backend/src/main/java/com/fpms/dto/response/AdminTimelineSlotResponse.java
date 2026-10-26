package com.fpms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO dành riêng cho Admin/Nhân viên quản lý ca đá (Timeline Grid).
 * Chứa đầy đủ thông tin đặt sân, khách hàng, chi phí và trạng thái ca đá.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminTimelineSlotResponse {
    private Long pitchId;
    private String pitchName;
    private String pitchTypeName;
    private Long timeSlotId;
    private LocalTime startTime;
    private LocalTime endTime;
    private LocalDate bookingDate;
    private String status; // AVAILABLE, CONFIRMED, IN_PROGRESS, WAITING_CANCELLATION, WAITING_APPROVAL, COMPLETED, MAINTENANCE, PENDING_HOLD

    // Thông tin chi tiết đơn đặt sân (nếu có)
    private Long bookingId;
    private String bookingCode;
    private String guestName;
    private String guestPhone;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;
    private String customerNote;
    private String cancellationReason;
}
