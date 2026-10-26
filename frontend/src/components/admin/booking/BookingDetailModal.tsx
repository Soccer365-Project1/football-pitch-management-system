import React, { useState, useEffect } from 'react';
import { X, Loader2, User, Calendar, DollarSign, Clock, FileText, AlertCircle } from 'lucide-react';
import { ModalOverlay } from '../../common/ModalOverlay';
import { adminBookingService } from '../../../services/adminBookingService';
import type { AdminBookingDetail } from '../../../types/adminBooking';
import { formatPrice, formatDate } from '../../../utils/formatUtils';

export interface BookingDetailModalProps {
  isOpen: boolean;
  bookingId: number | null;
  onClose: () => void;
  onOpenApprove?: (id: number) => void;
  onOpenCollect?: (id: number) => void;
  onOpenCancel?: (id: number) => void;
}

export const BookingDetailModal: React.FC<BookingDetailModalProps> = ({
  isOpen,
  bookingId,
  onClose,
  onOpenApprove,
  onOpenCollect,
  onOpenCancel,
}) => {
  const [loading, setLoading] = useState<boolean>(false);
  const [detail, setDetail] = useState<AdminBookingDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen || !bookingId) {
      setDetail(null);
      setError(null);
      return;
    }

    let isMounted = true;
    setLoading(true);
    setError(null);

    adminBookingService
      .getBookingDetail(bookingId)
      .then((data) => {
        if (isMounted) setDetail(data);
      })
      .catch((err) => {
        if (isMounted) {
          setError(err?.response?.data?.message || 'Không thể tải chi tiết đơn đặt sân');
        }
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [isOpen, bookingId]);

  if (!isOpen) return null;

  const getStatusBadge = (status?: string) => {
    switch (status) {
      case 'WAITING_APPROVAL':
        return <span className="badge badge-warning">Chờ duyệt (Cọc online)</span>;
      case 'CONFIRMED':
        return <span className="badge badge-success">Đã xác nhận</span>;
      case 'IN_PROGRESS':
        return <span className="badge badge-info" style={{ backgroundColor: 'rgba(59, 130, 246, 0.15)', color: '#2563eb' }}>Đang thi đấu</span>;
      case 'COMPLETED':
        return <span className="badge badge-secondary" style={{ backgroundColor: 'rgba(100, 116, 139, 0.15)', color: '#475569' }}>Đã hoàn thành</span>;
      case 'WAITING_CANCELLATION':
        return <span className="badge badge-danger" style={{ backgroundColor: 'rgba(239, 68, 68, 0.15)', color: '#dc2626' }}>Yêu cầu hủy cọc</span>;
      case 'CANCELLED':
        return <span className="badge badge-danger" style={{ opacity: 0.75 }}>Đã hủy</span>;
      case 'REFUNDED':
        return <span className="badge badge-secondary">Đã hoàn tiền</span>;
      case 'PENDING_HOLD':
        return <span className="badge badge-secondary">Tạm giữ chỗ</span>;
      default:
        return <span className="badge">{status}</span>;
    }
  };

  return (
    <ModalOverlay onClose={onClose} maxWidth="680px">
      <div className="flex justify-between items-center mb-4 pb-3" style={{ borderBottom: '1px solid var(--color-border)' }}>
        <div className="flex items-center gap-3">
          <h2 className="text-xl font-bold">Chi Tiết Đơn Đặt Sân</h2>
          {detail && <span className="text-sm font-semibold text-muted">#{detail.bookingCode}</span>}
          {detail && getStatusBadge(detail.status)}
        </div>
        <button
          onClick={onClose}
          className="text-muted hover:text-[var(--color-text-base)] transition-colors p-1 rounded-md"
          title="Đóng"
        >
          <X size={22} />
        </button>
      </div>

      {loading ? (
        <div className="flex flex-col items-center justify-center py-12 text-muted">
          <Loader2 className="animate-spin mb-3" size={36} />
          <p className="text-sm">Đang tải dữ liệu đơn đặt sân...</p>
        </div>
      ) : error ? (
        <div className="text-center py-8 text-danger">
          <AlertCircle className="mx-auto mb-2" size={36} />
          <p className="font-semibold">{error}</p>
          <button className="btn btn-secondary mt-4" onClick={onClose}>
            Đóng
          </button>
        </div>
      ) : detail ? (
        <div className="space-y-4" style={{ overflowY: 'auto', maxHeight: 'calc(80vh - 120px)', paddingRight: '0.25rem' }}>
          {/* Khối 1: Thông tin khách hàng & Nhân viên */}
          <div className="p-3.5 rounded-lg" style={{ backgroundColor: 'var(--color-bg-base)', border: '1px solid var(--color-border)' }}>
            <div className="flex items-center gap-2 mb-2 font-semibold text-sm text-primary">
              <User size={16} />
              <span>Thông Tin Khách Hàng & Nhân Viên</span>
            </div>
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div>
                <span className="text-muted block text-xs">Họ tên khách:</span>
                <span className="font-medium text-base">{detail.customerName}</span>
                {detail.customerId ? (
                  <span className="ml-2 inline-block px-1.5 py-0.5 text-xs rounded bg-blue-100 text-blue-800 font-semibold">Thành viên</span>
                ) : (
                  <span className="ml-2 inline-block px-1.5 py-0.5 text-xs rounded bg-gray-100 text-gray-700 font-medium">Khách vãng lai</span>
                )}
              </div>
              <div>
                <span className="text-muted block text-xs">Số điện thoại:</span>
                <span className="font-medium">{detail.customerPhone}</span>
              </div>
              {detail.customerEmail && (
                <div>
                  <span className="text-muted block text-xs">Email:</span>
                  <span className="font-medium">{detail.customerEmail}</span>
                </div>
              )}
              <div>
                <span className="text-muted block text-xs">Nhân viên phụ trách:</span>
                <span className="font-medium">{detail.staffName || 'Hệ thống tự động'}</span>
              </div>
            </div>
          </div>

          {/* Khối 2: Thông tin sân bóng & Giờ thi đấu */}
          <div className="p-3.5 rounded-lg" style={{ backgroundColor: 'var(--color-bg-base)', border: '1px solid var(--color-border)' }}>
            <div className="flex items-center gap-2 mb-2 font-semibold text-sm text-primary">
              <Calendar size={16} />
              <span>Sân Bóng & Khung Giờ Thi Đấu</span>
            </div>
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div>
                <span className="text-muted block text-xs">Sân bóng:</span>
                <span className="font-medium text-base">{detail.pitchName}</span>
                {detail.pitchTypeName && <span className="text-xs text-muted block">({detail.pitchTypeName})</span>}
              </div>
              <div>
                <span className="text-muted block text-xs">Ngày thi đấu:</span>
                <span className="font-medium">{formatDate(detail.bookingDate)}</span>
              </div>
              <div>
                <span className="text-muted block text-xs">Khung giờ:</span>
                <span className="font-medium text-primary flex items-center gap-1">
                  <Clock size={14} />
                  {detail.startTime} - {detail.endTime}
                </span>
              </div>
              <div>
                <span className="text-muted block text-xs">Hình thức đặt:</span>
                <span className="font-medium">{detail.bookingType === 'AT_COUNTER' ? 'Đặt trực tiếp tại quầy' : 'Đặt trực tuyến (Online)'}</span>
              </div>
            </div>
          </div>

          {/* Khối 3: Quyết toán Tài chính */}
          <div className="p-3.5 rounded-lg" style={{ backgroundColor: 'var(--color-bg-base)', border: '1px solid var(--color-border)' }}>
            <div className="flex items-center gap-2 mb-2 font-semibold text-sm text-primary">
              <DollarSign size={16} />
              <span>Cơ Cấu Tài Chính & Thanh Toán</span>
            </div>
            <div className="space-y-1.5 text-sm">
              <div className="flex justify-between">
                <span className="text-muted">Tổng tiền sân bóng:</span>
                <span className="font-semibold">{formatPrice(detail.totalPitchAmount || 0)}</span>
              </div>
              <div className="flex justify-between text-success">
                <span>Tiền đã thanh toán (Cọc 30%):</span>
                <span>- {formatPrice(detail.depositAmount || 0)}</span>
              </div>
              {detail.additionalFee > 0 && (
                <div className="flex justify-between text-warning">
                  <span>Phụ phí phát sinh (Nước/Áo/Bóng):</span>
                  <span>+ {formatPrice(detail.additionalFee)}</span>
                </div>
              )}
              <div style={{ height: '1px', backgroundColor: 'var(--color-border)', margin: '0.5rem 0' }} />
              <div className="flex justify-between text-base font-bold">
                <span className={detail.remainingAmount > 0 ? 'text-danger' : 'text-success'}>
                  {detail.remainingAmount > 0 ? 'Số tiền còn nợ / phải thu:' : 'Đã thanh toán đủ:'}
                </span>
                <span className={detail.remainingAmount > 0 ? 'text-danger' : 'text-success'}>
                  {formatPrice(detail.remainingAmount || 0)}
                </span>
              </div>
            </div>
          </div>

          {/* Khối 4: Ghi chú */}
          {detail.customerNote && (
            <div className="p-3 rounded-lg text-xs" style={{ backgroundColor: 'rgba(245, 158, 11, 0.08)', border: '1px solid rgba(245, 158, 11, 0.25)' }}>
              <div className="font-semibold text-warning mb-1 flex items-center gap-1">
                <FileText size={14} /> Ghi chú / Lý do hủy:
              </div>
              <p className="italic text-base-content">{detail.customerNote}</p>
            </div>
          )}

          {/* Khối 5: Lịch sử Giao dịch thanh toán con */}
          {detail.transactions && detail.transactions.length > 0 && (
            <div>
              <h4 className="font-semibold text-sm mb-2">Lịch Sử Giao Dịch Thanh Toán ({detail.transactions.length})</h4>
              <div style={{ border: '1px solid var(--color-border)', borderRadius: 'var(--radius-md)', overflow: 'hidden' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.8125rem' }}>
                  <thead style={{ backgroundColor: 'var(--color-bg-base)' }}>
                    <tr style={{ borderBottom: '1px solid var(--color-border)' }}>
                      <th className="p-2 text-left text-muted font-medium">MÃ GD</th>
                      <th className="p-2 text-left text-muted font-medium">LOẠI GD</th>
                      <th className="p-2 text-left text-muted font-medium">PTTT</th>
                      <th className="p-2 text-right text-muted font-medium">SỐ TIỀN</th>
                      <th className="p-2 text-center text-muted font-medium">TRẠNG THÁI</th>
                    </tr>
                  </thead>
                  <tbody>
                    {detail.transactions.map((t) => (
                      <tr key={t.id} style={{ borderBottom: '1px solid var(--color-border)' }}>
                        <td className="p-2 font-mono text-xs">{t.transactionCode || `#${t.id}`}</td>
                        <td className="p-2">
                          {t.paymentType === 'DEPOSIT_30' ? 'Cọc 30%' : t.paymentType === 'REMAINING_70' ? 'Thu nốt 70%' : t.paymentType === 'FULL_PAYMENT' ? 'Toàn phần 100%' : 'Hoàn cọc'}
                        </td>
                        <td className="p-2">{t.paymentMethodName || t.paymentMethodCode}</td>
                        <td className="p-2 text-right font-semibold">{formatPrice(t.amount)}</td>
                        <td className="p-2 text-center">
                          {t.status === 'SUCCESS' ? (
                            <span className="text-xs text-success font-medium">Thành công</span>
                          ) : (
                            <span className="text-xs text-danger font-medium">{t.status}</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Khối Thao tác nhanh ngữ cảnh */}
          <div className="flex justify-end gap-2 pt-3" style={{ borderTop: '1px solid var(--color-border)' }}>
            {detail.status === 'WAITING_APPROVAL' && onOpenApprove && (
              <button
                className="btn btn-primary"
                onClick={() => {
                  onClose();
                  onOpenApprove(detail.id);
                }}
              >
                Duyệt đơn này
              </button>
            )}

            {(detail.status === 'IN_PROGRESS' || detail.status === 'CONFIRMED') && detail.remainingAmount > 0 && onOpenCollect && (
              <button
                className="btn btn-primary"
                style={{ backgroundColor: 'var(--color-secondary)' }}
                onClick={() => {
                  onClose();
                  onOpenCollect(detail.id);
                }}
              >
                Thu tiền & Đóng ca
              </button>
            )}

            {(detail.status === 'WAITING_APPROVAL' || detail.status === 'CONFIRMED' || detail.status === 'WAITING_CANCELLATION') && onOpenCancel && (
              <button
                className="btn btn-secondary text-danger"
                onClick={() => {
                  onClose();
                  onOpenCancel(detail.id);
                }}
              >
                {detail.status === 'WAITING_CANCELLATION' ? 'Xử lý yêu cầu hủy' : 'Hủy & Hoàn cọc'}
              </button>
            )}

            <button className="btn btn-secondary" onClick={onClose}>
              Đóng
            </button>
          </div>
        </div>
      ) : null}
    </ModalOverlay>
  );
};
