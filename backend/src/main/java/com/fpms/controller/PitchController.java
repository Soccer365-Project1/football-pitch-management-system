package com.fpms.controller;

import com.fpms.common.response.ApiResponse;
import com.fpms.dto.response.PublicPitchResponse;
import com.fpms.service.PitchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Public Pitch", description = "API Public danh sách sân cho Khách hàng")
@RestController
@RequestMapping("/api/v1/pitches")
@RequiredArgsConstructor
public class PitchController {

    private final PitchService pitchService;

    @Operation(summary = "Lấy danh sách sân cho Form Đặt", description = "Chỉ trả về các sân đang ACTIVE hoặc MAINTENANCE (không yêu cầu quyền Admin)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PublicPitchResponse>>> getActivePitches(
            @RequestParam(required = false) Long pitchTypeId
    ) {
        List<PublicPitchResponse> responses = pitchService.getActivePitches(pitchTypeId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sân thành công", responses));
    }

    @Operation(summary = "Lấy danh sách loại sân", description = "Trả về tất cả loại sân")
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<com.fpms.dto.response.PitchTypeResponse>>> getPitchTypes() {
        List<com.fpms.dto.response.PitchTypeResponse> responses = pitchService.getAllPitchTypes();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách loại sân thành công", responses));
    }
}
