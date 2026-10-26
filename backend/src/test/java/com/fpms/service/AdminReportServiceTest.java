package com.fpms.service;

import com.fpms.dto.request.ReportFilterRequest;
import com.fpms.dto.response.report.*;
import com.fpms.entity.enums.BookingStatus;
import com.fpms.entity.enums.RoleName;
import com.fpms.entity.enums.UserStatus;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.repository.BookingRepository;
import com.fpms.repository.UserRepository;
import com.fpms.service.impl.AdminReportServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminReportServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminReportServiceImpl adminReportService;

    @Test
    @DisplayName("getDashboardSummary: Thành công tính toán 4 chỉ số KPI và so sánh kỳ trước")
    void getDashboardSummary_Success() {
        LocalDate startDate = LocalDate.of(2026, 8, 17);
        LocalDate endDate = LocalDate.of(2026, 8, 23); // 7 ngày
        ReportFilterRequest request = new ReportFilterRequest(startDate, endDate);

        LocalDate prevStartDate = LocalDate.of(2026, 8, 10);
        LocalDate prevEndDate = LocalDate.of(2026, 8, 16);

        // Doanh thu
        when(bookingRepository.sumRevenueBetweenDates(startDate, endDate))
                .thenReturn(BigDecimal.valueOf(10000000));
        when(bookingRepository.sumRevenueBetweenDates(prevStartDate, prevEndDate))
                .thenReturn(BigDecimal.valueOf(8000000));

        // Trạng thái đơn kỳ này: 8 COMPLETED, 2 CANCELLED -> Tổng 10 đơn
        List<Object[]> currentStatusRows = new ArrayList<>();
        currentStatusRows.add(new Object[]{BookingStatus.COMPLETED, 8L});
        currentStatusRows.add(new Object[]{BookingStatus.CANCELLED, 2L});
        when(bookingRepository.countBookingsByStatusBetweenDates(startDate, endDate))
                .thenReturn(currentStatusRows);

        // Trạng thái đơn kỳ trước: 9 COMPLETED, 1 CANCELLED -> Tổng 10 đơn
        List<Object[]> prevStatusRows = new ArrayList<>();
        prevStatusRows.add(new Object[]{BookingStatus.COMPLETED, 9L});
        prevStatusRows.add(new Object[]{BookingStatus.CANCELLED, 1L});
        when(bookingRepository.countBookingsByStatusBetweenDates(prevStartDate, prevEndDate))
                .thenReturn(prevStatusRows);

        // Khách hàng mới
        when(userRepository.countByIsDeletedFalseAndRoleRoleNameAndCreatedAtBetween(
                eq(RoleName.ROLE_CUSTOMER), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(15L);

        // Tỷ lệ khách quay lại
        when(bookingRepository.countDistinctCustomersBetweenDates(startDate, endDate))
                .thenReturn(10L);
        List<Object[]> repeatCustomerRows = new ArrayList<>();
        repeatCustomerRows.add(new Object[]{101L, 3L});
        repeatCustomerRows.add(new Object[]{102L, 2L});
        when(bookingRepository.findRepeatCustomerIdsBetweenDates(startDate, endDate))
                .thenReturn(repeatCustomerRows);

        DashboardSummaryResponse response = adminReportService.getDashboardSummary(request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(10000000), response.getCurrentRevenue());
        assertEquals(BigDecimal.valueOf(8000000), response.getPreviousRevenue());
        assertEquals(25.0, response.getRevenueGrowthRate()); // (10tr - 8tr) / 8tr * 100 = 25%

        assertEquals(10L, response.getTotalBookings());
        assertEquals(8L, response.getCompletedBookings());
        assertEquals(2L, response.getCancelledBookings());
        assertEquals(20.0, response.getCancellationRate()); // 2 / 10 = 20%
        assertEquals(10.0, response.getPreviousCancellationRate()); // 1 / 10 = 10%
        assertEquals(10.0, response.getCancellationRateDiff());

        assertEquals(15L, response.getNewCustomersCount());
        assertEquals(20.0, response.getRepeatCustomerRate()); // 2 / 10 = 20%
    }

    @Test
    @DisplayName("getDashboardSummary: Báo lỗi INVALID_DATE_RANGE khi ngày bắt đầu sau ngày kết thúc")
    void getDashboardSummary_InvalidDateRange_ThrowsException() {
        ReportFilterRequest request = new ReportFilterRequest(
                LocalDate.of(2026, 8, 25),
                LocalDate.of(2026, 8, 20)
        );

        AppException ex = assertThrows(AppException.class, () -> adminReportService.getDashboardSummary(request));
        assertEquals(ErrorCode.INVALID_DATE_RANGE, ex.getErrorCode());
    }

    @Test
    @DisplayName("getRevenueTrend: Điền đầy đủ các ngày trong kỳ và bù 0 VNĐ cho ngày không có doanh thu")
    void getRevenueTrend_FillsMissingDatesWithZero() {
        LocalDate startDate = LocalDate.of(2026, 8, 17); // Thứ 2
        LocalDate endDate = LocalDate.of(2026, 8, 19);   // Thứ 4 (3 ngày)
        ReportFilterRequest request = new ReportFilterRequest(startDate, endDate);

        // Giả sử chỉ có ngày 17 và ngày 19 có đơn, ngày 18 không có
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{LocalDate.of(2026, 8, 17), BigDecimal.valueOf(1500000), 2L});
        rows.add(new Object[]{LocalDate.of(2026, 8, 19), BigDecimal.valueOf(3000000), 4L});
        when(bookingRepository.getDailyRevenueBetweenDates(startDate, endDate)).thenReturn(rows);

        List<RevenueTrendPointResponse> trend = adminReportService.getRevenueTrend(request);

        assertEquals(3, trend.size());
        assertEquals(LocalDate.of(2026, 8, 17), trend.get(0).getDate());
        assertEquals(BigDecimal.valueOf(1500000), trend.get(0).getRevenue());
        assertEquals(2L, trend.get(0).getBookingCount());

        // Ngày 18 được tự động bù 0
        assertEquals(LocalDate.of(2026, 8, 18), trend.get(1).getDate());
        assertEquals(BigDecimal.ZERO, trend.get(1).getRevenue());
        assertEquals(0L, trend.get(1).getBookingCount());

        assertEquals(LocalDate.of(2026, 8, 19), trend.get(2).getDate());
        assertEquals(BigDecimal.valueOf(3000000), trend.get(2).getRevenue());
    }

    @Test
    @DisplayName("getBookingFrequency: Trả về chính xác 7 ngày trong tuần từ Thứ 2 đến Chủ Nhật")
    void getBookingFrequency_ReturnsAllSevenDays() {
        LocalDate startDate = LocalDate.of(2026, 8, 17);
        LocalDate endDate = LocalDate.of(2026, 8, 23);
        ReportFilterRequest request = new ReportFilterRequest(startDate, endDate);

        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{LocalDate.of(2026, 8, 17), 5L}); // Thứ 2
        rows.add(new Object[]{LocalDate.of(2026, 8, 21), 8L}); // Thứ 6
        when(bookingRepository.getDailyBookingCountBetweenDates(startDate, endDate)).thenReturn(rows);

        List<BookingFrequencyResponse> frequency = adminReportService.getBookingFrequency(request);

        assertEquals(7, frequency.size());
        assertEquals(1, frequency.get(0).getDayOfWeekIndex());
        assertEquals("Thứ 2", frequency.get(0).getDayOfWeekName());
        assertEquals(5L, frequency.get(0).getBookingCount());

        assertEquals("Thứ 3", frequency.get(1).getDayOfWeekName());
        assertEquals(0L, frequency.get(1).getBookingCount());

        assertEquals(5, frequency.get(4).getDayOfWeekIndex());
        assertEquals("Thứ 6", frequency.get(4).getDayOfWeekName());
        assertEquals(8L, frequency.get(4).getBookingCount());

        assertEquals("Chủ Nhật", frequency.get(6).getDayOfWeekName());
    }

    @Test
    @DisplayName("getRevenueByPitchType: Tính tỷ trọng doanh thu chính xác theo từng loại sân")
    void getRevenueByPitchType_CalculatesPercentageCorrectly() {
        LocalDate startDate = LocalDate.of(2026, 8, 17);
        LocalDate endDate = LocalDate.of(2026, 8, 23);
        ReportFilterRequest request = new ReportFilterRequest(startDate, endDate);

        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{1L, "Sân 5 người", BigDecimal.valueOf(60000000), 50L});
        rows.add(new Object[]{2L, "Sân 7 người", BigDecimal.valueOf(40000000), 20L});
        when(bookingRepository.getRevenueByPitchTypeBetweenDates(startDate, endDate)).thenReturn(rows);

        List<RevenueByPitchTypeResponse> pitchTypeStats = adminReportService.getRevenueByPitchType(request);

        assertEquals(2, pitchTypeStats.size());
        assertEquals("Sân 5 người", pitchTypeStats.get(0).getPitchTypeName());
        assertEquals(60.0, pitchTypeStats.get(0).getPercentage());

        assertEquals("Sân 7 người", pitchTypeStats.get(1).getPitchTypeName());
        assertEquals(40.0, pitchTypeStats.get(1).getPercentage());
    }

    @Test
    @DisplayName("getUserStatistic: Thống kê chính xác tổng quan tài khoản người dùng")
    void getUserStatistic_Success() {
        when(userRepository.countByIsDeletedFalse()).thenReturn(200L);
        when(userRepository.countByIsDeletedFalseAndRoleRoleName(RoleName.ROLE_CUSTOMER)).thenReturn(185L);
        when(userRepository.countByIsDeletedFalseAndRoleRoleName(RoleName.ROLE_STAFF)).thenReturn(12L);
        when(userRepository.countByIsDeletedFalseAndStatus(UserStatus.LOCKED)).thenReturn(5L);
        when(userRepository.countByIsDeletedFalseAndCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(30L);

        UserStatisticResponse stat = adminReportService.getUserStatistic();

        assertEquals(200L, stat.getTotalUsers());
        assertEquals(185L, stat.getTotalCustomers());
        assertEquals(12L, stat.getTotalStaffs());
        assertEquals(5L, stat.getTotalLockedUsers());
        assertEquals(30L, stat.getNewUsersThisMonth());
    }
}
