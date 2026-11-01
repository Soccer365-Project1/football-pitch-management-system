import api from './api';
import type { ApiResponse, PageResponse } from '../types/common';
import type {
  AdminBookingSummary,
  AdminBookingDetail,
  AdminBookingSearchParams,
  CounterBookingCreationPayload,
  AdminBookingCancelPayload,
  CollectPaymentPayload
} from '../types/adminBooking';

/**
 * Service API Phân hệ Quản lý Đơn đặt sân dành cho Admin & Staff (SRS UC011)
 * Kết nối các endpoints của AdminBookingController (/api/v1/admin/bookings/**)
 */
export const adminBookingService = {
  /**
   * Tra cứu danh sách đơn đặt sân phân trang với bộ lọc đa tiêu chí
   * Endpoint: GET /api/v1/admin/bookings
   */
  getBookings: async (params?: AdminBookingSearchParams): Promise<PageResponse<AdminBookingSummary>> => {
    const cleanParams: Record<string, any> = {};
    if (params) {
      if (params.page) cleanParams.page = params.page;
      if (params.size) cleanParams.size = params.size;
      if (params.sortBy) cleanParams.sortBy = params.sortBy;
      if (params.sortDir) cleanParams.sortDir = params.sortDir;
      if (params.keyword?.trim()) cleanParams.keyword = params.keyword.trim();
      if (params.status && params.status !== 'ALL') cleanParams.status = params.status;
      if (params.fromDate) cleanParams.fromDate = params.fromDate;
      if (params.toDate) cleanParams.toDate = params.toDate;
      if (params.pitchId) cleanParams.pitchId = params.pitchId;
    }

    const response = await api.get<ApiResponse<PageResponse<AdminBookingSummary>>>('/admin/bookings', {
      params: cleanParams
    });
    return response.data.data!;
  },

  /**
   * Lấy chi tiết một đơn đặt sân kèm lịch sử giao dịch thanh toán
   * Endpoint: GET /api/v1/admin/bookings/{id}
   */
  getBookingDetail: async (id: number): Promise<AdminBookingDetail> => {
    const response = await api.get<ApiResponse<AdminBookingDetail>>(`/admin/bookings/${id}`);
    return response.data.data!;
  },

  /**
   * UC011.1: Tạo đơn đặt sân trực tiếp tại quầy
   * Endpoint: POST /api/v1/admin/bookings/counter
   */
  createCounterBooking: async (payload: CounterBookingCreationPayload): Promise<AdminBookingDetail> => {
    const response = await api.post<ApiResponse<AdminBookingDetail>>('/admin/bookings/counter', payload);
    return response.data.data!;
  },

  /**
   * UC011.2: Duyệt đơn đặt sân online từ WAITING_APPROVAL sang CONFIRMED
   * Endpoint: PATCH /api/v1/admin/bookings/{id}/approve
   */
  approveBooking: async (id: number): Promise<AdminBookingDetail> => {
    const response = await api.patch<ApiResponse<AdminBookingDetail>>(`/admin/bookings/${id}/approve`);
    return response.data.data!;
  },

  /**
   * UC011.3: Hủy đơn đặt sân và hoàn cọc
   * Endpoint: POST /api/v1/admin/bookings/{id}/cancel-refund
   */
  cancelAndRefundBooking: async (id: number, payload: AdminBookingCancelPayload): Promise<AdminBookingDetail> => {
    const response = await api.post<ApiResponse<AdminBookingDetail>>(`/admin/bookings/${id}/cancel-refund`, payload);
    return response.data.data!;
  },

  /**
   * UC011.5: Admin tự hủy đơn đặt sân cho khách (có tùy chọn hoàn cọc hoặc không hoàn cọc)
   * Endpoint: POST /api/v1/admin/bookings/{id}/cancel
   */
  cancelBooking: async (id: number, payload: AdminBookingCancelPayload): Promise<AdminBookingDetail> => {
    const response = await api.post<ApiResponse<AdminBookingDetail>>(`/admin/bookings/${id}/cancel`, payload);
    return response.data.data!;
  },

  /**
   * UC011.4: Thu tiền còn lại và đóng ca đá sang COMPLETED
   * Endpoint: POST /api/v1/admin/bookings/{id}/collect-payment
   */
  collectPayment: async (id: number, payload: CollectPaymentPayload): Promise<AdminBookingDetail> => {
    const response = await api.post<ApiResponse<AdminBookingDetail>>(`/admin/bookings/${id}/collect-payment`, payload);
    return response.data.data!;
  }
};
