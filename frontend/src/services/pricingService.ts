import api from './api';
import type { ApiResponse } from '../types/common';
import type { 
  PitchTypePricingMatrix, 
  SavePitchTypePricingRequest, 
  PriceMatrixResponse 
} from '../types/pricing';

/**
 * =========================================================================================
 * SERVICE QUẢN TRỊ BẢNG GIÁ MA TRẬN (PRICING SERVICE)
 * =========================================================================================
 * Tương tác với AdminPriceMatrixController (/api/v1/admin/pricing)
 */
export const pricingService = {
  /**
   * Lấy cấu hình ma trận bảng giá của một loại sân cụ thể
   * @param pitchTypeId ID loại sân (ví dụ 1 cho Sân 5, 2 cho Sân 7)
   */
  getPricingByPitchTypeId: async (pitchTypeId: number): Promise<PitchTypePricingMatrix> => {
    const response = await api.get<ApiResponse<PitchTypePricingMatrix>>(
      `/admin/pricing/pitch-types/${pitchTypeId}`
    );
    return response.data.data!;
  },

  /**
   * Lưu hoặc cập nhật đồng loạt (Upsert) danh sách ô giá cho loại sân
   * @param pitchTypeId ID loại sân
   * @param data Danh sách các ô giá rates: [{ dayType, isPeakHour, price }]
   */
  savePricingForPitchType: async (
    pitchTypeId: number,
    data: SavePitchTypePricingRequest
  ): Promise<PitchTypePricingMatrix> => {
    const response = await api.put<ApiResponse<PitchTypePricingMatrix>>(
      `/admin/pricing/pitch-types/${pitchTypeId}`,
      data
    );
    return response.data.data!;
  },

  /**
   * Lấy toàn bộ ma trận giá của tất cả các loại sân trong hệ thống
   */
  getAllPricingMatrices: async (): Promise<PitchTypePricingMatrix[]> => {
    const response = await api.get<ApiResponse<PitchTypePricingMatrix[]>>(
      '/admin/pricing/matrix'
    );
    return response.data.data || [];
  },

  /**
   * Lấy danh sách phẳng tất cả các ô giá (có thể lọc theo pitchTypeId)
   */
  getAllPrices: async (pitchTypeId?: number): Promise<PriceMatrixResponse[]> => {
    const params = pitchTypeId ? { pitchTypeId } : {};
    const response = await api.get<ApiResponse<PriceMatrixResponse[]>>(
      '/admin/pricing',
      { params }
    );
    return response.data.data || [];
  },

  /**
   * Xóa một ô giá cụ thể theo ID
   */
  deleteSinglePrice: async (id: number): Promise<void> => {
    await api.delete<ApiResponse<void>>(`/admin/pricing/${id}`);
  },
};

export default pricingService;
