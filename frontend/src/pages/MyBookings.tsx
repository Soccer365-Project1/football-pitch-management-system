import React, { useState, useEffect, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search, Loader2, CalendarCheck, Trash2, Clock, ChevronLeft, ChevronRight, X } from 'lucide-react';
import {
  bookingService,
  type BookingSummaryResponse,
  type BookingResponse,
  type SearchFieldType,
  type BookingFilterParams
} from '../services/bookingService.ts';
import { showToast, showAlert } from '../utils/toast.ts';
import { formatDate } from '../utils/formatUtils.ts';
import { BookingCancelModal } from '../components/booking/BookingCancelModal.tsx';
import { BookingDetailModal } from '../components/booking/BookingDetailModal.tsx';
import { useMyBookings } from '../hooks/queries/useBookingQueries.ts';
import { useScheduleWebSocket } from '../hooks/useScheduleWebSocket.ts';
import { useQueryClient } from '@tanstack/react-query';

const STATUS_OPTIONS: { value: string; label: string }[] = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'PENDING_HOLD', label: 'Chờ đặt cọc' },
  { value: 'WAITING_APPROVAL', label: 'Chờ duyệt đơn' },
  { value: 'CONFIRMED', label: 'Đã xác nhận' },
  { value: 'IN_PROGRESS', label: 'Đang sử dụng sân' },
  { value: 'COMPLETED', label: 'Hoàn thành' },
  { value: 'WAITING_CANCELLATION', label: 'Chờ xác nhận hủy' },
  { value: 'CANCELLED', label: 'Đã hủy' },
  { value: 'REFUNDED', label: 'Đã hoàn tiền' },
];

const SEARCH_FIELD_OPTIONS: { value: SearchFieldType; label: string; placeholder: string }[] = [
  { value: 'ALL', label: 'Tất cả', placeholder: 'Tìm mã đơn, tên sân, SĐT...' },
  { value: 'BOOKING_CODE', label: 'Mã đơn', placeholder: 'Nhập mã đơn (VD: #B...)' },
  { value: 'PITCH_NAME', label: 'Tên sân', placeholder: 'Nhập tên sân bóng...' },
  { value: 'PHONE', label: 'Số điện thoại', placeholder: 'Nhập số điện thoại khách...' },
];

const MyBookings: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();

  // Đọc trực tiếp các giá trị bộ lọc & phân trang từ URL Search Params (URL-based state)
  const status = searchParams.get('status') || '';
  const searchField = (searchParams.get('searchField') as SearchFieldType) || 'ALL';
  const urlKeyword = searchParams.get('keyword') || '';
  const fromDate = searchParams.get('fromDate') || '';
  const toDate = searchParams.get('toDate') || '';
  const page = Math.max(1, parseInt(searchParams.get('page') || '1', 10));
  const pageSize = 10;

  // State nội bộ cho ô input từ khóa (để gõ phím mượt mà trước khi debounce sync lên URL)
  const [keywordInput, setKeywordInput] = useState<string>(urlKeyword);

  // Các state modal & thao tác
  const [selectedBooking, setSelectedBooking] = useState<BookingResponse | null>(null);
  const [detailLoadingId, setDetailLoadingId] = useState<number | null>(null);
  const [repayingId, setRepayingId] = useState<number | null>(null);
  const [cancellingBooking, setCancellingBooking] = useState<BookingSummaryResponse | null>(null);

  const queryClient = useQueryClient();

  // Đồng bộ lại ô gõ từ khóa nếu URL thay đổi từ ngoài (ví dụ bấm nút Back/Forward trình duyệt)
  useEffect(() => {
    setKeywordInput(urlKeyword);
  }, [urlKeyword]);

  // Cập nhật URL Query Params
  const updateParams = (newValues: {
    status?: string;
    searchField?: SearchFieldType;
    keyword?: string;
    fromDate?: string;
    toDate?: string;
    page?: number;
  }) => {
    setSearchParams(
      (prev) => {
        const next = new URLSearchParams(prev);

        if (newValues.status !== undefined) {
          if (newValues.status) next.set('status', newValues.status);
          else next.delete('status');
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

        if (newValues.page !== undefined) {
          if (newValues.page > 1) next.set('page', String(newValues.page));
          else next.delete('page');
        }

        return next;
      },
      { replace: true }
    );
  };

  // Debounce 500ms cho ô tìm kiếm từ khóa: Khi ngừng gõ 500ms thì cập nhật lên URL
  useEffect(() => {
    const timer = setTimeout(() => {
      const trimmed = keywordInput.trim();
      if (trimmed !== urlKeyword) {
        updateParams({ keyword: trimmed, page: 1 });
      }
    }, 500);

    return () => clearTimeout(timer);
  }, [keywordInput, urlKeyword]);

  // Phím Enter để tìm kiếm ngay lập tức lên URL
  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      const trimmed = keywordInput.trim();
      if (trimmed !== urlKeyword) {
        updateParams({ keyword: trimmed, page: 1 });
      }
    }
  };

  // Gom toàn bộ tham số gửi lên server
  const queryParams: BookingFilterParams = useMemo(() => {
    const params: BookingFilterParams = {
      page,
      pageSize,
      sortBy: 'createdAt',
      sortDir: 'DESC',
    };
    if (status) params.status = status;
    if (urlKeyword) {
      params.keyword = urlKeyword;
      params.searchField = searchField;
    }
    if (fromDate) params.fromDate = fromDate;
    if (toDate) params.toDate = toDate;
    return params;
  }, [page, pageSize, status, urlKeyword, searchField, fromDate, toDate]);

  // Gọi React Query lấy dữ liệu từ Backend
  const { data: pageData, isLoading: loading, isFetching } = useMyBookings(queryParams);

  const bookings: BookingSummaryResponse[] = pageData?.items || pageData?.content || [];
  const totalElements = pageData?.totalElements || 0;
  const totalPages = pageData?.totalPages || 1;

  // Lắng nghe WebSocket real-time
  useScheduleWebSocket();

  // Đổi trạng thái -> Cập nhật URL ngay
  const handleStatusChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    updateParams({ status: e.target.value, page: 1 });
  };

  // Đổi trường tìm kiếm -> Cập nhật URL ngay
  const handleSearchFieldChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    updateParams({ searchField: e.target.value as SearchFieldType, page: 1 });
  };

  // Xóa trắng ô từ khóa
  const handleClearKeyword = () => {
    setKeywordInput('');
    updateParams({ keyword: '', page: 1 });
  };

  // Xóa bộ lọc ngày
  const handleClearDateFilter = () => {
    updateParams({ fromDate: '', toDate: '', page: 1 });
  };

  // Đổi trang
  const handlePageChange = (newPage: number) => {
    updateParams({ page: newPage });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // Thanh toán cọc lại (Repay)
  const handleRepay = async (bookingId: number) => {
    try {
      setRepayingId(bookingId);
      const res = await bookingService.repayBooking(bookingId);
      if (res && res.data && res.data.paymentUrl) {
        window.location.href = res.data.paymentUrl;
      } else {
        showToast('Không nhận được liên kết thanh toán từ máy chủ', 'error');
      }
    } catch (err: any) {
      setSelectedBooking((prev) =>
        prev && prev.id === bookingId ? { ...prev, status: 'CANCELLED' } : prev
      );

      const errorMsg =
        err?.response?.data?.message ||
        'Thời gian giữ chỗ còn lại dưới 2 phút không đủ để thanh toán. Đơn đặt sân đã được hủy, vui lòng chọn lại khung giờ mới.';
      await showAlert('Hết hạn giữ chỗ', errorMsg, 'warning');
      queryClient.invalidateQueries({ queryKey: ['myBookings'] });
    } finally {
      setRepayingId(null);
    }
  };

  // Mở modal xem chi tiết đơn
  const handleOpenDetail = async (id: number) => {
    try {
      setDetailLoadingId(id);
      const res = await bookingService.getMyBookingDetail(id);
      if (res && res.data) {
        setSelectedBooking(res.data);
      }
    } catch (err: any) {
      showToast(err?.response?.data?.message || 'Không thể xem chi tiết đơn đặt sân', 'error');
    } finally {
      setDetailLoadingId(null);
    }
  };

  // Mở modal xác nhận hủy đơn
  const handleOpenCancelModal = (booking: BookingSummaryResponse) => {
    setCancellingBooking(booking);
  };

  // Xác nhận gửi yêu cầu hủy đơn kèm lý do
  const handleConfirmCancel = async (bookingId: number, reason: string) => {
    setCancellingBooking(null);
    try {
      await bookingService.cancelBooking(bookingId, reason);
      showToast('Yêu cầu hủy đơn đã được gửi thành công!', 'success');
      queryClient.invalidateQueries({ queryKey: ['myBookings'] });
    } catch (err: any) {
      showToast(err?.response?.data?.message || 'Không thể hủy đơn đặt sân', 'error');
    }
  };

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  // Badge trạng thái theo chuẩn thiết kế
  const getStatusBadge = (bStatus: string) => {
    switch (bStatus) {
      case 'CONFIRMED':
        return <span className="booking-badge confirmed">Đã xác nhận</span>;
      case 'IN_PROGRESS':
        return <span className="booking-badge in-progress">Đang sử dụng sân</span>;
      case 'WAITING_CANCELLATION':
      case 'PENDING_CANCEL':
        return <span className="booking-badge waiting">Chờ xác nhận hủy</span>;
      case 'WAITING_APPROVAL':
        return <span className="booking-badge waiting">Chờ duyệt đơn</span>;
      case 'COMPLETED':
        return <span className="booking-badge completed">Hoàn thành</span>;
      case 'PENDING_HOLD':
        return <span className="booking-badge waiting">Chờ đặt cọc</span>;
      case 'CANCELLED':
        return <span className="booking-badge cancelled">Đã hủy</span>;
      case 'REFUNDED':
        return <span className="booking-badge refunded">Đã hoàn tiền</span>;
      default:
        return <span className="booking-badge default">{bStatus}</span>;
    }
  };

  const getStatusText = (bStatus: string) => {
    switch (bStatus) {
      case 'CONFIRMED':
        return 'Đã xác nhận';
      case 'IN_PROGRESS':
        return 'Đang sử dụng sân';
      case 'WAITING_CANCELLATION':
      case 'PENDING_CANCEL':
        return 'Chờ xác nhận hủy';
      case 'WAITING_APPROVAL':
        return 'Chờ duyệt đơn';
      case 'COMPLETED':
        return 'Hoàn thành';
      case 'PENDING_HOLD':
        return 'Chờ đặt cọc';
      case 'CANCELLED':
        return 'Đã hủy';
      case 'REFUNDED':
        return 'Đã hoàn tiền';
      default:
        return bStatus;
    }
  };

  // Tạo thông tin chi tiết đầy đủ khi hover vào mã đơn
  const getBookingTooltip = (b: BookingSummaryResponse) => {
    const code = b.bookingCode
      ? (b.bookingCode.startsWith('#') ? b.bookingCode : `#${b.bookingCode}`)
      : `#${b.id}`;
    const time = `${b.startTime?.substring(0, 5)} - ${b.endTime?.substring(0, 5)}`;
    const statusText = getStatusText(b.status);
    const total = formatPrice(b.totalAmount || 0);
    const deposit = b.depositAmount ? formatPrice(b.depositAmount) : null;

    return [
      `Mã đơn: ${code}`,
      `Sân bóng: ${b.pitchName || 'Chưa cập nhật'}`,
      `Ngày đá: ${b.bookingDate ? formatDate(b.bookingDate) : 'Chưa cập nhật'}`,
      `Khung giờ: ${time}`,
      `Trạng thái: ${statusText}`,
      `Tổng tiền: ${total}`,
      deposit ? `Tiền đã cọc: ${deposit}` : null,
    ]
      .filter(Boolean)
      .join('\n');
  };

  // Placeholder tìm kiếm thay đổi linh hoạt theo trường đang chọn
  const currentPlaceholder =
    SEARCH_FIELD_OPTIONS.find((opt) => opt.value === searchField)?.placeholder ||
    'Tìm kiếm theo từ khóa...';

  return (
    <div className="bookings-page-container">
      {/* =========================================================================
          1. BẢN DESKTOP
          ========================================================================= */}
      <div className="bookings-desktop-view">
        {/* Hàng 3 thẻ thống kê tổng quan Desktop */}
        <div className="bookings-overview-desktop">
          {/* Thẻ 1: Kết quả tìm kiếm */}
          <div className="bookings-stat-card">
            <div className="bookings-stat-icon green">
              <CalendarCheck size={28} />
            </div>
            <div className="bookings-stat-info">
              <p className="bookings-stat-title">Tổng đơn tìm thấy</p>
              <p className="bookings-stat-value">{totalElements} đơn</p>
            </div>
          </div>

          {/* Thẻ 2: Trang hiện tại */}
          <div className="bookings-stat-card">
            <div className="bookings-stat-icon blue">
              <Clock size={28} />
            </div>
            <div className="bookings-stat-info">
              <p className="bookings-stat-title">Trang hiện tại</p>
              <p className="bookings-stat-value">
                Trang {page} / {totalPages}
              </p>
            </div>
          </div>

          {/* Thẻ 3: Trạng thái lọc */}
          <div className="bookings-stat-card">
            <div className="bookings-stat-icon red">
              <Trash2 size={26} />
            </div>
            <div className="bookings-stat-info">
              <p className="bookings-stat-title">Bộ lọc trạng thái</p>
              <p className="bookings-stat-value" style={{ fontSize: '1.05rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                {STATUS_OPTIONS.find((s) => s.value === status)?.label || 'Tất cả'}
              </p>
            </div>
          </div>
        </div>

        {/* Khung Card Bảng Dữ Liệu Desktop */}
        <div className="bookings-desktop-card">
          {/* TOOLBAR TÌM KIẾM & BỘ LỌC HYBRID */}
          <div className="bookings-filter-bar">
            {/* Cụm trái: Bộ lọc trạng thái & Ngày đá */}
            <div className="bookings-filter-left">
              {/* Dropdown Trạng thái: Chọn đổi là gọi API ngay */}
              <select
                value={status}
                onChange={handleStatusChange}
                className="bookings-status-select"
                title="Lọc trực tiếp theo trạng thái đơn"
              >
                {STATUS_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>

              {/* Lọc theo ngày đá: Từ ngày - Đến ngày */}
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <input
                  type="date"
                  value={fromDate}
                  onChange={(e) => updateParams({ fromDate: e.target.value, page: 1 })}
                  className="bookings-date-input"
                  title="Từ ngày đá"
                />
                <span style={{ color: 'var(--color-text-muted)', fontSize: '0.85rem' }}>-</span>
                <input
                  type="date"
                  value={toDate}
                  onChange={(e) => updateParams({ toDate: e.target.value, page: 1 })}
                  className="bookings-date-input"
                  title="Đến ngày đá"
                />
                {(fromDate || toDate) && (
                  <button
                    type="button"
                    onClick={handleClearDateFilter}
                    className="bookings-clear-btn"
                    title="Xóa bộ lọc ngày"
                  >
                    <X size={15} />
                  </button>
                )}
              </div>
            </div>

            {/* Cụm phải: Tìm kiếm ghép (Chọn trường + Ô nhập từ khóa có debounce 500ms) */}
            <div className="bookings-search-group">
              <select
                value={searchField}
                onChange={handleSearchFieldChange}
                className="bookings-search-field-select"
                title="Chọn trường tìm kiếm"
              >
                {SEARCH_FIELD_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>

              <div className="bookings-search-input-wrap">
                <Search size={16} style={{ color: 'var(--color-text-muted)', flexShrink: 0 }} />
                <input
                  type="text"
                  placeholder={currentPlaceholder}
                  value={keywordInput}
                  onChange={(e) => setKeywordInput(e.target.value)}
                  onKeyDown={handleKeyDown}
                  className="bookings-search-input"
                />
                {keywordInput && (
                  <button
                    type="button"
                    onClick={handleClearKeyword}
                    className="bookings-clear-btn"
                    title="Xóa từ khóa"
                  >
                    <X size={15} />
                  </button>
                )}
              </div>
            </div>
          </div>

          {/* Bảng Dữ Liệu Desktop */}
          <div className="bookings-table-wrapper" style={{ position: 'relative' }}>
            {isFetching && !loading && (
              <div
                style={{
                  position: 'absolute',
                  top: '0.5rem',
                  right: '1rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.4rem',
                  fontSize: '0.75rem',
                  color: 'var(--color-primary)',
                  fontWeight: 600,
                  zIndex: 2,
                }}
              >
                <Loader2 size={13} className="animate-spin" />
                <span>Đang đồng bộ...</span>
              </div>
            )}

            <table className="bookings-table">
              <thead>
                <tr>
                  <th style={{ width: '15%' }}>Mã đơn</th>
                  <th style={{ width: '12%' }}>Ngày đá</th>
                  <th style={{ width: '17%' }}>Sân / Khung giờ</th>
                  <th style={{ width: '18%' }}>Trạng thái</th>
                  <th style={{ width: '14%' }}>Tổng tiền</th>
                  <th style={{ width: '24%', textAlign: 'right' }}>Thao tác</th>
                </tr>
              </thead>

              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={6} style={{ padding: '4rem 1rem', textAlign: 'center', color: 'var(--color-text-muted)' }}>
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.625rem' }}>
                        <Loader2 size={22} className="animate-spin" style={{ color: 'var(--color-primary)' }} />
                        <span style={{ fontSize: '0.9rem', fontWeight: 500 }}>Đang tải danh sách đặt sân...</span>
                      </div>
                    </td>
                  </tr>
                ) : bookings.length === 0 ? (
                  <tr>
                    <td colSpan={6} style={{ padding: '4rem 1rem', textAlign: 'center', fontSize: '0.9rem', color: 'var(--color-text-muted)' }}>
                      {urlKeyword || status || fromDate || toDate
                        ? 'Không tìm thấy đơn đặt sân nào phù hợp với bộ lọc'
                        : 'Bạn chưa có đơn đặt sân nào'}
                    </td>
                  </tr>
                ) : (
                  bookings.map((booking) => (
                    <tr key={booking.id}>
                      {/* Mã đơn */}
                      <td>
                        <span className="booking-code-text" title={getBookingTooltip(booking)}>
                          #{booking.bookingCode ? booking.bookingCode.replace('#', '') : booking.id}
                        </span>
                      </td>

                      {/* Ngày đá */}
                      <td>
                        <span style={{ color: 'var(--color-text-base)', fontWeight: 500 }}>
                          {formatDate(booking.bookingDate)}
                        </span>
                      </td>

                      {/* Sân / Khung giờ */}
                      <td>
                        <span className="booking-pitch-text" title={booking.pitchName || ''}>
                          {booking.pitchName || 'Sân bóng'}
                        </span>
                        <p className="booking-time-text">
                          {booking.startTime?.substring(0, 5)} - {booking.endTime?.substring(0, 5)}
                        </p>
                      </td>

                      {/* Trạng thái */}
                      <td>{getStatusBadge(booking.status)}</td>

                      {/* Tổng tiền */}
                      <td>
                        <span className="booking-amount-text">
                          {formatPrice(booking.totalAmount)}
                        </span>
                      </td>

                      {/* Thao tác */}
                      <td>
                        <div className="booking-actions">
                          {booking.status === 'PENDING_HOLD' && (
                            <button
                              type="button"
                              disabled={repayingId === booking.id}
                              onClick={() => handleRepay(booking.id)}
                              className="booking-btn-deposit"
                            >
                              {repayingId === booking.id ? (
                                <Loader2 size={13} className="animate-spin inline mr-1" />
                              ) : null}
                              Đặt cọc
                            </button>
                          )}

                          {(booking.status === 'CONFIRMED' || booking.status === 'WAITING_APPROVAL') && (
                            <button
                              type="button"
                              onClick={() => handleOpenCancelModal(booking)}
                              className="booking-btn-cancel"
                            >
                              Hủy đơn
                            </button>
                          )}

                          {booking.status === 'COMPLETED' && (
                            <button
                              type="button"
                              disabled={detailLoadingId === booking.id}
                              onClick={() => handleOpenDetail(booking.id)}
                              className="booking-btn-pay"
                            >
                              {detailLoadingId === booking.id ? (
                                <Loader2 size={13} className="animate-spin inline mr-1" />
                              ) : null}
                              Thanh toán nốt
                            </button>
                          )}

                          <button
                            type="button"
                            disabled={detailLoadingId === booking.id}
                            onClick={() => handleOpenDetail(booking.id)}
                            className="booking-btn-detail"
                          >
                            {detailLoadingId === booking.id ? (
                              <Loader2 size={13} className="animate-spin inline mr-1" />
                            ) : null}
                            Xem chi tiết
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* THANH PHÂN TRANG DESKTOP */}
          {totalPages > 1 && (
            <div className="bookings-pagination-bar">
              <span className="bookings-pagination-info">
                Hiển thị trang <strong>{page}</strong> trên <strong>{totalPages}</strong> ({totalElements} đơn)
              </span>

              <div className="bookings-pagination-controls">
                <button
                  type="button"
                  disabled={page <= 1}
                  onClick={() => handlePageChange(Math.max(1, page - 1))}
                  className="bookings-page-btn"
                  title="Trang trước"
                >
                  <ChevronLeft size={16} />
                </button>

                {Array.from({ length: totalPages }, (_, i) => i + 1).map((p) => {
                  // Chỉ hiển thị các trang lân cận trang hiện tại
                  if (p === 1 || p === totalPages || (p >= page - 2 && p <= page + 2)) {
                    return (
                      <button
                        key={p}
                        type="button"
                        onClick={() => handlePageChange(p)}
                        className={`bookings-page-btn ${page === p ? 'active' : ''}`}
                      >
                        {p}
                      </button>
                    );
                  }
                  if (p === page - 3 || p === page + 3) {
                    return (
                      <span key={p} style={{ padding: '0 0.25rem', color: 'var(--color-text-muted)' }}>
                        ...
                      </span>
                    );
                  }
                  return null;
                })}

                <button
                  type="button"
                  disabled={page >= totalPages}
                  onClick={() => handlePageChange(Math.min(totalPages, page + 1))}
                  className="bookings-page-btn"
                  title="Trang sau"
                >
                  <ChevronRight size={16} />
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* =========================================================================
          2. BẢN MOBILE
          ========================================================================= */}
      <div className="bookings-mobile-view">
        {/* Carousel thống kê Mobile */}
        <div className="bookings-overview-mobile-wrapper">
          <div className="bookings-overview-mobile-scroll">
            <div className="bookings-stat-card-mobile">
              <div className="bookings-stat-icon-mobile green">
                <CalendarCheck size={20} />
              </div>
              <span className="bookings-stat-title-mobile">Tìm thấy</span>
              <span className="bookings-stat-value-mobile">{totalElements} đơn</span>
            </div>

            <div className="bookings-stat-card-mobile">
              <div className="bookings-stat-icon-mobile blue">
                <Clock size={20} />
              </div>
              <span className="bookings-stat-title-mobile">Trang</span>
              <span className="bookings-stat-value-mobile">{page}/{totalPages}</span>
            </div>
          </div>
          <div className="bookings-carousel-arrow">
            <ChevronRight size={16} />
          </div>
        </div>

        {/* Toolbar Mobile: Chọn trạng thái & Cụm tìm kiếm */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginBottom: '1.25rem' }}>
          {/* Dropdown Trạng thái */}
          <select
            value={status}
            onChange={handleStatusChange}
            className="bookings-status-select"
          >
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>

          {/* Lọc theo ngày đá Mobile: Từ ngày - Đến ngày */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', width: '100%' }}>
            <input
              type="date"
              value={fromDate}
              onChange={(e) => updateParams({ fromDate: e.target.value, page: 1 })}
              className="bookings-date-input"
              style={{ flex: 1, minWidth: 0 }}
              title="Từ ngày đá"
            />
            <span style={{ color: 'var(--color-text-muted)', fontSize: '0.85rem' }}>-</span>
            <input
              type="date"
              value={toDate}
              onChange={(e) => updateParams({ toDate: e.target.value, page: 1 })}
              className="bookings-date-input"
              style={{ flex: 1, minWidth: 0 }}
              title="Đến ngày đá"
            />
            {(fromDate || toDate) && (
              <button
                type="button"
                onClick={handleClearDateFilter}
                className="bookings-clear-btn"
                title="Xóa bộ lọc ngày"
              >
                <X size={15} />
              </button>
            )}
          </div>

          {/* Cụm tìm kiếm Mobile */}
          <div className="bookings-search-group">
            <select
              value={searchField}
              onChange={handleSearchFieldChange}
              className="bookings-search-field-select"
            >
              {SEARCH_FIELD_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>

            <div className="bookings-search-input-wrap">
              <Search size={16} style={{ color: 'var(--color-text-muted)', flexShrink: 0 }} />
              <input
                type="text"
                placeholder={currentPlaceholder}
                value={keywordInput}
                onChange={(e) => setKeywordInput(e.target.value)}
                onKeyDown={handleKeyDown}
                className="bookings-search-input"
              />
              {keywordInput && (
                <button
                  type="button"
                  onClick={handleClearKeyword}
                  className="bookings-clear-btn"
                >
                  <X size={15} />
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Danh Sách Đơn Hàng Dạng Thẻ Mobile */}
        <div className="bookings-mobile-list">
          {loading ? (
            <div style={{ padding: '3rem 1rem', textAlign: 'center', color: 'var(--color-text-muted)' }}>
              <Loader2 size={24} className="animate-spin" style={{ color: 'var(--color-primary)', margin: '0 auto 0.5rem auto' }} />
              <p style={{ fontSize: '0.9rem', fontWeight: 500 }}>Đang tải danh sách đặt sân...</p>
            </div>
          ) : bookings.length === 0 ? (
            <div style={{ padding: '3rem 1rem', textAlign: 'center', fontSize: '0.9rem', color: 'var(--color-text-muted)', backgroundColor: 'var(--color-bg-surface)', borderRadius: '1rem', border: '1px solid var(--color-border)' }}>
              {urlKeyword || status || fromDate || toDate
                ? 'Không tìm thấy đơn đặt sân nào phù hợp với bộ lọc'
                : 'Bạn chưa có đơn đặt sân nào'}
            </div>
          ) : (
            bookings.map((booking) => (
              <div key={booking.id} className="bookings-mobile-card">
                <div className="bookings-mobile-card-header">
                  <span className="bookings-mobile-code" title={getBookingTooltip(booking)}>
                    #{booking.bookingCode ? booking.bookingCode.replace('#', '') : booking.id}
                  </span>
                  {getStatusBadge(booking.status)}
                </div>

                <div className="bookings-mobile-card-body">
                  <div className="bookings-mobile-row">
                    <span className="bookings-mobile-label">Sân bóng:</span>
                    <span className="bookings-mobile-value font-semibold">{booking.pitchName || 'Sân bóng'}</span>
                  </div>
                  <div className="bookings-mobile-row">
                    <span className="bookings-mobile-label">Ngày đá:</span>
                    <span className="bookings-mobile-value">{formatDate(booking.bookingDate)}</span>
                  </div>
                  <div className="bookings-mobile-row">
                    <span className="bookings-mobile-label">Khung giờ:</span>
                    <span className="bookings-mobile-value">{booking.startTime?.substring(0, 5)} - {booking.endTime?.substring(0, 5)}</span>
                  </div>
                  <div className="bookings-mobile-row">
                    <span className="bookings-mobile-label">Tổng tiền:</span>
                    <span className="bookings-mobile-value amount">{formatPrice(booking.totalAmount)}</span>
                  </div>
                </div>

                <div className="bookings-mobile-card-actions">
                  {booking.status === 'PENDING_HOLD' && (
                    <button
                      type="button"
                      disabled={repayingId === booking.id}
                      onClick={() => handleRepay(booking.id)}
                      className="booking-btn-deposit"
                    >
                      {repayingId === booking.id ? (
                        <Loader2 size={13} className="animate-spin inline mr-1" />
                      ) : null}
                      Đặt cọc
                    </button>
                  )}

                  {(booking.status === 'CONFIRMED' || booking.status === 'WAITING_APPROVAL') && (
                    <button
                      type="button"
                      onClick={() => handleOpenCancelModal(booking)}
                      className="booking-btn-cancel"
                    >
                      Hủy đơn
                    </button>
                  )}

                  {booking.status === 'COMPLETED' && (
                    <button
                      type="button"
                      disabled={detailLoadingId === booking.id}
                      onClick={() => handleOpenDetail(booking.id)}
                      className="booking-btn-pay"
                    >
                      {detailLoadingId === booking.id ? (
                        <Loader2 size={13} className="animate-spin inline mr-1" />
                      ) : null}
                      Thanh toán nốt
                    </button>
                  )}

                  <button
                    type="button"
                    disabled={detailLoadingId === booking.id}
                    onClick={() => handleOpenDetail(booking.id)}
                    className="booking-btn-detail"
                  >
                    {detailLoadingId === booking.id ? (
                      <Loader2 size={13} className="animate-spin inline mr-1" />
                    ) : null}
                    Xem chi tiết
                  </button>
                </div>
              </div>
            ))
          )}
        </div>

        {/* PHÂN TRANG MOBILE */}
        {totalPages > 1 && (
          <div className="bookings-pagination-bar" style={{ justifyContent: 'center' }}>
            <div className="bookings-pagination-controls">
              <button
                type="button"
                disabled={page <= 1}
                onClick={() => handlePageChange(Math.max(1, page - 1))}
                className="bookings-page-btn"
              >
                <ChevronLeft size={16} />
              </button>
              <span style={{ fontSize: '0.85rem', fontWeight: 600, padding: '0 0.5rem' }}>
                {page} / {totalPages}
              </span>
              <button
                type="button"
                disabled={page >= totalPages}
                onClick={() => handlePageChange(Math.min(totalPages, page + 1))}
                className="bookings-page-btn"
              >
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Modal Popup Xem Chi Tiết Đơn Đặt Sân */}
      <BookingDetailModal
        booking={selectedBooking}
        onClose={() => setSelectedBooking(null)}
        formatPrice={formatPrice}
        getStatusBadge={getStatusBadge}
        onRepay={handleRepay}
        repaying={repayingId === selectedBooking?.id}
      />

      {/* Modal Popup Yêu Cầu Hủy Đơn Đặt Sân */}
      <BookingCancelModal
        booking={cancellingBooking}
        onClose={() => setCancellingBooking(null)}
        onConfirm={handleConfirmCancel}
        formatPrice={formatPrice}
      />
    </div>
  );
};

export default MyBookings;
