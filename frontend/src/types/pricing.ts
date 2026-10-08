/**
 * =========================================================================================
 * ENUM & INTERFACES: CẤU HÌNH BIỂU GIÁ MA TRẬN (PRICING MATRIX)
 * =========================================================================================
 * Khớp 100% với DTOs Backend:
 * - com.fpms.entity.enums.DayType
 * - com.fpms.dto.request.SavePitchTypePricingRequest
 * - com.fpms.dto.request.PriceRateItemRequest
 * - com.fpms.dto.response.PitchTypePricingMatrixResponse
 * - com.fpms.dto.response.PriceRateResponse
 */

/** Phân loại ngày áp dụng biểu giá */
export const DayType = {
  WEEKDAY: 'WEEKDAY',
  WEEKEND: 'WEEKEND',
  HOLIDAY: 'HOLIDAY',
} as const;

export type DayType = (typeof DayType)[keyof typeof DayType];

/** Đơn vị ô giá cho từng bộ (Loại ngày x Khung giờ) */
export interface PriceRateItem {
  id?: number;
  dayType: DayType;
  isPeakHour: boolean;
  price: number;
}

/** Payload item gửi lên khi lưu cấu hình */
export interface PriceRateItemRequest {
  dayType: DayType;
  isPeakHour: boolean;
  price: number;
}

/** Dữ liệu biểu giá theo Loại sân (Phản hồi từ API GET /admin/pricing/pitch-types/{id}) */
export interface PitchTypePricingMatrix {
  pitchTypeId: number;
  pitchTypeName: string;
  playerCapacity?: number;
  rates: PriceRateItem[];
}

/** Payload gửi lên khi Admin bấm "Lưu Bảng giá" (PUT /admin/pricing/pitch-types/{id}) */
export interface SavePitchTypePricingRequest {
  rates: PriceRateItemRequest[];
}

/** Bản ghi phẳng biểu giá */
export interface PriceMatrixResponse {
  id: number;
  pitchTypeId: number;
  pitchTypeName: string;
  dayType: DayType;
  isPeakHour: boolean;
  price: number;
}
