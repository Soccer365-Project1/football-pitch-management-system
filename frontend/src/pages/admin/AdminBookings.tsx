import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Search, Loader2, ChevronLeft, ChevronRight, X } from 'lucide-react';
import { adminBookingService } from '../../services/adminBookingService';
import { pitchService } from '../../services/pitchService';
import type {
  AdminBookingSummary,
  BookingStatus,
  AdminBookingSearchParams,
} from '../../types/adminBooking';
import type { Pitch } from '../../types/pitch';
import { formatPrice, formatDate } from '../../utils/formatUtils';

// Modals
import { CounterBookingModal } from '../../components/admin/booking/CounterBookingModal';
import { ApproveBookingModal } from '../../components/admin/booking/ApproveBookingModal';
import { CancelRefundModal } from '../../components/admin/booking/CancelRefundModal';
import { CollectPaymentModal } from '../../components/admin/booking/CollectPaymentModal';
import { AdminCancelModal } from '../../components/admin/booking/AdminCancelModal';

const AdminBookings: React.FC = () => {
  const dateInputRef = useRef<HTMLInputElement>(null);

  // States
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [debouncedKeyword, setDebouncedKeyword] = useState<string>('');
  const [activeTab, setActiveTab] = useState<string>('ALL');
  const [pitchFilter, setPitchFilter] = useState<string>('ALL');
  const [selectedDate, setSelectedDate] = useState<string>('');

  const [page, setPage] = useState<number>(1);
  const pageSize = 10;

  // Data
  const [bookings, setBookings] = useState<AdminBookingSummary[]>([]);
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Modal States
  const [isCreateModalOpen, setIsCreateModalOpen] = useState<boolean>(false);
  const [approveBooking, setApproveBooking] = useState<AdminBookingSummary | null>(null);
  const [checkoutBooking, setCheckoutBooking] = useState<AdminBookingSummary | null>(null);
  const [cancelBooking, setCancelBooking] = useState<AdminBookingSummary | null>(null);
  const [adminCancelBooking, setAdminCancelBooking] = useState<AdminBookingSummary | null>(null);

  // Debounce search input
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedKeyword(searchTerm.trim());
      setPage(1);
    }, 400);

    return () => clearTimeout(handler);
  }, [searchTerm]);

  useEffect(() => {
    setPage(1);
  }, [activeTab, pitchFilter, selectedDate]);

  // Load pitches list for filter
  useEffect(() => {
    pitchService
      .getPitches()
      .then((data) => {
        if (Array.isArray(data)) {
          setPitches(data);
        }
      })
      .catch((err) => {
        console.error('Không thể tải danh sách sân:', err);
      });
  }, []);

  // Fetch bookings from backend
  const loadBookings = useCallback(async () => {
    setLoading(true);
    setError(null);

    let statusParam: BookingStatus | undefined = undefined;
    if (activeTab === 'PENDING') statusParam = 'WAITING_APPROVAL';
    else if (activeTab === 'CONFIRMED') statusParam = 'CONFIRMED';
    else if (activeTab === 'IN_PROGRESS') statusParam = 'IN_PROGRESS';
    else if (activeTab === 'COMPLETED') statusParam = 'COMPLETED';
    else if (activeTab === 'PENDING_CANCEL') statusParam = 'WAITING_CANCELLATION';
    else if (activeTab === 'CANCELLED') statusParam = 'CANCELLED';

    const params: AdminBookingSearchParams = {
      page,
      size: pageSize,
      sortBy: 'createdAt',
      sortDir: 'DESC',
      keyword: debouncedKeyword || undefined,
      status: statusParam,
      pitchId: pitchFilter !== 'ALL' && !isNaN(Number(pitchFilter)) ? Number(pitchFilter) : undefined,
      fromDate: selectedDate || undefined,
      toDate: selectedDate || undefined,
    };

    try {
      const data = await adminBookingService.getBookings(params);
      setBookings(data.items || []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Không thể tải danh sách đơn đặt sân.');
      setBookings([]);
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, debouncedKeyword, activeTab, pitchFilter, selectedDate]);

  useEffect(() => {
    loadBookings();
  }, [loadBookings]);

  const getStatusBadge = (status: BookingStatus) => {
    switch (status) {
      case 'WAITING_APPROVAL':
        return (
          <span
            className="badge badge-secondary"
            style={{ backgroundColor: 'rgba(100, 116, 139, 0.1)', color: 'var(--color-text-base)' }}
          >
            Chờ xác nhận
          </span>
        );
      case 'CONFIRMED':
        return <span className="badge badge-success">Đã xác nhận</span>;
      case 'IN_PROGRESS':
        return <span className="badge badge-warning">Đang đá</span>;
      case 'COMPLETED':
        return (
          <span
            className="badge"
            style={{ backgroundColor: 'rgba(59, 130, 246, 0.15)', color: 'var(--color-secondary)' }}
          >
            Đã hoàn thành
          </span>
        );
      case 'WAITING_CANCELLATION':
        return <span className="badge badge-danger">Yêu cầu hủy</span>;
      case 'CANCELLED':
        return (
          <span className="badge badge-danger" style={{ opacity: 0.7 }}>
            Đã hủy
          </span>
        );
      case 'REFUNDED':
        return (
          <span
            className="badge"
            style={{ backgroundColor: 'var(--color-bg-base)', border: '1px solid var(--color-border)' }}
          >
            Đã hoàn tiền
          </span>
        );
      default:
        return <span className="badge">{status}</span>;
    }
  };

  return (
    <div style={{ height: 'calc(100vh - 140px)', display: 'flex', flexDirection: 'column' }}>
      <div className="card" style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        {/* Toolbar chuẩn SRS */}
        <div className="flex items-center justify-between mb-6 gap-4" style={{ flexShrink: 0 }}>
          <div className="flex gap-4 items-center">
            <h2 className="text-xl font-semibold mr-2 whitespace-nowrap">Danh sách Đơn</h2>
            <select
              className="btn btn-secondary"
              style={{
                height: '42px',
                fontWeight: 'normal',
                fontFamily: 'inherit',
                outline: 'none',
                border: '1px solid var(--color-border)',
              }}
              value={activeTab}
              onChange={(e) => setActiveTab(e.target.value)}
            >
              <option value="ALL">Trạng thái: Tất cả</option>
              <option value="PENDING">Chờ xác nhận</option>
              <option value="CONFIRMED">Đã xác nhận</option>
              <option value="IN_PROGRESS">Đang đá (Nợ phí)</option>
              <option value="COMPLETED">Đã hoàn thành</option>
              <option value="PENDING_CANCEL">Yêu cầu hủy</option>
              <option value="CANCELLED">Đã hủy</option>
            </select>
          </div>

          <div className="flex gap-2 items-center">
            <div
              className="flex items-center gap-2"
              style={{
                height: '42px',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-md)',
                backgroundColor: 'var(--color-bg-base)',
                padding: '0 0.75rem',
              }}
            >
              <Search size={16} className="text-muted" />
              <input
                type="text"
                placeholder="Tìm mã đơn, tên khách..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                style={{
                  background: 'transparent',
                  border: 'none',
                  outline: 'none',
                  color: 'var(--color-text-base)',
                  fontFamily: 'inherit',
                  width: '175px',
                }}
              />
            </div>

            <select
              className="btn btn-secondary"
              style={{
                height: '42px',
                fontWeight: 'normal',
                fontFamily: 'inherit',
                outline: 'none',
                border: '1px solid var(--color-border)',
              }}
              value={pitchFilter}
              onChange={(e) => setPitchFilter(e.target.value)}
            >
              <option value="ALL">Loại sân: Tất cả</option>
              {pitches.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </select>

            <div
              className="flex items-center gap-2"
              style={{
                height: '42px',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-md)',
                backgroundColor: 'var(--color-bg-base)',
                padding: '0 0.75rem',
                cursor: 'pointer',
              }}
              onClick={() => {
                if (dateInputRef.current) {
                  try {
                    dateInputRef.current.showPicker();
                  } catch {
                    dateInputRef.current.focus();
                  }
                }
              }}
            >
              <span className="text-muted text-sm font-medium">Ngày:</span>
              <input
                ref={dateInputRef}
                type="date"
                value={selectedDate}
                onChange={(e) => setSelectedDate(e.target.value)}
                style={{
                  background: 'transparent',
                  border: 'none',
                  outline: 'none',
                  color: 'var(--color-text-base)',
                  fontFamily: 'inherit',
                  cursor: 'pointer',
                }}
                onClick={(e) => e.stopPropagation()}
              />
              {selectedDate && (
                <button
                  type="button"
                  className="text-muted hover:text-danger ml-1"
                  onClick={(e) => {
                    e.stopPropagation();
                    setSelectedDate('');
                  }}
                  title="Xóa lọc ngày"
                >
                  <X size={14} />
                </button>
              )}
            </div>

            <button
              className="btn btn-primary whitespace-nowrap flex-shrink-0"
              style={{ height: '42px' }}
              onClick={() => setIsCreateModalOpen(true)}
            >
              Tạo đơn tại quầy
            </button>
          </div>
        </div>

        {/* Table danh sách đơn */}
        <div style={{ flex: 1, overflowY: 'auto', overflowX: 'auto', borderTop: '1px solid var(--color-border)' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead style={{ position: 'sticky', top: 0, backgroundColor: 'var(--color-bg-surface)', zIndex: 10 }}>
              <tr style={{ borderBottom: '1px solid var(--color-border)' }}>
                <th className="p-4 font-semibold text-muted text-sm">MÃ ĐƠN</th>
                <th className="p-4 font-semibold text-muted text-sm">KHÁCH HÀNG</th>
                <th className="p-4 font-semibold text-muted text-sm">SÂN / GIỜ</th>
                <th className="p-4 font-semibold text-muted text-sm">TRẠNG THÁI</th>
                <th className="p-4 font-semibold text-muted text-sm">TÀI CHÍNH</th>
                <th className="p-4 font-semibold text-muted text-sm whitespace-nowrap">THAO TÁC</th>
              </tr>
            </thead>
            <tbody>
              {loading && bookings.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-muted" style={{ verticalAlign: 'middle' }}>
                    <div style={{ minHeight: '220px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '0.75rem' }}>
                      <Loader2 size={24} className="animate-spin text-primary" />
                      <span>Đang tải danh sách đơn đặt sân...</span>
                    </div>
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-danger" style={{ verticalAlign: 'middle' }}>
                    <p className="font-semibold mb-2">{error}</p>
                    <button className="btn btn-secondary text-xs" onClick={loadBookings}>
                      Thử lại
                    </button>
                  </td>
                </tr>
              ) : bookings.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-8 text-center text-muted" style={{ verticalAlign: 'middle' }}>
                    <div style={{ minHeight: '180px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}>
                      <div className="text-base font-medium">Không tìm thấy đơn đặt sân nào phù hợp</div>
                      {searchTerm && (
                        <div className="text-xs text-muted">Thử tìm kiếm với từ khóa khác hoặc điều chỉnh lại bộ lọc</div>
                      )}
                    </div>
                  </td>
                </tr>
              ) : (
                bookings.map((booking) => {
                  const bookingCodeDisplay = booking.bookingCode?.startsWith('#')
                    ? booking.bookingCode
                    : `#${booking.bookingCode}`;

                  const price = booking.totalAmount || 0;
                  const deposit = booking.depositAmount || 0;
                  const remaining = booking.remainingAmount ?? Math.max(0, price - deposit);

                  return (
                    <tr key={booking.id} style={{ borderBottom: '1px solid var(--color-border)' }}>
                      <td className="p-4 font-semibold">{bookingCodeDisplay.toUpperCase()}</td>
                      <td className="p-4">
                        <div className="font-semibold">{booking.customerName}</div>
                        <div className="text-sm text-muted">{booking.customerPhone || '0987xxx'}</div>
                      </td>
                      <td className="p-4">
                        <div className="font-semibold">{booking.pitchName}</div>
                        <div className="text-sm text-muted">
                          {formatDate(booking.bookingDate)} | {booking.startTime?.slice(0, 5)} - {booking.endTime?.slice(0, 5)}
                        </div>
                      </td>
                      <td className="p-4">{getStatusBadge(booking.status)}</td>
                      <td className="p-4">
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                          <div className="text-sm">Tổng: {formatPrice(price)}</div>
                          {booking.additionalFee > 0 && (
                            <div className="text-sm text-muted">Gồm phụ phí: {formatPrice(booking.additionalFee)}</div>
                          )}
                          {booking.status === 'PENDING_HOLD' ? (
                            <>
                              <div className="text-sm text-warning">Cọc cần thu: {formatPrice(deposit)}</div>
                              <div className="text-sm text-muted">Chưa thanh toán cọc</div>
                            </>
                          ) : booking.status === 'WAITING_APPROVAL' ? (
                            <div className="text-sm text-warning">Cọc chờ xác nhận: {formatPrice(deposit)}</div>
                          ) : booking.status === 'REFUNDED' ? (
                            <>
                              <div className="text-sm text-muted">Cọc ban đầu: {formatPrice(deposit)}</div>
                              <div className="text-sm text-success">Đã hoàn tiền</div>
                            </>
                          ) : booking.status === 'CANCELLED' ? (
                            <>
                              <div className="text-sm text-muted">Mức cọc của đơn: {formatPrice(deposit)}</div>
                              <div className="text-sm text-muted">Đã hủy · Không thu thêm</div>
                            </>
                          ) : booking.status === 'COMPLETED' ? (
                            <>
                              <div className="text-sm text-success">Đã thanh toán: {formatPrice(Math.max(0, price - remaining))}</div>
                              {remaining > 0 ? (
                                <div className="text-sm font-semibold text-danger">Còn phải thu: {formatPrice(remaining)}</div>
                              ) : (
                                <div className="text-sm text-success">Đã thanh toán đủ</div>
                              )}
                            </>
                          ) : (
                            <>
                              <div className="text-sm text-success">
                                {deposit >= price ? 'Đã thanh toán' : 'Đã cọc'}: {formatPrice(deposit)}
                              </div>
                              {remaining > 0 ? (
                                <div className="text-sm font-semibold text-danger">Còn phải thu: {formatPrice(remaining)}</div>
                              ) : (
                                <div className="text-sm text-success">Đã thanh toán đủ</div>
                              )}
                              {booking.status === 'WAITING_CANCELLATION' && (
                                <div className="text-sm text-warning">Chờ xử lý hủy / hoàn cọc</div>
                              )}
                            </>
                          )}
                        </div>
                      </td>
                      <td className="p-4 whitespace-nowrap" style={{ whiteSpace: 'nowrap' }}>
                        <div className="flex gap-2 items-center flex-nowrap whitespace-nowrap" style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', flexWrap: 'nowrap', whiteSpace: 'nowrap' }}>
                          {booking.status === 'WAITING_APPROVAL' && (
                            <>
                              <button
                                className="btn btn-primary whitespace-nowrap"
                                onClick={() => setApproveBooking(booking)}
                              >
                                Duyệt đơn
                              </button>
                              <button
                                className="btn btn-secondary text-danger whitespace-nowrap"
                                onClick={() => setAdminCancelBooking(booking)}
                                title="Admin tự hủy đơn cho khách"
                              >
                                Hủy đơn
                              </button>
                            </>
                          )}
                          {booking.status === 'CONFIRMED' && (
                            <>
                              {remaining > 0 && (
                                <button
                                  className="btn btn-primary whitespace-nowrap"
                                  style={{ backgroundColor: 'var(--color-secondary)' }}
                                  onClick={() => setCheckoutBooking(booking)}
                                >
                                  Thu tiền
                                </button>
                              )}
                              <button
                                className="btn btn-secondary text-danger whitespace-nowrap"
                                onClick={() => setAdminCancelBooking(booking)}
                                title="Admin tự hủy đơn cho khách"
                              >
                                Hủy đơn
                              </button>
                            </>
                          )}
                          {booking.status === 'IN_PROGRESS' && (
                            <button
                              className="btn btn-primary whitespace-nowrap"
                              style={{ backgroundColor: 'var(--color-secondary)' }}
                              onClick={() => setCheckoutBooking(booking)}
                            >
                              Thu tiền
                            </button>
                          )}
                          {booking.status === 'WAITING_CANCELLATION' && (
                            <button
                              className="btn btn-primary whitespace-nowrap"
                              style={{ backgroundColor: 'var(--color-danger)' }}
                              onClick={() => setCancelBooking(booking)}
                            >
                              Duyệt hủy
                            </button>
                          )}
                          {booking.status !== 'WAITING_APPROVAL' &&
                            booking.status !== 'CONFIRMED' &&
                            booking.status !== 'WAITING_CANCELLATION' &&
                            booking.status !== 'IN_PROGRESS' && (
                              <span className="text-muted text-sm font-semibold select-none">—</span>
                            )}
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Phân trang góc phải */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center',
            gap: '1.25rem',
            flexShrink: 0,
            borderTop: '1px solid var(--color-border)',
            padding: '0.75rem 1.25rem',
          }}
        >
          <div className="text-sm text-muted">
            {totalElements > 0 ? (
              <>
                Hiển thị{' '}
                <span className="font-semibold text-[var(--color-text-base)]">
                  {(page - 1) * pageSize + 1}
                </span>{' '}
                -{' '}
                <span className="font-semibold text-[var(--color-text-base)]">
                  {Math.min(page * pageSize, totalElements)}
                </span>{' '}
                trong tổng số{' '}
                <span className="font-semibold text-[var(--color-text-base)]">{totalElements}</span> đơn đặt sân
              </>
            ) : (
              '0 đơn đặt sân'
            )}
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              className="btn btn-secondary"
              style={{ padding: '0.4rem 0.75rem', height: '36px' }}
              disabled={page <= 1 || loading}
              onClick={() => setPage((prev) => Math.max(1, prev - 1))}
            >
              <ChevronLeft size={16} /> Trước
            </button>

            <span className="text-sm font-semibold px-2" style={{ color: 'var(--color-text-base)' }}>
              {page} / {totalPages || 1}
            </span>

            <button
              type="button"
              className="btn btn-secondary"
              style={{ padding: '0.4rem 0.75rem', height: '36px' }}
              disabled={page >= totalPages || loading}
              onClick={() => setPage((prev) => Math.min(totalPages, prev + 1))}
            >
              Sau <ChevronRight size={16} />
            </button>
          </div>
        </div>
      </div>

      {/* 1. Modal Tạo đơn tại quầy */}
      <CounterBookingModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={loadBookings}
      />

      {/* 2. Modal Thu tiền & Đóng ca */}
      <CollectPaymentModal
        isOpen={Boolean(checkoutBooking)}
        booking={checkoutBooking}
        onClose={() => setCheckoutBooking(null)}
        onSuccess={loadBookings}
      />

      {/* 3. Modal Xét duyệt yêu cầu hủy đơn (từ khách hàng) */}
      <CancelRefundModal
        isOpen={Boolean(cancelBooking)}
        booking={cancelBooking}
        onClose={() => setCancelBooking(null)}
        onSuccess={loadBookings}
      />

      {/* 4. Modal Duyệt đơn (Approve PENDING booking) */}
      <ApproveBookingModal
        isOpen={Boolean(approveBooking)}
        booking={approveBooking}
        onClose={() => setApproveBooking(null)}
        onSuccess={loadBookings}
      />

      {/* 5. Modal Admin tự hủy đơn (Cancel PENDING booking) */}
      <AdminCancelModal
        isOpen={Boolean(adminCancelBooking)}
        booking={adminCancelBooking}
        onClose={() => setAdminCancelBooking(null)}
        onSuccess={loadBookings}
      />
    </div>
  );
};

export default AdminBookings;
