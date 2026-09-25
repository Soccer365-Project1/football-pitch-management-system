import api from './api';
import type { ApiResponse } from '../types/common';
import type { 
  Pitch, 
  PitchType, 
  PitchFilterParams, 
  PitchRequest, 
  PageResponse 
} from '../types/pitch';

/**
 * Service quản lý các yêu cầu API liên quan đến sân bóng (Pitch Service)
 * Dành cho phân hệ Quản trị viên (Admin Dashboard)
 */
export const pitchService = {
  /**
   * Lấy danh sách sân bóng hỗ trợ tìm kiếm, lọc và phân trang từ Backend
   * @param params Bộ lọc gồm: từ khóa (keyword), loại sân (pitchTypeId), trạng thái (status), trang (page), số lượng (size)
   * @returns PageResponse chứa danh sách sân bóng (items) và thông tin phân trang (totalPages, totalElements...)
   */
  getPitches: async (params: PitchFilterParams): Promise<PageResponse<Pitch>> => {
    // Làm sạch params: Chỉ gửi lên các tham số hợp lệ, loại bỏ các giá trị mặc định 'ALL' hoặc chuỗi rỗng
    const cleanParams: Record<string, any> = {
      page: params.page || 1,
      size: params.size || 10,
    };
    if (params.keyword?.trim()) {
      cleanParams.keyword = params.keyword.trim();
    }
    if (params.pitchTypeId && params.pitchTypeId !== 'ALL') {
      cleanParams.pitchTypeId = params.pitchTypeId;
    }
    if (params.status && params.status !== 'ALL') {
      cleanParams.status = params.status;
    }

    const response = await api.get<ApiResponse<PageResponse<Pitch>>>('/admin/pitches', { 
      params: cleanParams 
    });
    return response.data.data!;
  },

  /**
   * Lấy danh mục tất cả loại sân bóng (Sân 5 người, Sân 7 người...)
   * Phục vụ hiển thị dropdown bộ lọc và select trong modal thêm/sửa
   */
  getPitchTypes: async (): Promise<PitchType[]> => {
    const response = await api.get<ApiResponse<PitchType[]>>('/admin/pitches/types');
    return response.data.data!;
  },

  /**
   * Lấy thông tin chi tiết một sân bóng theo ID
   * @param id Mã định danh sân bóng
   */
  getPitchById: async (id: number): Promise<Pitch> => {
    const response = await api.get<ApiResponse<Pitch>>(`/admin/pitches/${id}`);
    return response.data.data!;
  },

  /**
   * Tạo mới một sân bóng
   * @param data Dữ liệu sân mới gồm: name (bắt buộc), pitchTypeId (bắt buộc), description (tùy chọn)
   */
  createPitch: async (data: PitchRequest): Promise<Pitch> => {
    const response = await api.post<ApiResponse<Pitch>>('/admin/pitches', data);
    return response.data.data!;
  },

  /**
   * Cập nhật thông tin sân bóng (Tên sân, Loại sân, Mô tả)
   * @param id Mã sân bóng cần cập nhật
   * @param data Dữ liệu cập nhật
   */
  updatePitch: async (id: number, data: PitchRequest): Promise<Pitch> => {
    const response = await api.put<ApiResponse<Pitch>>(`/admin/pitches/${id}`, data);
    return response.data.data!;
  },

  /**
   * Chuyển đổi trạng thái hoạt động của sân (ACTIVE ⇄ MAINTENANCE)
   * @param id Mã sân bóng
   * @param status Trạng thái mới: 'ACTIVE' (Hoạt động) hoặc 'MAINTENANCE' (Bảo trì)
   */
  updatePitchStatus: async (id: number, status: 'ACTIVE' | 'MAINTENANCE'): Promise<Pitch> => {
    const response = await api.patch<ApiResponse<Pitch>>(`/admin/pitches/${id}/status`, { status });
    return response.data.data!;
  },

  /**
   * Xóa mềm sân bóng khỏi hệ thống (Đánh dấu isDeleted = true)
   * @param id Mã sân bóng cần xóa
   */
  deletePitch: async (id: number): Promise<void> => {
    await api.delete<ApiResponse<void>>(`/admin/pitches/${id}`);
  }
};
