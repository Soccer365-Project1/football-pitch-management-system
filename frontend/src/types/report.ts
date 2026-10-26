/**
 * Tham số lọc dữ liệu báo cáo thống kê theo khoảng thời gian
 * Tương ứng với class ReportFilterRequest.java bên Backend
 */
export interface ReportFilterParams {
  startDate: string; // Định dạng YYYY-MM-DD (ví dụ: '2026-08-17')
  endDate: string;   // Định dạng YYYY-MM-DD (ví dụ: '2026-08-23')
}

/**
 * Số liệu tổng quan 4 thẻ KPI Dashboard
 * Tương ứng với class DashboardSummaryResponse.java bên Backend
 */
export interface DashboardSummary {
  // Thẻ 1: Doanh thu
  currentRevenue: number;         // Tổng doanh thu kỳ hiện tại (VNĐ)
  previousRevenue: number;        // Tổng doanh thu kỳ trước liền kề (VNĐ)
  revenueGrowthRate: number;      // Tỷ lệ tăng trưởng doanh thu so với kỳ trước (%)

  // Thẻ 2: Tổng số đơn
  totalBookings: number;          // Tổng số lượt đặt sân trong kỳ
  completedBookings: number;      // Số đơn thành công / đã hoàn tất
  confirmedBookings: number;      // Số đơn đang xác nhận / chờ đá hoặc đang đá

  // Thẻ 3: Tỉ lệ hủy đơn
  cancelledBookings: number;      // Số đơn bị hủy
  cancellationRate: number;       // Tỉ lệ hủy đơn kỳ này (%)
  previousCancellationRate: number; // Tỉ lệ hủy đơn kỳ trước (%)
  cancellationRateDiff: number;   // Mức độ thay đổi tỉ lệ hủy so với kỳ trước (% điểm)

  // Thẻ 4: Khách hàng mới & Tỷ lệ quay lại
  newCustomersCount: number;      // Số lượng khách hàng mới đăng ký trong kỳ
  repeatCustomerRate: number;     // Tỷ lệ khách hàng quay lại đặt từ 2 lần trở lên (%)
}

/**
 * Điểm dữ liệu biểu đồ đường doanh thu theo ngày
 * Tương ứng với class RevenueTrendPointResponse.java bên Backend
 */
export interface RevenueTrendPoint {
  date: string;                   // Ngày thống kê (YYYY-MM-DD)
  dayName: string;                // Thứ trong tuần (Thứ 2, Thứ 3, ..., Chủ Nhật)
  revenue: number;                // Doanh thu trong ngày (VNĐ)
  bookingCount: number;           // Số lượng đơn đặt hoàn thành trong ngày
}

/**
 * Dữ liệu biểu đồ cột tần suất đặt sân theo thứ trong tuần
 * Tương ứng với class BookingFrequencyResponse.java bên Backend
 */
export interface BookingFrequencyPoint {
  dayOfWeekIndex: number;         // Thứ tự trong tuần (1 = Thứ 2, ..., 7 = Chủ Nhật)
  dayOfWeekName: string;          // Tên thứ hiển thị (Thứ 2, Thứ 3, ..., Chủ Nhật)
  bookingCount: number;           // Tổng số lượt đặt sân trong thứ này
}

/**
 * Dữ liệu biểu đồ tròn cơ cấu doanh thu theo phân loại sân bóng
 * Tương ứng với class RevenueByPitchTypeResponse.java bên Backend
 */
export interface RevenueByPitchType {
  pitchTypeId: number;            // ID loại sân
  pitchTypeName: string;          // Tên loại sân (Sân 5 người, Sân 7 người, ...)
  revenue: number;                // Doanh thu đóng góp (VNĐ)
  percentage: number;             // Tỷ trọng doanh thu (%)
  bookingCount: number;           // Tổng số ca đặt thành công của loại sân này
}
