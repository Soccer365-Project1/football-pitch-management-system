package com.fpms.controller;

import com.fpms.common.response.ApiResponse;
import com.fpms.dto.request.TimeSlotRequest;
import com.fpms.dto.response.TimeSlotResponse;
import com.fpms.service.TimeSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "4. Admin TimeSlot Management", description = "Các API quản trị danh mục khung giờ (Thêm, Sửa, Xóa, Lấy danh sách)")
@RestController
@RequestMapping("/api/v1/admin/time-slots")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTimeSlotController {

    private final TimeSlotService timeSlotService;

    @Operation(summary = "Lấy danh sách khung giờ", description = "Truy xuất toàn bộ danh sách ca đá trong ngày, sắp xếp tăng dần theo thời gian bắt đầu")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TimeSlotResponse>>> getAllTimeSlots(
            @RequestParam(required = false) Boolean activeOnly
    ) {
        List<TimeSlotResponse> response = timeSlotService.getAllTimeSlots(activeOnly);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khung giờ thành công", response));
    }

    @Operation(summary = "Lấy chi tiết khung giờ theo ID", description = "Truy xuất thông tin chi tiết của một ca đá cụ thể")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TimeSlotResponse>> getTimeSlotById(@PathVariable Long id) {
        TimeSlotResponse response = timeSlotService.getTimeSlotById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin khung giờ thành công", response));
    }

    @Operation(summary = "Thêm mới khung giờ", description = "Tạo mới một ca đá (hỗ trợ thiết lập Giờ thường hoặc Giờ vàng), tự động kiểm tra chống chồng chéo thời gian")
    @PostMapping
    public ResponseEntity<ApiResponse<TimeSlotResponse>> createTimeSlot(@Valid @RequestBody TimeSlotRequest request) {
        TimeSlotResponse response = timeSlotService.createTimeSlot(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm khung giờ thành công", response));
    }

    @Operation(summary = "Chỉnh sửa khung giờ", description = "Cập nhật thời gian hoặc thay đổi trạng thái Giờ thường / Giờ vàng của ca đá")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TimeSlotResponse>> updateTimeSlot(
            @PathVariable Long id,
            @Valid @RequestBody TimeSlotRequest request
    ) {
        TimeSlotResponse response = timeSlotService.updateTimeSlot(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật khung giờ thành công", response));
    }

    @Operation(summary = "Xóa khung giờ", description = "Xóa mềm khung giờ khỏi hệ thống (chặn xóa nếu đang có đơn đặt chưa hoàn tất)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTimeSlot(@PathVariable Long id) {
        timeSlotService.deleteTimeSlot(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa khung giờ thành công", null));
    }
}
