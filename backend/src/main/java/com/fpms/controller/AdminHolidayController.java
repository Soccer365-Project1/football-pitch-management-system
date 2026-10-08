package com.fpms.controller;

import com.fpms.common.response.ApiResponse;
import com.fpms.dto.request.HolidayRequest;
import com.fpms.dto.response.HolidayResponse;
import com.fpms.service.HolidayService;
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

@Tag(name = "5. Admin Holiday Management", description = "Các API quản trị danh mục ngày lễ (Thêm, Sửa, Xóa, Lấy danh sách)")
@RestController
@RequestMapping("/api/v1/admin/holidays")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminHolidayController {

    private final HolidayService holidayService;

    @Operation(summary = "Lấy danh sách ngày lễ", description = "Truy xuất danh sách ngày lễ trong hệ thống, sắp xếp tăng dần theo ngày. Có hỗ trợ lọc theo năm qua tham số year")
    @GetMapping
    public ResponseEntity<ApiResponse<List<HolidayResponse>>> getAllHolidays(
            @RequestParam(required = false) Integer year
    ) {
        List<HolidayResponse> response = holidayService.getAllHolidays(year);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách ngày lễ thành công", response));
    }

    @Operation(summary = "Lấy chi tiết ngày lễ theo ID", description = "Truy xuất thông tin chi tiết của một ngày lễ cụ thể")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HolidayResponse>> getHolidayById(@PathVariable Long id) {
        HolidayResponse response = holidayService.getHolidayById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin ngày lễ thành công", response));
    }

    @Operation(summary = "Thêm mới ngày lễ", description = "Tạo mới một ngày lễ trong hệ thống, tự động kiểm tra chống trùng lặp ngày lễ")
    @PostMapping
    public ResponseEntity<ApiResponse<HolidayResponse>> createHoliday(@Valid @RequestBody HolidayRequest request) {
        HolidayResponse response = holidayService.createHoliday(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm ngày lễ thành công", response));
    }

    @Operation(summary = "Chỉnh sửa ngày lễ", description = "Cập nhật tên, mô tả hoặc ngày diễn ra của ngày lễ")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HolidayResponse>> updateHoliday(
            @PathVariable Long id,
            @Valid @RequestBody HolidayRequest request
    ) {
        HolidayResponse response = holidayService.updateHoliday(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật ngày lễ thành công", response));
    }

    @Operation(summary = "Xóa ngày lễ", description = "Xóa một ngày lễ ra khỏi hệ thống cấu hình")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHoliday(@PathVariable Long id) {
        holidayService.deleteHoliday(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa ngày lễ thành công", null));
    }
}
