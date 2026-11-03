import api from './api';
import type { ApiResponse, PageResponse } from '../types/common';

export type TransactionStatus = 'PENDING' | 'SUCCESS' | 'FAILED';
export type PaymentType = 'DEPOSIT_30' | 'REMAINING_70' | 'FULL_PAYMENT' | 'REFUND';
export type SearchFieldType = 'ALL' | 'TRANSACTION_CODE' | 'BOOKING_CODE' | 'REFERENCE_CODE' | 'PHONE';

export interface TransactionSummaryResponse {
  id: number;
  transactionCode: string;
  bookingId?: number;
  amount: number;
  paymentMethodCode?: string;
  paymentMethodName?: string;
  paymentType: PaymentType;
  status: TransactionStatus;
  createdAt: string;
}

export interface TransactionDetailResponse {
  id: number;
  transactionCode: string;
  amount: number;
  paymentType: PaymentType;
  status: TransactionStatus;
  referenceCode?: string;
  note?: string;
  createdAt: string;

  paymentMethodId?: number;
  paymentMethodCode?: string;
  paymentMethodName?: string;

  processedById?: number;
  processedByName?: string;
  processedByEmail?: string;

  bookingId?: number;
  bookingCode?: string;
  bookingStatus?: string;
  customerName?: string;
  customerPhone?: string;
  pitchName?: string;
  bookingDate?: string;
  timeSlot?: string;
  bookingTotalAmount?: number;
  bookingAdditionalFee?: number;
  bookingDepositAmount?: number;
  bookingRemainingAmount?: number;
}

export interface TransactionFilterParams {
  page?: number;
  pageSize?: number;
  keyword?: string;
  searchField?: SearchFieldType;
  status?: string;
  paymentType?: string;
  paymentMethodCode?: string;
  fromDate?: string;
  toDate?: string;
  minAmount?: number;
  maxAmount?: number;
  sortBy?: string;
  sortDir?: string;
}

export const transactionService = {
  /**
   * Lấy danh sách giao dịch thanh toán phân trang kèm bộ lọc
   */
  getTransactions: async (params?: TransactionFilterParams): Promise<PageResponse<TransactionSummaryResponse>> => {
    const cleanParams: Record<string, any> = {};
    if (params) {
      if (params.page) cleanParams.page = params.page;
      if (params.pageSize) cleanParams.pageSize = params.pageSize;
      if (params.sortBy) cleanParams.sortBy = params.sortBy;
      if (params.sortDir) cleanParams.sortDir = params.sortDir;
      if (params.keyword?.trim()) cleanParams.keyword = params.keyword.trim();
      if (params.searchField && params.searchField !== 'ALL') cleanParams.searchField = params.searchField;
      if (params.status) cleanParams.status = params.status;
      if (params.paymentType) cleanParams.paymentType = params.paymentType;
      if (params.paymentMethodCode) cleanParams.paymentMethodCode = params.paymentMethodCode;
      if (params.fromDate) cleanParams.fromDate = params.fromDate;
      if (params.toDate) cleanParams.toDate = params.toDate;
      if (params.minAmount !== undefined) cleanParams.minAmount = params.minAmount;
      if (params.maxAmount !== undefined) cleanParams.maxAmount = params.maxAmount;
    }

    const res = await api.get<ApiResponse<PageResponse<TransactionSummaryResponse>>>('/admin/transactions', {
      params: cleanParams,
    });
    return res.data.data!;
  },

  /**
   * Lấy chi tiết toàn diện của một giao dịch thanh toán
   */
  getTransactionDetail: async (id: number): Promise<TransactionDetailResponse> => {
    const res = await api.get<ApiResponse<TransactionDetailResponse>>(`/admin/transactions/${id}`);
    return res.data.data!;
  },

  /**
   * Xuất danh sách giao dịch ra file Excel (.xlsx) theo bộ lọc hiện tại (bỏ phân trang)
   */
  exportExcel: async (params?: TransactionFilterParams): Promise<Blob> => {
    const cleanParams: Record<string, any> = {};
    if (params) {
      if (params.sortBy) cleanParams.sortBy = params.sortBy;
      if (params.sortDir) cleanParams.sortDir = params.sortDir;
      if (params.keyword?.trim()) cleanParams.keyword = params.keyword.trim();
      if (params.searchField && params.searchField !== 'ALL') cleanParams.searchField = params.searchField;
      if (params.status) cleanParams.status = params.status;
      if (params.paymentType) cleanParams.paymentType = params.paymentType;
      if (params.paymentMethodCode) cleanParams.paymentMethodCode = params.paymentMethodCode;
      if (params.fromDate) cleanParams.fromDate = params.fromDate;
      if (params.toDate) cleanParams.toDate = params.toDate;
      if (params.minAmount !== undefined) cleanParams.minAmount = params.minAmount;
      if (params.maxAmount !== undefined) cleanParams.maxAmount = params.maxAmount;
    }

    const res = await api.get('/admin/transactions/export-excel', {
      params: cleanParams,
      responseType: 'blob',
    });
    return res.data;
  },
};

export default transactionService;

