/**
 * Các định nghĩa Typescript cho Phân hệ Quản lý Đơn đặt sân dành cho Admin & Staff (SRS UC011)
 * Khớp 100% với DTOs của Backend Spring Boot
 */

export type BookingStatus =
  | 'PENDING_HOLD'
  | 'WAITING_APPROVAL'
  | 'CONFIRMED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'WAITING_CANCELLATION'
  | 'CANCELLED'
  | 'REFUNDED';

export type BookingType = 'ONLINE' | 'AT_COUNTER';

export type PaymentMethodCode = 'CASH' | 'VIETQR' | 'VNPAY' | 'MOMO';

export type PaymentType = 'DEPOSIT_30' | 'REMAINING_70' | 'FULL_PAYMENT' | 'REFUND';

export type TransactionStatus = 'SUCCESS' | 'FAILED' | 'PENDING';

export interface PaymentTransaction {
  id: number;
  transactionCode: string;
  amount: number;
  paymentType: PaymentType;
  paymentMethodName?: string;
  paymentMethodCode: string;
  status: TransactionStatus;
  referenceCode?: string;
  note?: string;
  processedByName?: string;
  createdAt: string;
}

export interface AdminBookingSummary {
  id: number;
  bookingCode: string;
  customerName: string;
  customerPhone: string;
  isMember: boolean;
  pitchName: string;
  pitchTypeName?: string;
  bookingDate: string;
  startTime: string;
  endTime: string;
  depositAmount: number;
  remainingAmount: number;
  additionalFee: number;
  totalAmount: number;
  bookingType: BookingType;
  status: BookingStatus;
  staffName?: string;
  createdAt?: string;
}

export interface AdminBookingDetail {
  id: number;
  bookingCode: string;
  customerId?: number;
  customerName: string;
  customerPhone: string;
  customerEmail?: string;
  staffName?: string;
  pitchName: string;
  pitchTypeName?: string;
  bookingDate: string;
  startTime: string;
  endTime: string;
  totalPitchAmount: number;
  depositAmount: number;
  additionalFee: number;
  remainingAmount: number;
  grandTotal: number;
  bookingType: BookingType;
  status: BookingStatus;
  customerNote?: string;
  createdAt?: string;
  updatedAt?: string;
  transactions?: PaymentTransaction[];
}

export interface AdminBookingSearchParams {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'ASC' | 'DESC';
  keyword?: string;
  status?: BookingStatus | 'ALL';
  fromDate?: string;
  toDate?: string;
  pitchId?: number;
}

export interface CounterBookingCreationPayload {
  pitchId: number;
  timeSlotId: number;
  bookingDate: string;
  customerId?: number;
  guestName: string;
  guestPhone: string;
  customerNote?: string;
  paidAmount?: number;
  paymentType?: PaymentType;
  paymentMethod?: PaymentMethodCode;
}

export interface AdminBookingCancelPayload {
  cancelReason: string;
  refundMethod: PaymentMethodCode;
  refundAmount?: number;
}

export interface CollectPaymentPayload {
  additionalFee?: number;
  paymentMethod: PaymentMethodCode;
  paymentNote?: string;
}
