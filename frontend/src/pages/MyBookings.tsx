import React, { useState, useEffect, useCallback } from 'react';
import { Search, Loader2, CalendarCheck, Trash2, Clock, ChevronRight } from 'lucide-react';
import { bookingService, type BookingSummaryResponse, type BookingResponse } from '../services/bookingService.ts';
import { showToast } from '../utils/toast.ts';
import { formatDate } from '../utils/formatUtils.ts';
import { BookingCancelModal } from '../components/booking/BookingCancelModal.tsx';
import { BookingDetailModal } from '../components/booking/BookingDetailModal.tsx';

const MyBookings: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'ALL' | 'UPCOMING' | 'HISTORY'>('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [bookings, setBookings] = useState<BookingSummaryResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [selectedBooking, setSelectedBooking] = useState<BookingResponse | null>(null);
  const [detailLoadingId, setDetailLoadingId] = useState<number | null>(null);

  // Tải danh sách đơn đặt sân cá nhân từ token
  const fetchBookings = useCallback(async () => {
    try {
      setLoading(true);
      const res = await bookingService.getMyBookings({ page: 1, size: 100 });
      if (res && res.data && res.data.items) {
        setBookings(res.data.items);
      } else {
        setBookings([]);
      }
    } catch (err: any) {
      console.error('Lỗi khi tải lịch sử đặt sân:', err);
      setBookings([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchBookings();
  }, [fetchBookings]);

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  // Badge trạng thái theo chuẩn thiết kế
  const getStatusBadge = (status: string) => {
    switch (status) {
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
        return <span className="booking-badge default">{status}</span>;
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
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
        return status;
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

  const [cancellingBooking, setCancellingBooking] = useState<BookingSummaryResponse | null>(null);

  // Mở modal xác nhận hủy đơn
  const handleOpenCancelModal = (booking: BookingSummaryResponse) => {
    setCancellingBooking(booking);
  };

  // Xác nhận gửi yêu cầu hủy đơn kèm lý do
  const handleConfirmCancel = async (bookingId: number, reason: string) => {
    // Đảm bảo luôn đóng popup modal ngay lập tức kể cả khi có exception
    setCancellingBooking(null);
    try {
      await bookingService.cancelBooking(bookingId, reason);
      showToast('Yêu cầu hủy đơn đã được gửi thành công!', 'success');
      await fetchBookings();
    } catch (err: any) {
      showToast(err?.response?.data?.message || 'Không thể hủy đơn đặt sân', 'error');
    }
  };

  // Thống kê số lượng cho các thẻ tổng quan
  const totalCount = bookings.length;
  const cancelledCount = bookings.filter((b) => {
    const s = String(b.status);
    return s === 'CANCELLED' || s === 'REFUNDED';
  }).length;

  const activeCount = bookings.filter((b) => {
    const s = String(b.status);
    return s === 'CONFIRMED' || s === 'COMPLETED' || s === 'IN_PROGRESS' || s === 'WAITING_APPROVAL' || s === 'WAITING_CANCELLATION';
  }).length;

  const filteredBookings = bookings.filter((b) => {
    const status = String(b.status);
    if (
      activeTab === 'UPCOMING' &&
      status !== 'CONFIRMED' &&
      status !== 'IN_PROGRESS' &&
      status !== 'PENDING_CANCEL' &&
      status !== 'WAITING_CANCELLATION' &&
      status !== 'WAITING_APPROVAL' &&
      status !== 'PENDING_HOLD'
    ) {
      return false;
    }

    if (
      activeTab === 'HISTORY' &&
      status !== 'COMPLETED' &&
      status !== 'CANCELLED' &&
      status !== 'REFUNDED'
    ) {
      return false;
    }

    if (searchTerm.trim()) {
      const term = searchTerm.trim().toLowerCase();
      const codeMatch = b.bookingCode && b.bookingCode.toLowerCase().includes(term);
      const pitchMatch = b.pitchName && b.pitchName.toLowerCase().includes(term);
      if (!codeMatch && !pitchMatch) {
        return false;
      }
    }

    return true;
  });

  return (
    <div className="bookings-page-container">
      {/* =========================================================================
          1. BẢN DESKTOP (Dựa trên image_5.png)
          ========================================================================= */}
      <div className="bookings-desktop-view">
        {/* Hàng 3 thẻ thống kê tổng quan Desktop */}
        <div className="bookings-overview-desktop">
          {/* Thẻ 1: Tổng đơn đặt */}
          <div className="bookings-stat-card">
            <div className="bookings-stat-icon green">
              <CalendarCheck size={28} />
            </div>
            <div className="bookings-stat-info">
              <p className="bookings-stat-title">Tổng đơn đặt</p>
              <p className="bookings-stat-value">{totalCount} đơn</p>
            </div>
          </div>

          {/* Thẻ 2: Đơn đã hủy */}
          <div className="bookings-stat-card">
            <div className="bookings-stat-icon red">
              <Trash2 size={26} />
            </div>
            <div className="bookings-stat-info">
              <p className="bookings-stat-title">Đơn đã hủy</p>
              <p className="bookings-stat-value">{cancelledCount} đơn</p>
            </div>
          </div>

          {/* Thẻ 3: Doanh thu / Chờ duyệt */}
          <div className="bookings-stat-card">
            <div className="bookings-stat-icon blue">
              <Clock size={28} />
            </div>
            <div className="bookings-stat-info">
              <p className="bookings-stat-title">Đã hoàn thành / Chờ duyệt</p>
              <p className="bookings-stat-value">{activeCount} đơn</p>
            </div>
          </div>
        </div>

        {/* Khung Card Bảng Dữ Liệu Desktop */}
        <div className="bookings-desktop-card">
          {/* Toolbar: Tabs điều hướng và Tìm kiếm */}
          <div className="bookings-toolbar">
            {/* Tabs: Tất cả | Sắp tới | Lịch sử */}
            <div className="bookings-tabs">
              <button
                type="button"
                onClick={() => setActiveTab('ALL')}
                className={`bookings-tab-btn ${activeTab === 'ALL' ? 'active' : ''}`}
              >
                Tất cả
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('UPCOMING')}
                className={`bookings-tab-btn ${activeTab === 'UPCOMING' ? 'active' : ''}`}
              >
                Sắp tới
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('HISTORY')}
                className={`bookings-tab-btn ${activeTab === 'HISTORY' ? 'active' : ''}`}
              >
                Lịch sử
              </button>
            </div>

            {/* Ô Tìm Kiếm Desktop */}
            <div className="bookings-desktop-search">
              <Search size={16} style={{ color: 'var(--color-text-muted)', flexShrink: 0 }} />
              <input
                type="text"
                placeholder="Tìm theo mã đơn..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="bookings-search-input"
              />
            </div>
          </div>

          {/* Bảng Dữ Liệu Desktop với Spacing rộng rãi */}
          <div className="bookings-table-wrapper">
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
                ) : filteredBookings.length === 0 ? (
                  <tr>
                    <td colSpan={6} style={{ padding: '4rem 1rem', textAlign: 'center', fontSize: '0.9rem', color: 'var(--color-text-muted)' }}>
                      {searchTerm ? 'Không tìm thấy đơn phù hợp với từ khóa' : 'Không có đơn đặt sân nào'}
                    </td>
                  </tr>
                ) : (
                  filteredBookings.map((booking) => (
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
                        <div className="booking-pitch-text" title={booking.pitchName}>
                          {booking.pitchName}
                        </div>
                        <div className="booking-time-text">
                          {booking.startTime?.substring(0, 5)} - {booking.endTime?.substring(0, 5)}
                        </div>
                      </td>

                      {/* Trạng thái */}
                      <td>
                        {getStatusBadge(booking.status)}
                      </td>

                      {/* Tổng tiền */}
                      <td>
                        <span className="booking-amount-text">
                          {formatPrice(booking.totalAmount || 0)}
                        </span>
                      </td>

                      {/* Thao tác */}
                      <td>
                        <div className="booking-actions">
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
        </div>
      </div>

      {/* =========================================================================
          2. BẢN MOBILE (Dựa trên image_4.png)
          ========================================================================= */}
      <div className="bookings-mobile-view">
        {/* Overview Carousel thu nhỏ xếp nằm ngang */}
        <div className="bookings-overview-mobile-wrapper">
          <div className="bookings-overview-mobile-scroll">
            {/* Thẻ 1: Tổng đơn đặt */}
            <div className="bookings-stat-card-mobile">
              <div className="bookings-stat-icon-mobile green">
                <CalendarCheck size={20} />
              </div>
              <span className="bookings-stat-title-mobile">Tổng đơn đặt</span>
              <span className="bookings-stat-value-mobile">{totalCount} đơn</span>
            </div>

            {/* Thẻ 2: Đơn đã hủy */}
            <div className="bookings-stat-card-mobile">
              <div className="bookings-stat-icon-mobile red">
                <Trash2 size={19} />
              </div>
              <span className="bookings-stat-title-mobile">Đơn đã hủy</span>
              <span className="bookings-stat-value-mobile">{cancelledCount} đơn</span>
            </div>

            {/* Thẻ 3: Doanh thu / Chờ duyệt */}
            <div className="bookings-stat-card-mobile">
              <div className="bookings-stat-icon-mobile blue">
                <Clock size={20} />
              </div>
              <span className="bookings-stat-title-mobile">Doanh thu / Chờ duyệt</span>
              <span className="bookings-stat-value-mobile">{activeCount} đơn</span>
            </div>
          </div>
          <div className="bookings-carousel-arrow">
            <ChevronRight size={16} />
          </div>
        </div>

        {/* Ô Tìm Kiếm Mobile đặt ngay dưới phần tổng quan */}
        <div className="bookings-mobile-search">
          <div className="bookings-mobile-search-box">
            <Search size={18} style={{ color: 'var(--color-text-muted)', flexShrink: 0 }} />
            <input
              type="text"
              placeholder="Tìm theo mã đơn..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="bookings-search-input"
            />
          </div>
        </div>

        {/* Tabs nhanh cho Mobile */}
        <div style={{ display: 'flex', gap: '1.5rem', marginBottom: '1.25rem', borderBottom: '1px solid var(--color-border)', paddingBottom: '0.5rem' }}>
          <button
            type="button"
            onClick={() => setActiveTab('ALL')}
            className={`bookings-tab-btn ${activeTab === 'ALL' ? 'active' : ''}`}
            style={{ fontSize: '0.9rem', paddingBottom: '0.5rem' }}
          >
            Tất cả
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('UPCOMING')}
            className={`bookings-tab-btn ${activeTab === 'UPCOMING' ? 'active' : ''}`}
            style={{ fontSize: '0.9rem', paddingBottom: '0.5rem' }}
          >
            Sắp tới
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('HISTORY')}
            className={`bookings-tab-btn ${activeTab === 'HISTORY' ? 'active' : ''}`}
            style={{ fontSize: '0.9rem', paddingBottom: '0.5rem' }}
          >
            Lịch sử
          </button>
        </div>

        {/* Danh Sách Đơn Hàng Dạng Thẻ Bo Tròn Mobile (Cards) */}
        <div className="bookings-mobile-list">
          {loading ? (
            <div style={{ padding: '3rem 1rem', textAlign: 'center', color: 'var(--color-text-muted)' }}>
              <Loader2 size={24} className="animate-spin" style={{ color: 'var(--color-primary)', margin: '0 auto 0.5rem auto' }} />
              <p style={{ fontSize: '0.9rem', fontWeight: 500 }}>Đang tải danh sách đặt sân...</p>
            </div>
          ) : filteredBookings.length === 0 ? (
            <div style={{ padding: '3rem 1rem', textAlign: 'center', fontSize: '0.9rem', color: 'var(--color-text-muted)', backgroundColor: 'var(--color-bg-surface)', borderRadius: '1rem', border: '1px solid var(--color-border)' }}>
              {searchTerm ? 'Không tìm thấy đơn phù hợp với từ khóa' : 'Không có đơn đặt sân nào'}
            </div>
          ) : (
            filteredBookings.map((booking) => (
              <div key={booking.id} className="bookings-mobile-card">
                {/* Header Thẻ: Mã đơn (#B1) + Badge trạng thái */}
                <div className="bookings-mobile-card-header">
                  <span className="bookings-mobile-code" title={getBookingTooltip(booking)}>
                    #{booking.bookingCode ? booking.bookingCode.replace('#', '') : booking.id}
                  </span>
                  {getStatusBadge(booking.status)}
                </div>

                {/* Thân Thẻ: Thông tin Sân, Ngày, Khung giờ, Tổng tiền */}
                <div className="bookings-mobile-card-body">
                  <div className="bookings-mobile-row">
                    <span className="bookings-mobile-label">Sân bóng:</span>
                    <span className="bookings-mobile-val" title={booking.pitchName}>{booking.pitchName}</span>
                  </div>
                  <div className="bookings-mobile-row">
                    <span className="bookings-mobile-label">Ngày đá:</span>
                    <span className="bookings-mobile-val">{formatDate(booking.bookingDate)}</span>
                  </div>
                  <div className="bookings-mobile-row">
                    <span className="bookings-mobile-label">Khung giờ:</span>
                    <span className="bookings-mobile-val">
                      {booking.startTime?.substring(0, 5)} - {booking.endTime?.substring(0, 5)}
                    </span>
                  </div>
                  <div className="bookings-mobile-row">
                    <span className="bookings-mobile-label">Tổng tiền:</span>
                    <span className="bookings-mobile-val highlight-amount">
                      {formatPrice(booking.totalAmount || 0)}
                    </span>
                  </div>
                </div>

                {/* Footer Thẻ: Nút bấm Hủy đơn / Xem chi tiết */}
                <div className="bookings-mobile-card-actions">
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
      </div>

      {/* Modal chi tiết đơn đặt sân */}
      <BookingDetailModal
        booking={selectedBooking}
        onClose={() => setSelectedBooking(null)}
        formatPrice={formatPrice}
        getStatusBadge={getStatusBadge}
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
