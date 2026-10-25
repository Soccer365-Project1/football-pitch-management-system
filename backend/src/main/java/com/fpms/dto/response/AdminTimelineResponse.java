package com.fpms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO tổng hợp dữ liệu lưới ca đá ngày cho Admin/Nhân viên.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminTimelineResponse {
    private LocalDate date;
    private List<AdminTimelineSlotResponse> bookings;
}
