import api from './api';
import type { ApiResponse } from '../types/common';
import type {
  ReportFilterParams,
  DashboardSummary,
  RevenueTrendPoint,
  BookingFrequencyPoint,
  RevenueByPitchType,
} from '../types/report';

/**
 * Service API Báo cáo & Thống kê phục vụ Admin Dashboard
 * Kết nối tới AdminReportController (/api/v1/admin/reports/**)
 */
export const adminReportService = {
  /**
   * 1. Lấy dữ liệu 4 thẻ KPI Dashboard
   * Endpoint: GET /api/v1/admin/reports/summary
   * @param params { startDate, endDate }
   */
  getDashboardSummary: async (params: ReportFilterParams): Promise<DashboardSummary> => {
    const response = await api.get<ApiResponse<DashboardSummary>>('/admin/reports/summary', {
      params,
    });
    return response.data.data!;
  },

  /**
   * 2. Lấy dữ liệu biểu đồ đường diễn biến doanh thu theo ngày
   * Endpoint: GET /api/v1/admin/reports/revenue-trend
   * @param params { startDate, endDate }
   */
  getRevenueTrend: async (params: ReportFilterParams): Promise<RevenueTrendPoint[]> => {
    const response = await api.get<ApiResponse<RevenueTrendPoint[]>>('/admin/reports/revenue-trend', {
      params,
    });
    return response.data.data || [];
  },

  /**
   * 3. Lấy dữ liệu biểu đồ cột tần suất đặt sân theo thứ trong tuần (T2 - CN)
   * Endpoint: GET /api/v1/admin/reports/booking-frequency
   * @param params { startDate, endDate }
   */
  getBookingFrequency: async (params: ReportFilterParams): Promise<BookingFrequencyPoint[]> => {
    const response = await api.get<ApiResponse<BookingFrequencyPoint[]>>('/admin/reports/booking-frequency', {
      params,
    });
    return response.data.data || [];
  },

  /**
   * 4. Lấy dữ liệu biểu đồ tròn tỷ trọng doanh thu theo phân loại sân
   * Endpoint: GET /api/v1/admin/reports/revenue-by-pitch-type
   * @param params { startDate, endDate }
   */
  getRevenueByPitchType: async (params: ReportFilterParams): Promise<RevenueByPitchType[]> => {
    const response = await api.get<ApiResponse<RevenueByPitchType[]>>('/admin/reports/revenue-by-pitch-type', {
      params,
    });
    return response.data.data || [];
  },
};
