package com.fpms.controller;

import com.fpms.common.response.ApiResponse;
import com.fpms.dto.request.ReportFilterRequest;
import com.fpms.dto.response.report.*;
import com.fpms.security.UserPrincipal;
import com.fpms.service.AdminReportService;
import com.fpms.service.ExcelExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "6. Admin Reports & Analytics", description = "Các API Báo cáo Thống kê Doanh thu, Tần suất đặt sân & Tổng quan người dùng")
@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final AdminReportService adminReportService;
    private final ExcelExportService excelExportService;

    @Operation(summary = "Lấy dữ liệu 4 thẻ KPI Dashboard", description = "Tính toán Doanh thu, Tổng số đơn, Tỉ lệ hủy đơn và Lượt khách mới trong kỳ (kèm so sánh % với kỳ trước)")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getDashboardSummary(
            @Valid @ModelAttribute ReportFilterRequest filterRequest
    ) {
        DashboardSummaryResponse response = adminReportService.getDashboardSummary(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Lấy số liệu KPI tổng quan thành công", response));
    }

    @Operation(summary = "Lấy dữ liệu biểu đồ đường doanh thu", description = "Thống kê doanh thu theo từng ngày liên tục trong khoảng thời gian đã chọn")
    @GetMapping("/revenue-trend")
    public ResponseEntity<ApiResponse<List<RevenueTrendPointResponse>>> getRevenueTrend(
            @Valid @ModelAttribute ReportFilterRequest filterRequest
    ) {
        List<RevenueTrendPointResponse> response = adminReportService.getRevenueTrend(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Lấy dữ liệu biểu đồ doanh thu thành công", response));
    }

    @Operation(summary = "Lấy dữ liệu biểu đồ cột tần suất đặt sân theo thứ", description = "Tổng hợp số lượt đặt sân theo 7 ngày trong tuần từ Thứ 2 đến Chủ Nhật")
    @GetMapping("/booking-frequency")
    public ResponseEntity<ApiResponse<List<BookingFrequencyResponse>>> getBookingFrequency(
            @Valid @ModelAttribute ReportFilterRequest filterRequest
    ) {
        List<BookingFrequencyResponse> response = adminReportService.getBookingFrequency(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Lấy dữ liệu tần suất đặt sân thành công", response));
    }

    @Operation(summary = "Lấy dữ liệu biểu đồ tròn cơ cấu doanh thu theo loại sân", description = "Phân rã tỷ trọng doanh thu đóng góp theo từng phân loại sân bóng (Sân 5, 7, 11)")
    @GetMapping("/revenue-by-pitch-type")
    public ResponseEntity<ApiResponse<List<RevenueByPitchTypeResponse>>> getRevenueByPitchType(
            @Valid @ModelAttribute ReportFilterRequest filterRequest
    ) {
        List<RevenueByPitchTypeResponse> response = adminReportService.getRevenueByPitchType(filterRequest);
        return ResponseEntity.ok(ApiResponse.success("Lấy cơ cấu doanh thu theo loại sân thành công", response));
    }

    @Operation(summary = "Lấy số liệu thống kê người dùng hệ thống", description = "Thống kê tổng số tài khoản, khách hàng, nhân viên, tài khoản bị khóa và người dùng mới trong tháng")
    @GetMapping("/users/summary")
    public ResponseEntity<ApiResponse<UserStatisticResponse>> getUserStatistic() {
        UserStatisticResponse response = adminReportService.getUserStatistic();
        return ResponseEntity.ok(ApiResponse.success("Lấy thống kê người dùng thành công", response));
    }

    @Operation(summary = "Xuất file báo cáo Excel doanh thu & đơn đặt sân (SRS UC018.2)",
               description = "Xuất file Excel .xlsx 2 sheet gồm Tổng quan KPI và Danh sách chi tiết đơn đặt sân")
    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportReportExcel(
            @Valid @ModelAttribute ReportFilterRequest filterRequest,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        byte[] excelData = excelExportService.exportRevenueReport(filterRequest, currentUser);
        String fileName = String.format("BaoCao_DoanhThu_FPMS_%s_%s.xlsx",
                filterRequest.getStartDate(), filterRequest.getEndDate());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelData);
    }
}
