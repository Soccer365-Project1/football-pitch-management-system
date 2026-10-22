package com.fpms.dto.request;

import com.fpms.entity.enums.BookingStatus;
import com.fpms.entity.enums.BookingType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * DTO tìm kiếm và lọc danh sách đơn đặt sân (Booking), kế thừa các trường phân trang và từ khóa từ BaseSearchRequest.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Yêu cầu tìm kiếm và lọc danh sách đơn đặt sân")
public class BookingSearchRequest extends BaseSearchRequest {

    @Schema(description = "Lọc theo mã đơn đặt sân chính xác", example = "#B179257908")
    private String bookingCode;

    @Schema(description = "Lọc theo trạng thái đơn đặt sân", example = "CONFIRMED")
    private BookingStatus status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Lọc đơn từ ngày (YYYY-MM-DD)", example = "2026-10-01")
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Lọc đơn đến ngày (YYYY-MM-DD)", example = "2026-10-31")
    private LocalDate toDate;

    @Schema(description = "Lọc theo ID sân bóng", example = "1")
    private Long pitchId;

    @Schema(description = "Lọc theo ID loại sân bóng (Ví dụ: Sân 5, Sân 7)", example = "2")
    private Long pitchTypeId;

    @Schema(description = "Lọc theo ID ca đá / khung giờ", example = "3")
    private Long timeSlotId;

    @Schema(description = "Lọc theo hình thức đặt sân (ONLINE hoặc AT_COUNTER)", example = "ONLINE")
    private BookingType bookingType;

    @Schema(description = "Lọc theo ID khách hàng (dành cho quản trị viên/nhân viên)", example = "10")
    private Long customerId;

    @Schema(description = "Lọc theo ID nhân viên phụ trách/tạo đơn", example = "5")
    private Long staffId;

    @Schema(description = "Lọc theo số điện thoại khách vãng lai hoặc khách đặt", example = "0987654321")
    private String guestPhone;
}
