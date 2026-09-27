import api from './api';
import type { ApiResponse } from '../types/common';
import type { TimeSlot, TimeSlotRequest } from '../types/timeslot';

/**
 * =========================================================================================
 * SERVICE QUẢN LÝ KHUNG GIỜ (TIMESLOT SERVICE)
 * =========================================================================================
 * Đóng gói toàn bộ các hàm gọi API RESTful tương tác với AdminTimeSlotController bên Backend
 * Đường dẫn gốc: /api/v1/admin/time-slots
 */
export const timeslotService = {
  /**
   * Lấy danh sách toàn bộ các khung giờ trong ngày
   * @param activeOnly Lọc chỉ lấy các khung giờ đang kích hoạt (mặc định là true)
   * @returns Danh sách các TimeSlot được sắp xếp theo thời gian bắt đầu tăng dần
   */
  getTimeSlots: async (activeOnly: boolean = true): Promise<TimeSlot[]> => {
    const params: Record<string, any> = {};
    if (activeOnly !== undefined) {
      params.activeOnly = activeOnly;
    }
    const response = await api.get<ApiResponse<TimeSlot[]>>('/admin/time-slots', { params });
    return response.data.data || [];
  },

  /**
   * Lấy thông tin chi tiết một khung giờ theo ID
   * @param id Mã định danh của khung giờ
   * @returns Chi tiết đối tượng TimeSlot
   */
  getTimeSlotById: async (id: number): Promise<TimeSlot> => {
    const response = await api.get<ApiResponse<TimeSlot>>(`/admin/time-slots/${id}`);
    return response.data.data!;
  },

  /**
   * Thêm mới một khung giờ vào hệ thống
   * @param data Dữ liệu ca đá mới (startTime, endTime, isPeakHour)
   * @returns Đối tượng TimeSlot mới được tạo thành công
   */
  createTimeSlot: async (data: TimeSlotRequest): Promise<TimeSlot> => {
    const response = await api.post<ApiResponse<TimeSlot>>('/admin/time-slots', data);
    return response.data.data!;
  },

  /**
   * Chỉnh sửa thông tin khung giờ (thời gian hoặc phân loại giờ vàng/thường)
   * @param id Mã định danh của khung giờ cần cập nhật
   * @param data Dữ liệu cập nhật mới
   * @returns Đối tượng TimeSlot sau khi cập nhật
   */
  updateTimeSlot: async (id: number, data: TimeSlotRequest): Promise<TimeSlot> => {
    const response = await api.put<ApiResponse<TimeSlot>>(`/admin/time-slots/${id}`, data);
    return response.data.data!;
  },

  /**
   * Xóa mềm khung giờ khỏi hệ thống
   * Backend sẽ chặn xóa nếu khung giờ này đang có đơn đặt lịch chưa hoàn tất trong tương lai (Mã lỗi 3014)
   * @param id Mã định danh của khung giờ muốn xóa
   */
  deleteTimeSlot: async (id: number): Promise<void> => {
    await api.delete<ApiResponse<void>>(`/admin/time-slots/${id}`);
  },
};
