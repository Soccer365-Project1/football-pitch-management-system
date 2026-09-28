package com.fpms.controller;

import com.fpms.common.response.ApiResponse;
import com.fpms.common.response.PageResponse;
import com.fpms.dto.request.PitchRequest;
import com.fpms.dto.request.UpdatePitchStatusRequest;
import com.fpms.dto.response.PitchResponse;
import com.fpms.dto.response.PitchTypeResponse;
import com.fpms.entity.enums.PitchStatus;
import com.fpms.service.PitchService;
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

@Tag(name = "3. Admin Pitch Management", description = "Các API quản trị danh mục sân bóng (Thêm, Sửa, Đổi trạng thái bảo trì/hoạt động, Lọc phân trang)")
@RestController
@RequestMapping("/api/v1/admin/pitches")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPitchController {

    private final PitchService pitchService;

    @Operation(summary = "Lấy danh sách sân bóng", description = "Truy xuất danh sách sân bóng hỗ trợ tìm kiếm theo tên, lọc theo loại sân, trạng thái và phân trang")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PitchResponse>>> getPitches(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long pitchTypeId,
            @RequestParam(required = false) PitchStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<PitchResponse> response = pitchService.getPitches(keyword, pitchTypeId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sân bóng thành công", response));
    }

    @Operation(summary = "Lấy danh mục các loại sân bóng", description = "Lấy danh sách loại sân (sân 5, sân 7...) phục vụ dropdown bộ lọc và tạo/sửa sân")
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<PitchTypeResponse>>> getAllPitchTypes() {
        List<PitchTypeResponse> types = pitchService.getAllPitchTypes();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh mục loại sân thành công", types));
    }

    @Operation(summary = "Lấy chi tiết sân bóng theo ID", description = "Truy xuất thông tin chi tiết của một sân bóng cụ thể")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PitchResponse>> getPitchById(@PathVariable Long id) {
        PitchResponse response = pitchService.getPitchById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin sân bóng thành công", response));
    }

    @Operation(summary = "Thêm sân bóng mới", description = "Tạo mới sân bóng với trạng thái mặc định là ACTIVE (Đang hoạt động)")
    @PostMapping
    public ResponseEntity<ApiResponse<PitchResponse>> createPitch(@Valid @RequestBody PitchRequest request) {
        PitchResponse response = pitchService.createPitch(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm sân bóng thành công", response));
    }

    @Operation(summary = "Chỉnh sửa thông tin sân bóng", description = "Cập nhật tên sân, loại sân và mô tả của sân bóng")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PitchResponse>> updatePitch(
            @PathVariable Long id,
            @Valid @RequestBody PitchRequest request
    ) {
        PitchResponse response = pitchService.updatePitch(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin sân bóng thành công", response));
    }

    @Operation(summary = "Cập nhật trạng thái sân bóng", description = "Chuyển đổi trạng thái sân giữa ACTIVE (Hoạt động) và MAINTENANCE (Bảo trì)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PitchResponse>> updatePitchStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePitchStatusRequest request
    ) {
        PitchResponse response = pitchService.updatePitchStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái sân bóng thành công", response));
    }

    @Operation(summary = "Xóa sân bóng", description = "Thực hiện xóa mềm sân bóng khỏi hệ thống")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePitch(@PathVariable Long id) {
        pitchService.deletePitch(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa sân bóng thành công", null));
    }
}
