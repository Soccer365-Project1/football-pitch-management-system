import api from './api.ts';
import type { ApiResponse, PageResponse } from '../types/common.ts';

export interface BookingCreationRequest {
  pitchId: number;
  timeSlotId: number;
  bookingDate: string;
  guestName: string;
  guestPhone: string;
  customerNote?: string;
}

export interface BookingDepositResponse {
  bookingCode: string;
  bookingId: number;
  depositAmount: number;
  paymentUrl: string;
}

export interface PaymentReturnResult {
  status: 'SUCCESS' | 'CANCELLED' | 'FAILED' | 'PENDING_HOLD' | 'INVALID_SIGNATURE';
  bookingCode?: string;
  bookingId?: number;
  amount?: number;
  transactionNo?: string;
  bankCode?: string;
  payDate?: string;
  message?: string;
}

export type BookingStatusType =
  | 'PENDING_HOLD'
  | 'WAITING_APPROVAL'
  | 'CONFIRMED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'WAITING_CANCELLATION'
  | 'CANCELLED'
  | 'REFUNDED'
  | (string & {});

export interface BookingResponse {
  id: number;
  bookingCode: string;
  guestName: string;
  guestPhone: string;
  staffName?: string;
  bookingDate: string;
  priceSnapshot: number;
  startTimeSnapshot: string;
  endTimeSnapshot: string;
  pitchNameSnapshot: string;
  pitchTypeName?: string;
  totalPitchAmount: number;
  depositAmount: number;
  additionalFee: number;
  remainingAmount: number;
  bookingType: 'ONLINE' | 'AT_COUNTER';
  status: BookingStatusType;
  holdExpiresAt?: string;
  customerNote?: string;
  createdAt?: string;
}

export interface BookingSummaryResponse {
  id: number;
  bookingCode: string;
  bookingDate: string;
  pitchName: string;
  startTime: string;
  endTime: string;
  status: BookingStatusType;
  totalAmount: number;
  depositAmount?: number;
}

export type SearchFieldType = 'ALL' | 'BOOKING_CODE' | 'PITCH_NAME' | 'PHONE';

export interface BookingFilterParams {
  status?: string;
  keyword?: string;
  searchField?: SearchFieldType;
  fromDate?: string;
  toDate?: string;
  page?: number;
  pageSize?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'ASC' | 'DESC';
}

export const bookingService = {
  createBooking: async (data: BookingCreationRequest) => {
    const response = await api.post('/bookings', data);
    return response.data;
  },

  checkoutDeposit: async (data: BookingCreationRequest) => {
    const response = await api.post<{ code: number; message: string; data: BookingDepositResponse }>('/bookings/deposit-checkout', data);
    return response.data;
  },

  verifyPaymentReturn: async (queryString: string) => {
    const response = await api.get<{ code: number; message: string; data: PaymentReturnResult }>(`/payments/vnpay/payment-return?${queryString}`);
    return response.data;
  },

  getMyBookings: async (params?: BookingFilterParams) => {
    const response = await api.get<ApiResponse<PageResponse<BookingSummaryResponse>>>('/bookings/my-bookings', { params });
    return response.data;
  },

  getMyBookingDetail: async (id: number) => {
    const response = await api.get<ApiResponse<BookingResponse>>(`/bookings/my-bookings/${id}`);
    return response.data;
  },

  cancelBooking: async (id: number, reason?: string) => {
    const response = await api.put<ApiResponse<BookingResponse>>(`/bookings/${id}/cancel`, { reason });
    return response.data;
  },

  repayBooking: async (id: number) => {
    const response = await api.post<ApiResponse<BookingDepositResponse>>(`/bookings/${id}/repay`);
    return response.data;
  },

  getAdminTimeline: async (date: string, pitchTypeId?: number | string) => {
    const params: any = { date };
    if (pitchTypeId && pitchTypeId !== 'all') {
      params.pitchTypeId = pitchTypeId;
    }
    const response = await api.get<ApiResponse<import('../types/adminTimeline.ts').AdminTimelineResponse>>('/bookings/admin/timeline', { params });
    return response.data?.data;
  },

  approveBooking: async (id: number) => {
    const response = await api.put<ApiResponse<BookingResponse>>(`/bookings/admin/${id}/approve`);
    return response.data;
  },

  checkInBooking: async (id: number) => {
    const response = await api.put<ApiResponse<BookingResponse>>(`/bookings/admin/${id}/check-in`);
    return response.data;
  }
};

