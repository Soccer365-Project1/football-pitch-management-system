package com.fpms.service.impl;

import com.fpms.dto.request.ReportFilterRequest;
import com.fpms.dto.response.report.*;
import com.fpms.entity.enums.BookingStatus;
import com.fpms.entity.enums.RoleName;
import com.fpms.entity.enums.UserStatus;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.repository.BookingRepository;
import com.fpms.repository.UserRepository;
import com.fpms.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportServiceImpl implements AdminReportService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Override
    public DashboardSummaryResponse getDashboardSummary(ReportFilterRequest filterRequest) {
        validateDateRange(filterRequest);

        LocalDate startDate = filterRequest.getStartDate();
        LocalDate endDate = filterRequest.getEndDate();

        // 1. Xác định kỳ trước tương ứng
        long daysSpan = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDate prevEndDate = startDate.minusDays(1);
        LocalDate prevStartDate = prevEndDate.minusDays(daysSpan - 1);

        // 2. Tính toán Doanh thu (Kỳ hiện tại vs Kỳ trước)
        BigDecimal currentRevenue = bookingRepository.sumRevenueBetweenDates(startDate, endDate);
        BigDecimal previousRevenue = bookingRepository.sumRevenueBetweenDates(prevStartDate, prevEndDate);
        Double revenueGrowthRate = calculateGrowthRate(currentRevenue, previousRevenue);

        // 3. Phân tích trạng thái đơn đặt sân kỳ hiện tại
        List<Object[]> currentStatusRows = bookingRepository.countBookingsByStatusBetweenDates(startDate, endDate);
        Map<BookingStatus, Long> currentStatusCounts = mapStatusCounts(currentStatusRows);

        long totalBookings = currentStatusCounts.values().stream().mapToLong(Long::longValue).sum();
        long completedBookings = currentStatusCounts.getOrDefault(BookingStatus.COMPLETED, 0L);
        long confirmedBookings = currentStatusCounts.getOrDefault(BookingStatus.CONFIRMED, 0L);
        long cancelledBookings = currentStatusCounts.getOrDefault(BookingStatus.CANCELLED, 0L);

        // 4. Phân tích tỷ lệ hủy đơn (Kỳ hiện tại vs Kỳ trước)
        List<Object[]> prevStatusRows = bookingRepository.countBookingsByStatusBetweenDates(prevStartDate, prevEndDate);
        Map<BookingStatus, Long> prevStatusCounts = mapStatusCounts(prevStatusRows);
        long prevTotalBookings = prevStatusCounts.values().stream().mapToLong(Long::longValue).sum();
        long prevCancelledBookings = prevStatusCounts.getOrDefault(BookingStatus.CANCELLED, 0L);

        double cancellationRate = totalBookings > 0
                ? roundToTwoDecimals((cancelledBookings * 100.0) / totalBookings)
                : 0.0;
        double prevCancellationRate = prevTotalBookings > 0
                ? roundToTwoDecimals((prevCancelledBookings * 100.0) / prevTotalBookings)
                : 0.0;
        double cancellationRateDiff = roundToTwoDecimals(cancellationRate - prevCancellationRate);

        // 5. Thống kê khách hàng mới trong kỳ
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        long newCustomersCount = userRepository.countByIsDeletedFalseAndRoleRoleNameAndCreatedAtBetween(
                RoleName.ROLE_CUSTOMER, startDateTime, endDateTime);

        // 6. Tỷ lệ khách quay lại trong kỳ
        long distinctCustomers = bookingRepository.countDistinctCustomersBetweenDates(startDate, endDate);
        List<Object[]> repeatCustomerRows = bookingRepository.findRepeatCustomerIdsBetweenDates(startDate, endDate);
        long repeatCustomerCount = repeatCustomerRows.size();
        double repeatCustomerRate = distinctCustomers > 0
                ? roundToTwoDecimals((repeatCustomerCount * 100.0) / distinctCustomers)
                : 0.0;

        return DashboardSummaryResponse.builder()
                .currentRevenue(currentRevenue)
                .previousRevenue(previousRevenue)
                .revenueGrowthRate(revenueGrowthRate)
                .totalBookings(totalBookings)
                .completedBookings(completedBookings)
                .confirmedBookings(confirmedBookings)
                .cancelledBookings(cancelledBookings)
                .cancellationRate(cancellationRate)
                .previousCancellationRate(prevCancellationRate)
                .cancellationRateDiff(cancellationRateDiff)
                .newCustomersCount(newCustomersCount)
                .repeatCustomerRate(repeatCustomerRate)
                .build();
    }

    @Override
    public List<RevenueTrendPointResponse> getRevenueTrend(ReportFilterRequest filterRequest) {
        validateDateRange(filterRequest);

        LocalDate startDate = filterRequest.getStartDate();
        LocalDate endDate = filterRequest.getEndDate();

        List<Object[]> rows = bookingRepository.getDailyRevenueBetweenDates(startDate, endDate);
        Map<LocalDate, DailyData> dailyMap = new HashMap<>();
        for (Object[] row : rows) {
            LocalDate date = (LocalDate) row[0];
            BigDecimal revenue = (BigDecimal) row[1];
            Long count = ((Number) row[2]).longValue();
            dailyMap.put(date, new DailyData(revenue, count));
        }

        List<RevenueTrendPointResponse> result = new ArrayList<>();
        LocalDate cur = startDate;
        while (!cur.isAfter(endDate)) {
            DailyData data = dailyMap.getOrDefault(cur, new DailyData(BigDecimal.ZERO, 0L));
            result.add(RevenueTrendPointResponse.builder()
                    .date(cur)
                    .dayName(getDayOfWeekName(cur.getDayOfWeek()))
                    .revenue(data.revenue())
                    .bookingCount(data.count())
                    .build());
            cur = cur.plusDays(1);
        }

        return result;
    }

    @Override
    public List<BookingFrequencyResponse> getBookingFrequency(ReportFilterRequest filterRequest) {
        validateDateRange(filterRequest);

        LocalDate startDate = filterRequest.getStartDate();
        LocalDate endDate = filterRequest.getEndDate();

        List<Object[]> rows = bookingRepository.getDailyBookingCountBetweenDates(startDate, endDate);
        Map<DayOfWeek, Long> countsByDow = new EnumMap<>(DayOfWeek.class);
        for (Object[] row : rows) {
            LocalDate date = (LocalDate) row[0];
            Long count = ((Number) row[1]).longValue();
            countsByDow.merge(date.getDayOfWeek(), count, Long::sum);
        }

        List<BookingFrequencyResponse> result = new ArrayList<>();
        DayOfWeek[] daysOfWeek = DayOfWeek.values(); // MONDAY to SUNDAY (1..7)
        for (int i = 0; i < daysOfWeek.length; i++) {
            DayOfWeek dow = daysOfWeek[i];
            result.add(BookingFrequencyResponse.builder()
                    .dayOfWeekIndex(i + 1)
                    .dayOfWeekName(getDayOfWeekName(dow))
                    .bookingCount(countsByDow.getOrDefault(dow, 0L))
                    .build());
        }

        return result;
    }

    @Override
    public List<RevenueByPitchTypeResponse> getRevenueByPitchType(ReportFilterRequest filterRequest) {
        validateDateRange(filterRequest);

        LocalDate startDate = filterRequest.getStartDate();
        LocalDate endDate = filterRequest.getEndDate();

        List<Object[]> rows = bookingRepository.getRevenueByPitchTypeBetweenDates(startDate, endDate);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Object[] row : rows) {
            BigDecimal rev = (BigDecimal) row[2];
            if (rev != null) {
                totalRevenue = totalRevenue.add(rev);
            }
        }

        List<RevenueByPitchTypeResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long pitchTypeId = ((Number) row[0]).longValue();
            String pitchTypeName = (String) row[1];
            BigDecimal revenue = (BigDecimal) row[2];
            Long bookingCount = ((Number) row[3]).longValue();

            double percentage = 0.0;
            if (totalRevenue.compareTo(BigDecimal.ZERO) > 0 && revenue != null) {
                percentage = roundToTwoDecimals(
                        revenue.multiply(BigDecimal.valueOf(100))
                                .divide(totalRevenue, 4, RoundingMode.HALF_UP)
                                .doubleValue()
                );
            }

            result.add(RevenueByPitchTypeResponse.builder()
                    .pitchTypeId(pitchTypeId)
                    .pitchTypeName(pitchTypeName)
                    .revenue(revenue != null ? revenue : BigDecimal.ZERO)
                    .percentage(percentage)
                    .bookingCount(bookingCount)
                    .build());
        }

        return result;
    }

    @Override
    public UserStatisticResponse getUserStatistic() {
        long totalUsers = userRepository.countByIsDeletedFalse();
        long totalCustomers = userRepository.countByIsDeletedFalseAndRoleRoleName(RoleName.ROLE_CUSTOMER);
        long totalStaffs = userRepository.countByIsDeletedFalseAndRoleRoleName(RoleName.ROLE_STAFF);
        long totalLockedUsers = userRepository.countByIsDeletedFalseAndStatus(UserStatus.LOCKED);

        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = LocalDateTime.now();
        long newUsersThisMonth = userRepository.countByIsDeletedFalseAndCreatedAtBetween(startOfMonth, endOfMonth);

        return UserStatisticResponse.builder()
                .totalUsers(totalUsers)
                .totalCustomers(totalCustomers)
                .totalStaffs(totalStaffs)
                .totalLockedUsers(totalLockedUsers)
                .newUsersThisMonth(newUsersThisMonth)
                .build();
    }

    // ==================== HELPER METHODS ====================

    private void validateDateRange(ReportFilterRequest request) {
        if (request == null || request.getStartDate() == null || request.getEndDate() == null) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private Double calculateGrowthRate(BigDecimal current, BigDecimal previous) {
        if (current == null) current = BigDecimal.ZERO;
        if (previous == null) previous = BigDecimal.ZERO;

        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }

        BigDecimal diff = current.subtract(previous);
        BigDecimal rate = diff.multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
        return rate.doubleValue();
    }

    private Map<BookingStatus, Long> mapStatusCounts(List<Object[]> rows) {
        Map<BookingStatus, Long> map = new EnumMap<>(BookingStatus.class);
        if (rows != null) {
            for (Object[] row : rows) {
                BookingStatus status = (BookingStatus) row[0];
                Long count = ((Number) row[1]).longValue();
                map.put(status, count);
            }
        }
        return map;
    }

    private double roundToTwoDecimals(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String getDayOfWeekName(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "Thứ 2";
            case TUESDAY -> "Thứ 3";
            case WEDNESDAY -> "Thứ 4";
            case THURSDAY -> "Thứ 5";
            case FRIDAY -> "Thứ 6";
            case SATURDAY -> "Thứ 7";
            case SUNDAY -> "Chủ Nhật";
        };
    }

    private record DailyData(BigDecimal revenue, Long count) {}
}
