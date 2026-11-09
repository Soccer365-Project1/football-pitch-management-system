package com.fpms.controller;

import com.fpms.dto.request.ReportFilterRequest;
import com.fpms.dto.response.report.*;
import com.fpms.exception.GlobalExceptionHandler;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.service.AdminReportService;
import com.fpms.service.ExcelExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminReportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminReportService adminReportService;

    @Mock
    private ExcelExportService excelExportService;

    @InjectMocks
    private AdminReportController adminReportController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminReportController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports/summary: Thành công trả về 4 chỉ số KPI")
    void getDashboardSummary_Success() throws Exception {
        DashboardSummaryResponse response = DashboardSummaryResponse.builder()
                .currentRevenue(BigDecimal.valueOf(15000000))
                .previousRevenue(BigDecimal.valueOf(12000000))
                .revenueGrowthRate(25.0)
                .totalBookings(50L)
                .completedBookings(45L)
                .confirmedBookings(3L)
                .cancelledBookings(2L)
                .cancellationRate(4.0)
                .previousCancellationRate(5.0)
                .cancellationRateDiff(-1.0)
                .newCustomersCount(20L)
                .repeatCustomerRate(35.0)
                .build();

        when(adminReportService.getDashboardSummary(any(ReportFilterRequest.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/admin/reports/summary")
                        .param("startDate", "2026-08-17")
                        .param("endDate", "2026-08-23")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.currentRevenue").value(15000000))
                .andExpect(jsonPath("$.data.revenueGrowthRate").value(25.0))
                .andExpect(jsonPath("$.data.totalBookings").value(50))
                .andExpect(jsonPath("$.data.cancellationRate").value(4.0));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports/revenue-trend: Thành công trả về chuỗi điểm biểu đồ đường")
    void getRevenueTrend_Success() throws Exception {
        List<RevenueTrendPointResponse> points = List.of(
                RevenueTrendPointResponse.builder()
                        .date(LocalDate.of(2026, 8, 17))
                        .dayName("Thứ 2")
                        .revenue(BigDecimal.valueOf(2500000))
                        .bookingCount(5L)
                        .build()
        );

        when(adminReportService.getRevenueTrend(any(ReportFilterRequest.class))).thenReturn(points);

        mockMvc.perform(get("/api/v1/admin/reports/revenue-trend")
                        .param("startDate", "2026-08-17")
                        .param("endDate", "2026-08-23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].dayName").value("Thứ 2"))
                .andExpect(jsonPath("$.data[0].revenue").value(2500000));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports/booking-frequency: Thành công trả về 7 ngày trong tuần")
    void getBookingFrequency_Success() throws Exception {
        List<BookingFrequencyResponse> list = List.of(
                BookingFrequencyResponse.builder().dayOfWeekIndex(1).dayOfWeekName("Thứ 2").bookingCount(10L).build(),
                BookingFrequencyResponse.builder().dayOfWeekIndex(2).dayOfWeekName("Thứ 3").bookingCount(8L).build()
        );

        when(adminReportService.getBookingFrequency(any(ReportFilterRequest.class))).thenReturn(list);

        mockMvc.perform(get("/api/v1/admin/reports/booking-frequency")
                        .param("startDate", "2026-08-17")
                        .param("endDate", "2026-08-23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].dayOfWeekName").value("Thứ 2"))
                .andExpect(jsonPath("$.data[0].bookingCount").value(10));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports/revenue-by-pitch-type: Thành công trả về cơ cấu loại sân")
    void getRevenueByPitchType_Success() throws Exception {
        List<RevenueByPitchTypeResponse> list = List.of(
                RevenueByPitchTypeResponse.builder()
                        .pitchTypeId(1L)
                        .pitchTypeName("Sân 5 người")
                        .revenue(BigDecimal.valueOf(6000000))
                        .percentage(60.0)
                        .bookingCount(20L)
                        .build()
        );

        when(adminReportService.getRevenueByPitchType(any(ReportFilterRequest.class))).thenReturn(list);

        mockMvc.perform(get("/api/v1/admin/reports/revenue-by-pitch-type")
                        .param("startDate", "2026-08-17")
                        .param("endDate", "2026-08-23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].pitchTypeName").value("Sân 5 người"))
                .andExpect(jsonPath("$.data[0].percentage").value(60.0));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports/users/summary: Thành công trả về tổng quan người dùng")
    void getUserStatistic_Success() throws Exception {
        UserStatisticResponse stat = UserStatisticResponse.builder()
                .totalUsers(100L)
                .totalCustomers(90L)
                .totalStaffs(8L)
                .totalLockedUsers(2L)
                .newUsersThisMonth(15L)
                .build();

        when(adminReportService.getUserStatistic()).thenReturn(stat);

        mockMvc.perform(get("/api/v1/admin/reports/users/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalUsers").value(100))
                .andExpect(jsonPath("$.data.totalCustomers").value(90))
                .andExpect(jsonPath("$.data.totalStaffs").value(8))
                .andExpect(jsonPath("$.data.totalLockedUsers").value(2))
                .andExpect(jsonPath("$.data.newUsersThisMonth").value(15));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports/summary: Thiếu tham số ngày trả lỗi 400")
    void getDashboardSummary_MissingDate_Returns400() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/summary")
                        .param("startDate", "2026-08-17")) // Thiếu endDate
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports/export-excel: Thành công trả về file Excel nhị phân")
    void exportReportExcel_Success() throws Exception {
        byte[] mockBytes = new byte[]{0x50, 0x4B, 0x03, 0x04}; // PK zip header
        when(excelExportService.exportRevenueReport(any(), any())).thenReturn(mockBytes);

        mockMvc.perform(get("/api/v1/admin/reports/export-excel")
                        .param("startDate", "2026-08-17")
                        .param("endDate", "2026-08-23"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"BaoCao_DoanhThu_FPMS_2026-08-17_2026-08-23.xlsx\""))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(mockBytes));
    }

    @Test
    @DisplayName("GET /api/v1/admin/reports/export-excel: Không có dữ liệu trả về lỗi 400 và mã 5002 (Luồng 4a SRS)")
    void exportReportExcel_EmptyData_Returns400() throws Exception {
        when(excelExportService.exportRevenueReport(any(), any()))
                .thenThrow(new AppException(ErrorCode.REPORT_DATA_EMPTY));

        mockMvc.perform(get("/api/v1/admin/reports/export-excel")
                        .param("startDate", "2026-08-17")
                        .param("endDate", "2026-08-23"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(5002))
                .andExpect(jsonPath("$.message").value("Không có dữ liệu để xuất báo cáo"));
    }
}
