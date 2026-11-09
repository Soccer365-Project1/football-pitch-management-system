import React, { useState, useEffect, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Search,
  Loader2,
  CreditCard,
  CheckCircle2,
  Clock,
  AlertTriangle,
  ChevronLeft,
  ChevronRight,
  X,
  Eye,
  Copy,
  Check,
  RotateCcw,
  TrendingUp,
  Receipt,
  Download
} from 'lucide-react';
import {
  transactionService,
  type TransactionSummaryResponse,
  type SearchFieldType,
  type TransactionStatus,
  type PaymentType
} from '../../services/transactionService';
import { formatPrice, formatDate } from '../../utils/formatUtils';
import { showToast } from '../../utils/toast';
import { DateInputWithPicker } from '../../components/common/DateInputWithPicker';
import { TransactionDetailModal } from '../../components/admin/transaction/TransactionDetailModal';
import { ModalOverlay } from '../../components/common/ModalOverlay';

const STATUS_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'SUCCESS', label: 'Thành công' },
  { value: 'PENDING', label: 'Đang chờ xử lý' },
  { value: 'FAILED', label: 'Thất bại' },
];

const PAYMENT_TYPE_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: 'Tất cả loại giao dịch' },
  { value: 'DEPOSIT_30', label: 'Cọc (30%)' },
  { value: 'REMAINING_70', label: 'Còn lại (70%)' },
  { value: 'FULL_PAYMENT', label: 'Toàn bộ (100%)' },
  { value: 'REFUND', label: 'Hoàn tiền cọc' },
];

const PAYMENT_METHOD_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: 'Tất cả phương thức' },
  { value: 'VNPAY', label: 'Cổng VNPAY' },
  { value: 'CASH', label: 'Tiền mặt' },
  { value: 'BANK_TRANSFER', label: 'Chuyển khoản ngân hàng' },
];

const SEARCH_FIELD_OPTIONS: { value: SearchFieldType; label: string; placeholder: string }[] = [
  { value: 'ALL', label: 'Tất cả các trường', placeholder: 'Tìm mã GD, mã đơn, đối soát, SĐT...' },
  { value: 'TRANSACTION_CODE', label: 'Mã giao dịch', placeholder: 'Nhập mã giao dịch (TXN_...)' },
  { value: 'BOOKING_CODE', label: 'Mã đơn đặt', placeholder: 'Nhập mã đơn đặt (#B...)' },
  { value: 'REFERENCE_CODE', label: 'Mã đối soát', placeholder: 'Nhập mã đối soát ngân hàng/VNPAY...' },
  { value: 'PHONE', label: 'Số điện thoại', placeholder: 'Nhập số điện thoại khách hàng...' },
];

export const AdminTransactions: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  // Đọc params từ URL
  const status = searchParams.get('status') || '';
  const paymentType = searchParams.get('paymentType') || '';
  const paymentMethodCode = searchParams.get('paymentMethodCode') || '';
  const searchField = (searchParams.get('searchField') as SearchFieldType) || 'ALL';
  const urlKeyword = searchParams.get('keyword') || '';
  const fromDate = searchParams.get('fromDate') || '';
  const toDate = searchParams.get('toDate') || '';
  const page = Math.max(1, parseInt(searchParams.get('page') || '1', 10));
  const pageSize = Math.max(10, parseInt(searchParams.get('pageSize') || '10', 10));

  // States nội bộ
  const [keywordInput, setKeywordInput] = useState<string>(urlKeyword);
  const [transactions, setTransactions] = useState<TransactionSummaryResponse[]>([]);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(false);
  const [exporting, setExporting] = useState<boolean>(false);
  const [showConfirmExport, setShowConfirmExport] = useState<boolean>(false);
  const [copiedId, setCopiedId] = useState<string | null>(null);

  // Modal Chi tiết
  const [selectedTransactionId, setSelectedTransactionId] = useState<number | null>(null);

  // Đồng bộ lại ô tìm kiếm khi URL thay đổi (nhấn Back/Forward)
  useEffect(() => {
    setKeywordInput(urlKeyword);
  }, [urlKeyword]);

  // Cập nhật URL Search Params
  const updateParams = (newValues: {
    status?: string;
    paymentType?: string;
    paymentMethodCode?: string;
    searchField?: SearchFieldType;
    keyword?: string;
    fromDate?: string;
    toDate?: string;
    page?: number;
    pageSize?: number;
  }) => {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);

        if (newValues.status !== undefined) {
          if (newValues.status) next.set('status', newValues.status);
          else next.delete('status');
        }

        if (newValues.paymentType !== undefined) {
          if (newValues.paymentType) next.set('paymentType', newValues.paymentType);
          else next.delete('paymentType');
        }

        if (newValues.paymentMethodCode !== undefined) {
          if (newValues.paymentMethodCode) next.set('paymentMethodCode', newValues.paymentMethodCode);
          else next.delete('paymentMethodCode');
        }

        if (newValues.searchField !== undefined) {
          if (newValues.searchField && newValues.searchField !== 'ALL') {
            next.set('searchField', newValues.searchField);
          } else {
            next.delete('searchField');
          }
        }

        if (newValues.keyword !== undefined) {
          const trimmed = newValues.keyword.trim();
          if (trimmed) next.set('keyword', trimmed);
          else next.delete('keyword');
        }

        if (newValues.fromDate !== undefined) {
          if (newValues.fromDate) next.set('fromDate', newValues.fromDate);
          else next.delete('fromDate');
        }

        if (newValues.toDate !== undefined) {
          if (newValues.toDate) next.set('toDate', newValues.toDate);
          else next.delete('toDate');
        }

        if (newValues.pageSize !== undefined) {
          if (newValues.pageSize !== 10) next.set('pageSize', String(newValues.pageSize));
          else next.delete('pageSize');
        }

        if (newValues.page !== undefined) {
          if (newValues.page > 1) next.set('page', String(newValues.page));
          else next.delete('page');
        }

        return next;
      },
      { replace: true }
    );
  };

  // Debounce tìm kiếm từ khóa (500ms)
  useEffect(() => {
    const timer = setTimeout(() => {
      const trimmed = keywordInput.trim();
      if (trimmed !== urlKeyword) {
        updateParams({ keyword: trimmed, page: 1 });
      }
    }, 500);

    return () => clearTimeout(timer);
  }, [keywordInput]);

  // Gọi API tải danh sách giao dịch
  useEffect(() => {
    let isMounted = true;
    setLoading(true);

    transactionService
      .getTransactions({
        page,
        pageSize,
        keyword: urlKeyword,
        searchField,
        status,
        paymentType,
        paymentMethodCode,
        fromDate,
        toDate,
      })
      .then((data: any) => {
        if (isMounted && data) {
          // PageResponse trả về items hoặc content
          const list = data.items || data.content || [];
          setTransactions(list);
          setTotalPages(data.totalPages || 1);
          setTotalElements(data.totalElements || 0);
        }
      })
      .catch((err) => {
        if (isMounted) {
          showToast(err?.response?.data?.message || 'Không thể tải danh sách giao dịch', 'error');
        }
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [page, pageSize, urlKeyword, searchField, status, paymentType, paymentMethodCode, fromDate, toDate]);

  // Tính toán nhanh thống kê trên trang hiện tại
  const pageStats = useMemo(() => {
    let successCount = 0;
    let pendingCount = 0;
    let failedCount = 0;
    let successAmount = 0;

    transactions.forEach((t) => {
      if (t.status === 'SUCCESS') {
        successCount++;
        successAmount += Number(t.amount || 0);
      } else if (t.status === 'PENDING') {
        pendingCount++;
      } else if (t.status === 'FAILED') {
        failedCount++;
      }
    });

    return { successCount, pendingCount, failedCount, successAmount };
  }, [transactions]);

  // Các handler bộ lọc
  const handleStatusChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    updateParams({ status: e.target.value, page: 1 });
  };

  const handlePaymentTypeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    updateParams({ paymentType: e.target.value, page: 1 });
  };

  const handlePaymentMethodChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    updateParams({ paymentMethodCode: e.target.value, page: 1 });
  };

  const handleSearchFieldChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    updateParams({ searchField: e.target.value as SearchFieldType, page: 1 });
  };

  const handleFromDateChange = (val: string) => {
    if (val && toDate && val > toDate) {
      showToast('Thời gian bắt đầu (Từ ngày) phải nhỏ hơn hoặc bằng thời gian kết thúc', 'error');
      return;
    }
    updateParams({ fromDate: val, page: 1 });
  };

  const handleToDateChange = (val: string) => {
    if (val && fromDate && val < fromDate) {
      showToast('Thời gian kết thúc (Đến ngày) phải lớn hơn hoặc bằng thời gian bắt đầu', 'error');
      return;
    }
    updateParams({ toDate: val, page: 1 });
  };

  const handleClearAllFilters = () => {
    setKeywordInput('');
    setSearchParams(new URLSearchParams(), { replace: true });
  };

  const handleCopy = (code: string) => {
    navigator.clipboard.writeText(code);
    setCopiedId(code);
    showToast('Đã sao chép mã giao dịch', 'success');
    setTimeout(() => setCopiedId(null), 2000);
  };

  const handleOpenExportConfirm = () => {
    if (totalElements === 0) {
      showToast('Không có dữ liệu giao dịch để xuất Excel', 'warning');
      return;
    }
    setShowConfirmExport(true);
  };

  const executeExportExcel = async () => {
    setShowConfirmExport(false);
    setExporting(true);
    try {
      const blob = await transactionService.exportExcel({
        keyword: urlKeyword,
        searchField,
        status,
        paymentType,
        paymentMethodCode,
        fromDate,
        toDate,
      });

      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      const dateStr = new Date().toISOString().split('T')[0];
      link.setAttribute('download', `GiaoDich_${dateStr}.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
      window.URL.revokeObjectURL(url);
      showToast('Xuất danh sách giao dịch ra Excel thành công!', 'success');
    } catch (error: any) {
      if (error?.response?.data instanceof Blob) {
        try {
          const text = await error.response.data.text();
          const errJson = JSON.parse(text);
          showToast(errJson.message || 'Không có dữ liệu để xuất Excel', 'warning');
        } catch {
          showToast('Xuất Excel thất bại. Vui lòng thử lại.', 'error');
        }
      } else {
        showToast(error?.response?.data?.message || 'Xuất Excel thất bại. Vui lòng thử lại.', 'error');
      }
    } finally {
      setExporting(false);
    }
  };

  const activeFilterCount = [
    status,
    paymentType,
    paymentMethodCode,
    urlKeyword,
    fromDate,
    toDate,
    searchField !== 'ALL' ? searchField : '',
  ].filter(Boolean).length;

  const currentPlaceholder =
    SEARCH_FIELD_OPTIONS.find((opt) => opt.value === searchField)?.placeholder ||
    'Tìm kiếm giao dịch...';

  const formatDateTime = (iso?: string) => {
    if (!iso) return '---';
    const d = new Date(iso);
    return d.toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getStatusBadge = (bStatus: TransactionStatus) => {
    switch (bStatus) {
      case 'SUCCESS':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.35rem',
              padding: '0.25rem 0.65rem',
              borderRadius: '9999px',
              fontSize: '0.75rem',
              fontWeight: 600,
              backgroundColor: '#ecfdf5',
              color: '#047857',
              border: '1px solid #a7f3d0',
            }}
          >
            <CheckCircle2 size={12} color="#10b981" />
            Thành công
          </span>
        );
      case 'PENDING':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.35rem',
              padding: '0.25rem 0.65rem',
              borderRadius: '9999px',
              fontSize: '0.75rem',
              fontWeight: 600,
              backgroundColor: '#fffbeb',
              color: '#b45309',
              border: '1px solid #fde68a',
            }}
          >
            <Clock size={12} color="#f59e0b" />
            Đang chờ
          </span>
        );
      case 'FAILED':
        return (
          <span
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.35rem',
              padding: '0.25rem 0.65rem',
              borderRadius: '9999px',
              fontSize: '0.75rem',
              fontWeight: 600,
              backgroundColor: '#fef2f2',
              color: '#b91c1c',
              border: '1px solid #fecaca',
            }}
          >
            <AlertTriangle size={12} color="#ef4444" />
            Thất bại
          </span>
        );
      default:
        return (
          <span
            style={{
              padding: '0.25rem 0.65rem',
              borderRadius: '0.375rem',
              fontSize: '0.75rem',
              backgroundColor: 'var(--color-bg-base)',
              color: 'var(--color-text-muted)',
            }}
          >
            {bStatus}
          </span>
        );
    }
  };

  const getPaymentTypeBadge = (type: PaymentType) => {
    switch (type) {
      case 'DEPOSIT_30':
        return (
          <span style={{ padding: '0.2rem 0.55rem', borderRadius: '0.375rem', fontSize: '0.75rem', fontWeight: 500, backgroundColor: '#eff6ff', color: '#1d4ed8', border: '1px solid #bfdbfe', whiteSpace: 'nowrap', display: 'inline-block' }}>
            Cọc (30%)
          </span>
        );
      case 'REMAINING_70':
        return (
          <span style={{ padding: '0.2rem 0.55rem', borderRadius: '0.375rem', fontSize: '0.75rem', fontWeight: 500, backgroundColor: '#eef2ff', color: '#4338ca', border: '1px solid #c7d2fe', whiteSpace: 'nowrap', display: 'inline-block' }}>
            Còn lại (70%)
          </span>
        );
      case 'FULL_PAYMENT':
        return (
          <span style={{ padding: '0.2rem 0.55rem', borderRadius: '0.375rem', fontSize: '0.75rem', fontWeight: 500, backgroundColor: '#ecfdf5', color: '#047857', border: '1px solid #a7f3d0', whiteSpace: 'nowrap', display: 'inline-block' }}>
            Toàn bộ (100%)
          </span>
        );
      case 'REFUND':
        return (
          <span style={{ padding: '0.2rem 0.55rem', borderRadius: '0.375rem', fontSize: '0.75rem', fontWeight: 500, backgroundColor: '#faf5ff', color: '#7e22ce', border: '1px solid #e9d5ff', whiteSpace: 'nowrap', display: 'inline-block' }}>
            Hoàn cọc
          </span>
        );
      default:
        return <span style={{ fontSize: '0.75rem', whiteSpace: 'nowrap' }}>{type}</span>;
    }
  };

  const selectStyle: React.CSSProperties = {
    height: '42px',
    padding: '0 0.85rem',
    fontSize: '0.875rem',
    fontWeight: 500,
    fontFamily: 'inherit',
    color: 'var(--color-text-base)',
    backgroundColor: 'var(--color-bg-surface)',
    border: '1px solid var(--color-border)',
    borderRadius: 'var(--radius-md)',
    outline: 'none',
    cursor: 'pointer',
    transition: 'border-color 0.15s ease',
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* 1. Header Trang & Tiêu đề */}
      <div>
        <h1
          style={{
            fontSize: '1.5rem',
            fontWeight: 800,
            color: 'var(--color-text-base)',
            display: 'flex',
            alignItems: 'center',
            gap: '0.65rem',
            margin: 0,
          }}
        >
          <Receipt style={{ color: 'var(--color-primary)' }} size={28} />
          Quản Lý Giao Dịch
        </h1>
        <p style={{ fontSize: '0.875rem', color: 'var(--color-text-muted)', margin: '0.25rem 0 0 0' }}>
          Theo dõi dòng tiền, đối soát giao dịch thanh toán trực tuyến và thu tiền mặt tại quầy
        </p>
      </div>

      {/* 2. Thẻ KPI Tổng Quan Nhanh */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
        <div style={{ padding: '1.15rem', borderRadius: 'var(--radius-md)', backgroundColor: 'var(--color-bg-surface)', border: '1px solid var(--color-border)', boxShadow: 'var(--shadow-sm)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', color: 'var(--color-text-muted)', fontSize: '0.75rem', fontWeight: 600, marginBottom: '0.5rem' }}>
            <span>TỔNG GIAO DỊCH</span>
            <Receipt size={18} style={{ color: 'var(--color-primary)' }} />
          </div>
          <div style={{ fontSize: '1.65rem', fontWeight: 900, color: 'var(--color-text-base)' }}>{totalElements}</div>
          <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', marginTop: '0.25rem' }}>Bản ghi trong hệ thống</div>
        </div>

        <div style={{ padding: '1.15rem', borderRadius: 'var(--radius-md)', backgroundColor: 'var(--color-bg-surface)', border: '1px solid var(--color-border)', boxShadow: 'var(--shadow-sm)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', color: 'var(--color-text-muted)', fontSize: '0.75rem', fontWeight: 600, marginBottom: '0.5rem' }}>
            <span>TIỀN THÀNH CÔNG (TRANG)</span>
            <TrendingUp size={18} color="#10b981" />
          </div>
          <div style={{ fontSize: '1.65rem', fontWeight: 900, color: '#047857' }}>{formatPrice(pageStats.successAmount)}</div>
          <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', marginTop: '0.25rem' }}>{pageStats.successCount} giao dịch thành công</div>
        </div>

        <div style={{ padding: '1.15rem', borderRadius: 'var(--radius-md)', backgroundColor: 'var(--color-bg-surface)', border: '1px solid var(--color-border)', boxShadow: 'var(--shadow-sm)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', color: 'var(--color-text-muted)', fontSize: '0.75rem', fontWeight: 600, marginBottom: '0.5rem' }}>
            <span>ĐANG CHỜ XỬ LÝ</span>
            <Clock size={18} color="#f59e0b" />
          </div>
          <div style={{ fontSize: '1.65rem', fontWeight: 900, color: '#b45309' }}>{pageStats.pendingCount}</div>
          <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', marginTop: '0.25rem' }}>Giao dịch chờ thanh toán</div>
        </div>

        <div style={{ padding: '1.15rem', borderRadius: 'var(--radius-md)', backgroundColor: 'var(--color-bg-surface)', border: '1px solid var(--color-border)', boxShadow: 'var(--shadow-sm)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', color: 'var(--color-text-muted)', fontSize: '0.75rem', fontWeight: 600, marginBottom: '0.5rem' }}>
            <span>GIAO DỊCH THẤT BẠI</span>
            <AlertTriangle size={18} color="#ef4444" />
          </div>
          <div style={{ fontSize: '1.65rem', fontWeight: 900, color: '#dc2626' }}>{pageStats.failedCount}</div>
          <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', marginTop: '0.25rem' }}>Hết hạn hoặc lỗi cổng</div>
        </div>
      </div>

      {/* 3. Toolbar Tìm Kiếm & Lọc Đa Năng */}
      <div
        style={{
          padding: '1.25rem',
          borderRadius: 'var(--radius-md)',
          backgroundColor: 'var(--color-bg-surface)',
          border: '1px solid var(--color-border)',
          boxShadow: 'var(--shadow-sm)',
          display: 'flex',
          flexDirection: 'column',
          gap: '1rem',
        }}
      >
        {/* Hàng 1: Dropdown filters + Date Picker range */}
        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '0.75rem' }}>
          {/* Trạng thái */}
          <select value={status} onChange={handleStatusChange} style={selectStyle} title="Lọc theo trạng thái giao dịch">
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>

          {/* Loại thanh toán */}
          <select value={paymentType} onChange={handlePaymentTypeChange} style={selectStyle} title="Lọc theo loại giao dịch">
            {PAYMENT_TYPE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>

          {/* Phương thức */}
          <select value={paymentMethodCode} onChange={handlePaymentMethodChange} style={selectStyle} title="Lọc theo phương thức thanh toán">
            {PAYMENT_METHOD_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>

          {/* Khoảng ngày đôi */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
            <DateInputWithPicker
              value={fromDate}
              max={toDate || undefined}
              onChange={handleFromDateChange}
              placeholder="Từ ngày"
              title="Từ ngày giao dịch"
              label="Từ ngày"
            />
            <span style={{ color: 'var(--color-text-muted)', fontSize: '0.85rem' }}>-</span>
            <DateInputWithPicker
              value={toDate}
              min={fromDate || undefined}
              onChange={handleToDateChange}
              placeholder="Đến ngày"
              title="Đến ngày giao dịch"
              label="Đến ngày"
            />
          </div>

          {/* Nút Xóa bộ lọc (dịch sang trái, gần chữ Đến ngày) */}
          {activeFilterCount > 0 && (
            <button
              onClick={handleClearAllFilters}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.4rem',
                height: '42px',
                padding: '0 0.85rem',
                fontSize: '0.75rem',
                fontWeight: 600,
                color: '#b91c1c',
                backgroundColor: '#fef2f2',
                border: '1px solid #fecaca',
                borderRadius: 'var(--radius-md)',
                cursor: 'pointer',
                transition: 'all 0.15s ease',
              }}
              title="Xóa tất cả bộ lọc hiện tại"
            >
              <RotateCcw size={13} />
              Xóa bộ lọc ({activeFilterCount})
            </button>
          )}
        </div>

        {/* Hàng 2: Tìm kiếm từ khóa (ngắn gọn) + Nút Xuất Excel đặt sát cạnh */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
          {/* Dropdown chọn trường */}
          <select
            value={searchField}
            onChange={handleSearchFieldChange}
            style={{ ...selectStyle, flexShrink: 0, minWidth: '155px' }}
          >
            {SEARCH_FIELD_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>

          {/* Ô gõ từ khóa: Chiều rộng vừa phải (360px), gọn gàng */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.65rem',
              height: '42px',
              padding: '0 0.85rem',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'var(--color-bg-base)',
              width: '360px',
              maxWidth: '100%',
            }}
          >
            <Search size={16} style={{ color: 'var(--color-text-muted)', flexShrink: 0 }} />
            <input
              type="text"
              value={keywordInput}
              onChange={(e) => setKeywordInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  updateParams({ keyword: keywordInput.trim(), page: 1 });
                }
              }}
              placeholder={currentPlaceholder}
              style={{
                background: 'transparent',
                border: 'none',
                outline: 'none',
                color: 'var(--color-text-base)',
                fontFamily: 'inherit',
                fontSize: '0.875rem',
                width: '100%',
                minWidth: 0,
              }}
            />
            {keywordInput && (
              <button
                type="button"
                onClick={() => {
                  setKeywordInput('');
                  updateParams({ keyword: '', page: 1 });
                }}
                style={{
                  background: 'none',
                  border: 'none',
                  color: 'var(--color-text-muted)',
                  cursor: 'pointer',
                  padding: '0.2rem',
                  display: 'flex',
                  alignItems: 'center',
                }}
              >
                <X size={15} />
              </button>
            )}
          </div>

          {/* Nút Xuất Excel: Đặt sát ngay cạnh ô search */}
          <button
            type="button"
            onClick={handleOpenExportConfirm}
            disabled={exporting || loading}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.45rem',
              height: '42px',
              padding: '0 1.15rem',
              backgroundColor: '#059669',
              color: '#ffffff',
              border: 'none',
              borderRadius: 'var(--radius-md)',
              fontWeight: 600,
              fontSize: '0.85rem',
              cursor: exporting || loading ? 'not-allowed' : 'pointer',
              opacity: exporting || loading ? 0.7 : 1,
              boxShadow: 'var(--shadow-sm)',
              transition: 'background-color 0.15s ease',
              flexShrink: 0,
            }}
            onMouseEnter={(e) => {
              if (!exporting && !loading) e.currentTarget.style.backgroundColor = '#047857';
            }}
            onMouseLeave={(e) => {
              if (!exporting && !loading) e.currentTarget.style.backgroundColor = '#059669';
            }}
            title="Xuất danh sách giao dịch thỏa mãn bộ lọc ra file Excel (.xlsx)"
          >
            {exporting ? (
              <Loader2 size={16} className="animate-spin" />
            ) : (
              <Download size={16} />
            )}
            <span>{exporting ? 'Đang xuất...' : 'Xuất Excel'}</span>
          </button>
        </div>
      </div>

      {/* 4. Bảng Dữ Liệu Giao Dịch */}
      <div
        style={{
          backgroundColor: 'var(--color-bg-surface)',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--color-border)',
          boxShadow: 'var(--shadow-sm)',
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <div style={{ overflowX: 'auto', minHeight: '380px' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr
                style={{
                  borderBottom: '1px solid var(--color-border)',
                  backgroundColor: 'var(--color-bg-base)',
                  fontSize: '0.75rem',
                  fontWeight: 700,
                  color: 'var(--color-text-muted)',
                  textTransform: 'uppercase',
                  letterSpacing: '0.05em',
                }}
              >
                <th style={{ padding: '0.85rem 1rem' }}>Mã Giao Dịch</th>
                <th style={{ padding: '0.85rem 1rem' }}>Loại Giao Dịch</th>
                <th style={{ padding: '0.85rem 1rem' }}>Phương Thức</th>
                <th style={{ padding: '0.85rem 1rem', textAlign: 'right' }}>Số Tiền (VNĐ)</th>
                <th style={{ padding: '0.85rem 1rem', textAlign: 'center' }}>Trạng Thái</th>
                <th style={{ padding: '0.85rem 1rem' }}>Thời Gian</th>
                <th style={{ padding: '0.85rem 1rem', textAlign: 'center' }}>Thao Tác</th>
              </tr>
            </thead>
            <tbody style={{ fontSize: '0.875rem' }}>
              {loading && (
                <tr>
                  <td colSpan={7} style={{ padding: '3.5rem 1rem', textAlign: 'center', color: 'var(--color-text-muted)' }}>
                    <Loader2 className="animate-spin mx-auto mb-2" size={32} style={{ color: 'var(--color-primary)' }} />
                    <span>Đang tải dữ liệu giao dịch...</span>
                  </td>
                </tr>
              )}

              {!loading && transactions.length === 0 && (
                <tr>
                  <td colSpan={7} style={{ padding: '3.5rem 1rem', textAlign: 'center', color: 'var(--color-text-muted)' }}>
                    <CreditCard className="mx-auto mb-3" size={44} style={{ color: 'var(--color-text-muted)', opacity: 0.5 }} />
                    <div style={{ fontWeight: 600, color: 'var(--color-text-base)' }}>Không tìm thấy giao dịch nào</div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', marginTop: '0.25rem' }}>
                      {activeFilterCount > 0
                        ? 'Thử thay đổi bộ lọc hoặc xóa từ khóa tìm kiếm'
                        : 'Chưa có bản ghi giao dịch nào phát sinh trên hệ thống'}
                    </div>
                  </td>
                </tr>
              )}

              {!loading &&
                transactions.map((t) => (
                  <tr
                    key={t.id}
                    style={{
                      borderBottom: '1px solid var(--color-border)',
                      transition: 'background-color 0.15s ease',
                    }}
                    onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'rgba(0, 0, 0, 0.02)')}
                    onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'transparent')}
                  >
                    {/* Mã giao dịch */}
                    <td style={{ padding: '0.85rem 1rem', fontFamily: 'monospace', fontWeight: 600, fontSize: '0.8rem', color: 'var(--color-text-base)' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                        <span title={t.transactionCode}>{t.transactionCode}</span>
                        <button
                          onClick={() => handleCopy(t.transactionCode)}
                          style={{
                            background: 'none',
                            border: 'none',
                            cursor: 'pointer',
                            color: 'var(--color-text-muted)',
                            padding: 0,
                            display: 'flex',
                            alignItems: 'center',
                          }}
                          title="Sao chép mã giao dịch"
                        >
                          {copiedId === t.transactionCode ? (
                            <Check size={12} color="#10b981" />
                          ) : (
                            <Copy size={12} />
                          )}
                        </button>
                      </div>
                    </td>

                    {/* Loại giao dịch */}
                    <td style={{ padding: '0.85rem 1rem' }}>{getPaymentTypeBadge(t.paymentType)}</td>

                    {/* Phương thức */}
                    <td style={{ padding: '0.85rem 1rem' }}>
                      <span style={{ fontWeight: 500, fontSize: '0.8rem', color: 'var(--color-text-base)' }}>
                        {t.paymentMethodName || t.paymentMethodCode || '---'}
                      </span>
                    </td>

                    {/* Số tiền */}
                    <td style={{ padding: '0.85rem 1rem', textAlign: 'right', fontWeight: 700 }}>
                      <span style={{ color: t.paymentType === 'REFUND' ? '#dc2626' : '#047857' }}>
                        {t.paymentType === 'REFUND' ? '-' : '+'}
                        {formatPrice(t.amount)}
                      </span>
                    </td>

                    {/* Trạng thái */}
                    <td style={{ padding: '0.85rem 1rem', textAlign: 'center' }}>{getStatusBadge(t.status)}</td>

                    {/* Thời gian */}
                    <td style={{ padding: '0.85rem 1rem', fontSize: '0.8rem', color: 'var(--color-text-muted)', whiteSpace: 'nowrap' }}>
                      {formatDateTime(t.createdAt)}
                    </td>

                    {/* Thao tác */}
                    <td style={{ padding: '0.85rem 1rem', textAlign: 'center' }}>
                      <button
                        onClick={() => setSelectedTransactionId(t.id)}
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '0.35rem',
                          padding: '0.35rem 0.65rem',
                          borderRadius: 'var(--radius-md)',
                          fontSize: '0.75rem',
                          fontWeight: 600,
                          backgroundColor: 'var(--color-bg-base)',
                          color: 'var(--color-text-base)',
                          border: '1px solid var(--color-border)',
                          cursor: 'pointer',
                          transition: 'background-color 0.15s ease',
                        }}
                        onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-border)')}
                        onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = 'var(--color-bg-base)')}
                        title="Xem chi tiết giao dịch"
                      >
                        <Eye size={13} />
                        Chi tiết
                      </button>
                    </td>
                  </tr>
                ))}
            </tbody>
          </table>
        </div>

        {/* 5. Phân Trang (Pagination) */}
        {!loading && totalElements > 0 && (
          <div
            style={{
              display: 'flex',
              flexWrap: 'wrap',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '0.85rem 1.25rem',
              borderTop: '1px solid var(--color-border)',
              backgroundColor: 'var(--color-bg-base)',
              fontSize: '0.875rem',
              color: 'var(--color-text-muted)',
              gap: '0.75rem',
            }}
          >
            <div>
              Hiển thị trang <span style={{ fontWeight: 700, color: 'var(--color-text-base)' }}>{page}</span> /{' '}
              <span style={{ fontWeight: 700, color: 'var(--color-text-base)' }}>{totalPages}</span> (Tổng{' '}
              <span style={{ fontWeight: 700, color: 'var(--color-text-base)' }}>{totalElements}</span> giao dịch)
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <button
                disabled={page <= 1}
                onClick={() => updateParams({ page: page - 1 })}
                style={{
                  padding: '0.45rem',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--color-border)',
                  backgroundColor: 'var(--color-bg-surface)',
                  color: 'var(--color-text-base)',
                  cursor: page <= 1 ? 'not-allowed' : 'pointer',
                  opacity: page <= 1 ? 0.4 : 1,
                  display: 'flex',
                  alignItems: 'center',
                }}
                title="Trang trước"
              >
                <ChevronLeft size={16} />
              </button>

              <span
                style={{
                  padding: '0.25rem 0.65rem',
                  fontSize: '0.75rem',
                  fontWeight: 700,
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'var(--color-primary)',
                  color: '#ffffff',
                }}
              >
                {page}
              </span>

              <button
                disabled={page >= totalPages}
                onClick={() => updateParams({ page: page + 1 })}
                style={{
                  padding: '0.45rem',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--color-border)',
                  backgroundColor: 'var(--color-bg-surface)',
                  color: 'var(--color-text-base)',
                  cursor: page >= totalPages ? 'not-allowed' : 'pointer',
                  opacity: page >= totalPages ? 0.4 : 1,
                  display: 'flex',
                  alignItems: 'center',
                }}
                title="Trang sau"
              >
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* 6. Modal Chi Tiết Giao Dịch */}
      <TransactionDetailModal
        isOpen={!!selectedTransactionId}
        transactionId={selectedTransactionId}
        onClose={() => setSelectedTransactionId(null)}
      />

      {/* 7. Modal Xác Nhận Xuất File Excel */}
      {showConfirmExport && (
        <ModalOverlay onClose={() => setShowConfirmExport(false)} maxWidth="500px">
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              paddingBottom: '0.85rem',
              marginBottom: '1rem',
              borderBottom: '1px solid var(--color-border)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Download style={{ color: '#059669' }} size={22} />
              <h2 style={{ fontSize: '1.2rem', fontWeight: 800, margin: 0, color: 'var(--color-text-base)' }}>
                Xác Nhận Xuất File Excel
              </h2>
            </div>
            <button
              onClick={() => setShowConfirmExport(false)}
              style={{
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                color: 'var(--color-text-muted)',
                padding: '0.2rem',
                display: 'flex',
                alignItems: 'center',
              }}
              title="Đóng"
            >
              <X size={20} />
            </button>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.9rem', marginBottom: '1.25rem' }}>
            <p style={{ fontSize: '0.875rem', color: 'var(--color-text-base)', margin: 0, lineHeight: 1.5 }}>
              Bạn có chắc chắn muốn xuất danh sách giao dịch theo các điều kiện lọc hiện tại ra file Excel (<strong>.xlsx</strong>) không?
            </p>

            <div
              style={{
                padding: '0.85rem 1rem',
                borderRadius: 'var(--radius-md)',
                backgroundColor: 'var(--color-bg-base)',
                border: '1px solid var(--color-border)',
                fontSize: '0.825rem',
                display: 'flex',
                flexDirection: 'column',
                gap: '0.45rem',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--color-text-muted)' }}>Tổng số bản ghi:</span>
                <span style={{ fontWeight: 800, color: '#047857' }}>{totalElements} giao dịch</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--color-text-muted)' }}>Trạng thái:</span>
                <span style={{ fontWeight: 600, color: 'var(--color-text-base)' }}>
                  {STATUS_OPTIONS.find((o) => o.value === status)?.label || 'Tất cả trạng thái'}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--color-text-muted)' }}>Loại giao dịch:</span>
                <span style={{ fontWeight: 600, color: 'var(--color-text-base)' }}>
                  {PAYMENT_TYPE_OPTIONS.find((o) => o.value === paymentType)?.label || 'Tất cả loại giao dịch'}
                </span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ color: 'var(--color-text-muted)' }}>Phương thức:</span>
                <span style={{ fontWeight: 600, color: 'var(--color-text-base)' }}>
                  {PAYMENT_METHOD_OPTIONS.find((o) => o.value === paymentMethodCode)?.label || 'Tất cả phương thức'}
                </span>
              </div>
              {(fromDate || toDate) && (
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--color-text-muted)' }}>Khoảng thời gian:</span>
                  <span style={{ fontWeight: 600, color: 'var(--color-text-base)' }}>
                    {fromDate ? formatDate(fromDate) : 'Từ trước'} — {toDate ? formatDate(toDate) : 'Hiện tại'}
                  </span>
                </div>
              )}
              {urlKeyword && (
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <span style={{ color: 'var(--color-text-muted)' }}>Từ khóa tìm kiếm:</span>
                  <span style={{ fontWeight: 600, color: 'var(--color-text-base)' }}>"{urlKeyword}"</span>
                </div>
              )}
            </div>

            <p style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', margin: 0, fontStyle: 'italic' }}>
              💡 File Excel được xuất sẽ bao gồm toàn bộ các giao dịch thỏa mãn bộ lọc (bỏ qua phân trang) kèm công thức đối soát tổng tiền.
            </p>
          </div>

          <div
            style={{
              display: 'flex',
              justifyContent: 'flex-end',
              gap: '0.65rem',
              paddingTop: '0.85rem',
              borderTop: '1px solid var(--color-border)',
            }}
          >
            <button
              type="button"
              onClick={() => setShowConfirmExport(false)}
              style={{
                padding: '0.5rem 1rem',
                fontSize: '0.85rem',
                fontWeight: 600,
                color: 'var(--color-text-base)',
                backgroundColor: 'var(--color-bg-base)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-md)',
                cursor: 'pointer',
              }}
            >
              Hủy
            </button>
            <button
              type="button"
              onClick={executeExportExcel}
              disabled={exporting}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.45rem',
                padding: '0.5rem 1.25rem',
                fontSize: '0.85rem',
                fontWeight: 600,
                color: '#ffffff',
                backgroundColor: '#059669',
                border: 'none',
                borderRadius: 'var(--radius-md)',
                cursor: exporting ? 'not-allowed' : 'pointer',
                opacity: exporting ? 0.7 : 1,
              }}
            >
              <Download size={15} />
              <span>Xác nhận xuất Excel</span>
            </button>
          </div>
        </ModalOverlay>
      )}
    </div>
  );
};

export default AdminTransactions;
