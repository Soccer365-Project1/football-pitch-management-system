package com.fpms.service;

import com.fpms.dto.request.ReportFilterRequest;
import com.fpms.dto.response.report.*;

import java.util.List;

public interface AdminReportService {

    DashboardSummaryResponse getDashboardSummary(ReportFilterRequest filterRequest);

    List<RevenueTrendPointResponse> getRevenueTrend(ReportFilterRequest filterRequest);

    List<BookingFrequencyResponse> getBookingFrequency(ReportFilterRequest filterRequest);

    List<RevenueByPitchTypeResponse> getRevenueByPitchType(ReportFilterRequest filterRequest);

    UserStatisticResponse getUserStatistic();
}
