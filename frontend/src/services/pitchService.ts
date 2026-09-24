import api from './api';
import type { ApiResponse } from '../types/common';
import type { 
  Pitch, 
  PitchType, 
  PitchFilterParams, 
  PitchRequest, 
  PageResponse 
} from '../types/pitch';

export const pitchService = {
  // Lấy danh sách sân bóng (hỗ trợ tìm kiếm, lọc theo loại sân, trạng thái và phân trang)
  getPitches: async (params: PitchFilterParams): Promise<PageResponse<Pitch>> => {
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

  // Lấy danh mục các loại sân bóng phục vụ dropdown
  getPitchTypes: async (): Promise<PitchType[]> => {
    const response = await api.get<ApiResponse<PitchType[]>>('/admin/pitches/types');
    return response.data.data!;
  },

  // Lấy chi tiết sân bóng theo ID
  getPitchById: async (id: number): Promise<Pitch> => {
    const response = await api.get<ApiResponse<Pitch>>(`/admin/pitches/${id}`);
    return response.data.data!;
  },

  // Thêm sân bóng mới
  createPitch: async (data: PitchRequest): Promise<Pitch> => {
    const response = await api.post<ApiResponse<Pitch>>('/admin/pitches', data);
    return response.data.data!;
  },

  // Chỉnh sửa thông tin sân bóng
  updatePitch: async (id: number, data: PitchRequest): Promise<Pitch> => {
    const response = await api.put<ApiResponse<Pitch>>(`/admin/pitches/${id}`, data);
    return response.data.data!;
  },

  // Chuyển đổi trạng thái Hoạt động / Bảo trì
  updatePitchStatus: async (id: number, status: 'ACTIVE' | 'MAINTENANCE'): Promise<Pitch> => {
    const response = await api.patch<ApiResponse<Pitch>>(`/admin/pitches/${id}/status`, { status });
    return response.data.data!;
  },

  // Xóa mềm sân bóng
  deletePitch: async (id: number): Promise<void> => {
    await api.delete<ApiResponse<void>>(`/admin/pitches/${id}`);
  }
};
