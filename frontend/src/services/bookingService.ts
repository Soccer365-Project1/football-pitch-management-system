import api from './api.ts';

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
  status: 'SUCCESS' | 'CANCELLED' | 'FAILED' | 'INVALID_SIGNATURE';
  bookingCode?: string;
  bookingId?: number;
  amount?: number;
  transactionNo?: string;
  bankCode?: string;
  payDate?: string;
  message?: string;
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
  }
};
