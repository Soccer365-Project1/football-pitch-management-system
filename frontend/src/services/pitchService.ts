import api from './api.ts';
import type { ApiResponse } from '../types/common.ts';
import type { 
  Pitch, 
  PitchType, 
  PitchStatus, 
  TimeSlot, 
  ScheduleGridResponse, 
  PitchFilterParams, 
  PitchRequest, 
  PageResponse 
} from '../types/pitch.ts';

/**
 * Service quản lý các yêu cầu API liên quan đến sân bóng (Pitch Service)
 * Hỗ trợ cả người dùng đặt sân và phân hệ Quản trị viên (Admin Dashboard)
 */
export const pitchService = {
  // ==========================================
  // 1. Phân hệ Người dùng (Customer / Booking)
  // ==========================================

  /**
   * Lấy danh sách sân hoạt động (dành cho người dùng đặt sân)
   * Hoặc lấy danh sách sân bóng hỗ trợ tìm kiếm, lọc và phân trang (dành cho Admin)
   */
  getPitches: (async (paramsOrPitchTypeId?: PitchFilterParams | string | number): Promise<any> => {
    // Nếu tham số là một object (PitchFilterParams) -> Gọi API Admin có phân trang
    if (typeof paramsOrPitchTypeId === 'object' && paramsOrPitchTypeId !== null) {
      const cleanParams: Record<string, any> = {
        page: paramsOrPitchTypeId.page || 1,
        size: paramsOrPitchTypeId.size || 10,
      };
      if (paramsOrPitchTypeId.keyword?.trim()) {
        cleanParams.keyword = paramsOrPitchTypeId.keyword.trim();
      }
      if (paramsOrPitchTypeId.pitchTypeId && paramsOrPitchTypeId.pitchTypeId !== 'ALL') {
        cleanParams.pitchTypeId = paramsOrPitchTypeId.pitchTypeId;
      }
      if (paramsOrPitchTypeId.status && paramsOrPitchTypeId.status !== 'ALL') {
        cleanParams.status = paramsOrPitchTypeId.status;
      }

      const response = await api.get<ApiResponse<PageResponse<Pitch>>>('/admin/pitches', { 
        params: cleanParams 
      });
      return response.data.data!;
    }

    // Ngược lại -> Gọi API công khai lấy danh sách sân bóng hoạt động
    const pitchTypeId = paramsOrPitchTypeId;
    const res = await api.get<ApiResponse<Pitch[]>>('/pitches', { 
      params: { pitchTypeId: pitchTypeId && pitchTypeId !== 'all' ? pitchTypeId : undefined } 
    });
    return res.data?.data || [];
  }) as {
    (params: PitchFilterParams): Promise<PageResponse<Pitch>>;
    (pitchTypeId?: number | string): Promise<Pitch[]>;
  },

  /**
   * Lấy danh sách khung giờ hoạt động
   */
  getTimeSlots: async (): Promise<TimeSlot[]> => {
    const res = await api.get<ApiResponse<TimeSlot[]>>('/timeslots');
    return res.data?.data || [];
  },

  /**
   * Lấy lưới trạng thái đặt sân theo ngày
   */
  getScheduleGrid: async (date: string, pitchTypeId?: string | number): Promise<ScheduleGridResponse> => {
    const params: any = { date };
    if (pitchTypeId && pitchTypeId !== 'all') {
      params.pitchTypeId = pitchTypeId;
    }
    const res = await api.get<ApiResponse<ScheduleGridResponse>>('/bookings/schedule-grid', { params });
    return res.data?.data || { bookings: [], prices: [] };
  },

  /**
   * Lấy danh mục tất cả loại sân bóng (Sân 5 người, Sân 7 người...)
   */
  getPitchTypes: async (): Promise<PitchType[]> => {
    const res = await api.get<ApiResponse<PitchType[]>>('/pitches/types');
    return res.data?.data || [];
  },

  // ==========================================
  // 2. Phân hệ Quản trị viên (Admin Management)
  // ==========================================

  /**
   * Lấy thông tin chi tiết một sân bóng theo ID
   */
  getPitchById: async (id: number): Promise<Pitch> => {
    const response = await api.get<ApiResponse<Pitch>>(`/admin/pitches/${id}`);
    return response.data.data!;
  },

  /**
   * Tạo mới một sân bóng
   */
  createPitch: async (data: PitchRequest): Promise<Pitch> => {
    const response = await api.post<ApiResponse<Pitch>>('/admin/pitches', data);
    return response.data.data!;
  },

  /**
   * Cập nhật thông tin sân bóng (Tên sân, Loại sân, Mô tả)
   */
  updatePitch: async (id: number, data: PitchRequest): Promise<Pitch> => {
    const response = await api.put<ApiResponse<Pitch>>(`/admin/pitches/${id}`, data);
    return response.data.data!;
  },

  /**
   * Chuyển đổi trạng thái hoạt động của sân (ACTIVE, MAINTENANCE, INACTIVE)
   */
  updatePitchStatus: async (id: number, status: PitchStatus): Promise<Pitch> => {
    const response = await api.patch<ApiResponse<Pitch>>(`/admin/pitches/${id}/status`, { status });
    return response.data.data!;
  },

  /**
   * Xóa mềm sân bóng khỏi hệ thống (Đánh dấu isDeleted = true)
   */
  deletePitch: async (id: number): Promise<void> => {
    await api.delete<ApiResponse<void>>(`/admin/pitches/${id}`);
  }
};
