package com.fpms.controller;

import com.fpms.common.response.ApiResponse;
import com.fpms.dto.request.SavePitchTypePricingRequest;
import com.fpms.dto.response.PitchTypePricingMatrixResponse;
import com.fpms.dto.response.PriceMatrixResponse;
import com.fpms.service.PriceMatrixService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "6. Admin Pricing Management", description = "Các API quản trị cấu hình biểu giá ma trận sân bóng theo loại sân")
@RestController
@RequestMapping("/api/v1/admin/pricing")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPriceMatrixController {

    private final PriceMatrixService priceMatrixService;

    @Operation(summary = "Lấy bảng giá của một loại sân", description = "Truy xuất danh sách các ô giá động của loại sân theo pitchTypeId để hiển thị lên lưới bảng giá UI")
    @GetMapping("/pitch-types/{pitchTypeId}")
    public ResponseEntity<ApiResponse<PitchTypePricingMatrixResponse>> getPricingByPitchTypeId(
            @PathVariable Long pitchTypeId
    ) {
        PitchTypePricingMatrixResponse response = priceMatrixService.getPricingByPitchTypeId(pitchTypeId);
        return ResponseEntity.ok(ApiResponse.success("Lấy bảng giá của loại sân thành công", response));
    }

    @Operation(summary = "Lưu bảng giá cho loại sân (Upsert)", description = "Lưu hoặc cập nhật đồng thời danh sách các ô giá của loại sân khi bấm 'Lưu Bảng giá'")
    @PutMapping("/pitch-types/{pitchTypeId}")
    public ResponseEntity<ApiResponse<PitchTypePricingMatrixResponse>> savePricingForPitchType(
            @PathVariable Long pitchTypeId,
            @Valid @RequestBody SavePitchTypePricingRequest request
    ) {
        PitchTypePricingMatrixResponse response = priceMatrixService.savePricingForPitchType(pitchTypeId, request);
        return ResponseEntity.ok(ApiResponse.success("Lưu cấu hình bảng giá loại sân thành công", response));
    }

    @Operation(summary = "Lấy ma trận biểu giá toàn bộ loại sân", description = "Truy xuất bảng giá được gom nhóm theo từng loại sân phục vụ tra cứu tổng quan")
    @GetMapping("/matrix")
    public ResponseEntity<ApiResponse<List<PitchTypePricingMatrixResponse>>> getAllPricingMatrices() {
        List<PitchTypePricingMatrixResponse> response = priceMatrixService.getAllPricingMatrices();
        return ResponseEntity.ok(ApiResponse.success("Lấy ma trận biểu giá thành công", response));
    }

    @Operation(summary = "Lấy danh sách biểu giá phẳng", description = "Truy xuất danh sách phẳng các ô giá, có thể lọc theo pitchTypeId")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PriceMatrixResponse>>> getAllPrices(
            @RequestParam(required = false) Long pitchTypeId
    ) {
        List<PriceMatrixResponse> response = priceMatrixService.getAllPrices(pitchTypeId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách biểu giá thành công", response));
    }

    @Operation(summary = "Lấy chi tiết một ô giá theo ID", description = "Truy xuất thông tin của một bản ghi ô giá")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PriceMatrixResponse>> getPriceById(@PathVariable Long id) {
        PriceMatrixResponse response = priceMatrixService.getPriceById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết cấu hình giá thành công", response));
    }

    @Operation(summary = "Xóa một ô giá trong ma trận", description = "Xóa bản ghi cấu hình giá theo ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSinglePrice(@PathVariable Long id) {
        priceMatrixService.deleteSinglePrice(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa cấu hình giá thành công", null));
    }
}
