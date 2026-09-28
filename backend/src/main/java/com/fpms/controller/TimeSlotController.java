package com.fpms.controller;

import com.fpms.common.response.ApiResponse;
import com.fpms.dto.response.PublicTimeSlotResponse;
import com.fpms.service.TimeSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Public TimeSlot", description = "API Public danh sách khung giờ cho Khách hàng")
@RestController
@RequestMapping("/api/v1/timeslots")
@RequiredArgsConstructor
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    @Operation(summary = "Lấy danh sách khung giờ cho Form Đặt", description = "Chỉ trả về các khung giờ đang ACTIVE (không yêu cầu quyền Admin)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PublicTimeSlotResponse>>> getActiveTimeSlots() {
        List<PublicTimeSlotResponse> responses = timeSlotService.getPublicTimeSlots();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khung giờ thành công", responses));
    }
}
