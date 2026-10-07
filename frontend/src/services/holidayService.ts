import api from './api';
import type { ApiResponse } from '../types/common';
import type { Holiday, HolidayRequest } from '../types/holiday';

/**
 * =========================================================================================
 * SERVICE QUẢN TRỊ DANH MỤC NGÀY LỄ (HOLIDAY SERVICE)
 * =========================================================================================
 * Tương tác với AdminHolidayController (/api/v1/admin/holidays)
 */
export const holidayService = {
  /**
   * Lấy danh sách toàn bộ ngày lễ, có thể lọc theo năm
   * @param year Năm cần lọc (ví dụ: 2026)
   */
  getAllHolidays: async (year?: number): Promise<Holiday[]> => {
    const params = year ? { year } : {};
    const response = await api.get<ApiResponse<Holiday[]>>('/admin/holidays', { params });
    return response.data.data || [];
  },

  /**
   * Lấy chi tiết một ngày lễ theo ID
   */
  getHolidayById: async (id: number): Promise<Holiday> => {
    const response = await api.get<ApiResponse<Holiday>>(`/admin/holidays/${id}`);
    return response.data.data!;
  },

  /**
   * Thêm mới một ngày lễ vào hệ thống (tự động kiểm tra chống trùng ngày)
   */
  createHoliday: async (data: HolidayRequest): Promise<Holiday> => {
    const response = await api.post<ApiResponse<Holiday>>('/admin/holidays', data);
    return response.data.data!;
  },

  /**
   * Chỉnh sửa thông tin ngày lễ (ngày, tên, mô tả phụ thu)
   */
  updateHoliday: async (id: number, data: HolidayRequest): Promise<Holiday> => {
    const response = await api.put<ApiResponse<Holiday>>(`/admin/holidays/${id}`, data);
    return response.data.data!;
  },

  /**
   * Xóa một ngày lễ khỏi hệ thống
   */
  deleteHoliday: async (id: number): Promise<void> => {
    await api.delete<ApiResponse<void>>(`/admin/holidays/${id}`);
  },
};

export default holidayService;
