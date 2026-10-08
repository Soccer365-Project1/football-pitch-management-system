/**
 * =========================================================================================
 * INTERFACES: QUẢN LÝ DANH MỤC NGÀY LỄ (HOLIDAYS)
 * =========================================================================================
 * Khớp 100% với Backend DTOs:
 * - com.fpms.dto.request.HolidayRequest
 * - com.fpms.dto.response.HolidayResponse
 */

/** Dữ liệu Ngày lễ nhận về từ Backend */
export interface Holiday {
  id: number;
  holidayDate: string; // Định dạng 'YYYY-MM-DD'
  name: string;        // Tối đa 150 ký tự
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

/** Payload gửi lên khi tạo mới hoặc cập nhật Ngày lễ */
export interface HolidayRequest {
  holidayDate: string; // Định dạng 'YYYY-MM-DD'
  name: string;
  description?: string;
}
