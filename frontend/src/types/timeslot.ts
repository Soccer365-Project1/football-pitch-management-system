/**
 * =========================================================================================
 * ĐỊNH NGHĨA TYPES & DTOS PHÂN HỆ QUẢN LÝ KHUNG GIỜ (TIMESLOT MODULE)
 * =========================================================================================
 * Tài liệu tham chiếu: SRS Mục 3.4.15 & Backend AdminTimeSlotController
 * Dự án: Football Pitch Management System (FPMS)
 * Ticket: FPMS-103 (Subtask ST-04: Frontend Quản lý Khung giờ)
 */

/**
 * Interface đại diện cho đối tượng Khung giờ (TimeSlot) trong hệ thống
 * Phù hợp với TimeSlotResponse từ Spring Boot Backend
 */
export interface TimeSlot {
  /** Mã định danh duy nhất của khung giờ trong cơ sở dữ liệu */
  id: number;

  /** Giờ bắt đầu khung giờ theo định dạng "HH:mm" (ví dụ: "06:00", "17:30") */
  startTime: string;

  /** Giờ kết thúc khung giờ theo định dạng "HH:mm" (ví dụ: "07:30", "19:00") */
  endTime: string;

  /** Cờ đánh dấu khung giờ cao điểm / giờ vàng (true: Giờ vàng, false: Giờ thường) */
  isPeakHour: boolean;

  /** Trạng thái kích hoạt của khung giờ (true: Đang áp dụng, false: Tạm ngưng) */
  isActive: boolean;

  /** Chuỗi thời gian đã định dạng sẵn từ Backend (ví dụ: "06:00 - 07:30") */
  formattedTime?: string;

  /**
   * Đơn giá tham chiếu thực tế cho Sân 5 người (lấy từ bảng price_matrices qua Backend API)
   * null hoặc 0 nếu hệ thống chưa cấu hình giá
   */
  pricePitch5?: number | null;

  /**
   * Đơn giá tham chiếu thực tế cho Sân 7 người (lấy từ bảng price_matrices qua Backend API)
   * null hoặc 0 nếu hệ thống chưa cấu hình giá
   */
  pricePitch7?: number | null;
}

/**
 * Interface dữ liệu gửi lên Backend khi Thêm mới hoặc Cập nhật khung giờ
 * Phù hợp với TimeSlotRequest.java bên Backend Spring Boot
 */
export interface TimeSlotRequest {
  /** Giờ bắt đầu (định dạng "HH:mm", bắt buộc) */
  startTime: string;

  /** Giờ kết thúc (định dạng "HH:mm", bắt buộc, phải lớn hơn startTime) */
  endTime: string;

  /** Cờ phân loại Giờ vàng (true) hoặc Giờ thường (false) */
  isPeakHour: boolean;
}

/**
 * Phân loại bộ lọc khung giờ trên giao diện
 */
export type TimeSlotPeakFilter = 'ALL' | 'NORMAL' | 'PEAK';
