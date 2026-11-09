import React from 'react';
import { XCircle, Loader2 } from 'lucide-react';
import { ModalOverlay } from '../common/ModalOverlay.tsx';
import { formatDate } from '../../utils/formatUtils.ts';
import type { BookingResponse } from '../../services/bookingService.ts';

export interface BookingDetailModalProps {
  booking: BookingResponse | null;
  onClose: () => void;
  formatPrice: (price: number) => string;
  getStatusBadge: (status: string) => React.ReactNode;
  onRepay?: (bookingId: number) => void;
  repaying?: boolean;
  onPayRemaining?: (bookingId: number) => void;
  payingRemaining?: boolean;
}

export const BookingDetailModal: React.FC<BookingDetailModalProps> = ({
  booking,
  onClose,
  formatPrice,
  getStatusBadge,
  onRepay,
  repaying = false,
  onPayRemaining,
  payingRemaining = false,
}) => {
  if (!booking) return null;

  return (
    <ModalOverlay onClose={onClose} maxWidth="580px">
      <div className="booking-modal-content">
        <div className="booking-modal-header">
          <div style={{ flex: 1, paddingRight: '2rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem', flexWrap: 'wrap' }}>
              <h3 className="booking-modal-title">Chi tiết đơn đặt sân</h3>
              {getStatusBadge(booking.status)}
            </div>
            <p className="booking-modal-code">
              Mã đơn: #{booking.bookingCode ? booking.bookingCode.replace('#', '') : booking.id}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="booking-modal-close"
            title="Đóng"
            aria-label="Đóng chi tiết đơn"
          >
            <XCircle size={20} />
          </button>
        </div>

        <div className="booking-modal-body">
          <div className="booking-modal-section-card">
            <div className="booking-modal-row">
              <span className="booking-modal-label">Sân bóng:</span>
              <span className="booking-modal-val" title={booking.pitchNameSnapshot}>
                {booking.pitchNameSnapshot}
              </span>
            </div>
            <div className="booking-modal-row">
              <span className="booking-modal-label">Ngày đặt đá:</span>
              <span className="booking-modal-val">{formatDate(booking.bookingDate)}</span>
            </div>
            <div className="booking-modal-row">
              <span className="booking-modal-label">Khung giờ:</span>
              <span className="booking-modal-val" style={{ color: 'var(--color-primary)' }}>
                {booking.startTimeSnapshot?.substring(0, 5)} - {booking.endTimeSnapshot?.substring(0, 5)}
              </span>
            </div>
            <div className="booking-modal-row">
              <span className="booking-modal-label">Hình thức đặt:</span>
              <span className="booking-modal-val">
                {booking.bookingType === 'ONLINE' ? 'Đặt trực tuyến qua website' : 'Đặt tại quầy'}
              </span>
            </div>
          </div>

          <div className="booking-modal-prices">
            <div className="booking-modal-row">
              <span className="booking-modal-label">Tổng tiền sân:</span>
              <span className="booking-modal-val">
                {formatPrice(booking.totalPitchAmount || 0)}
              </span>
            </div>
            <div className="booking-modal-row" style={{ color: 'var(--color-primary)' }}>
              <span style={{ fontWeight: 500 }}>Tiền cọc (30%):</span>
              <span style={{ fontWeight: 700 }}>{formatPrice(booking.depositAmount || 0)}</span>
            </div>
            {Number(booking.additionalFee || 0) > 0 && (
              <div className="booking-modal-row">
                <span className="booking-modal-label">Phụ phí phát sinh:</span>
                <span className="booking-modal-val">{formatPrice(booking.additionalFee)}</span>
              </div>
            )}
            <div className="booking-modal-total-row">
              <span>Còn lại thanh toán tại sân:</span>
              <span style={{ color: 'var(--color-primary)' }}>
                {formatPrice(booking.remainingAmount || 0)}
              </span>
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', fontSize: '0.825rem', color: 'var(--color-text-muted)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '0.75rem' }}>
              <span style={{ flexShrink: 0, whiteSpace: 'nowrap' }}>Khách hàng:</span>
              <span
                style={{
                  fontWeight: 600,
                  color: 'var(--color-text-base)',
                  maxWidth: '70%',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  whiteSpace: 'nowrap',
                  textAlign: 'right'
                }}
                title={`${booking.guestName} (${booking.guestPhone})`}
              >
                {booking.guestName} ({booking.guestPhone})
              </span>
            </div>
            {booking.customerNote && (
              <div style={{ marginTop: '0.5rem' }}>
                <span style={{ display: 'block', marginBottom: '0.25rem' }}>Ghi chú:</span>
                <p style={{
                  padding: '0.75rem',
                  borderRadius: '0.5rem',
                  backgroundColor: 'var(--color-bg-base)',
                  border: '1px solid var(--color-border)',
                  fontStyle: 'italic',
                  color: 'var(--color-text-base)',
                  wordBreak: 'break-word',
                  overflowWrap: 'anywhere'
                }}>
                  "{booking.customerNote}"
                </p>
              </div>
            )}
          </div>
        </div>

        <div className="booking-modal-footer">
          {booking.status === 'PENDING_HOLD' && onRepay && (
            <button
              type="button"
              disabled={repaying}
              onClick={() => onRepay(booking.id)}
              className="booking-btn-modal-repay"
            >
              {repaying ? (
                <Loader2 size={16} className="animate-spin inline mr-1" />
              ) : null}
              Đặt cọc ngay
            </button>
          )}
          {booking.status === 'IN_PROGRESS' && onPayRemaining && (
            <button
              type="button"
              disabled={payingRemaining}
              onClick={() => onPayRemaining(booking.id)}
              className="booking-btn-modal-repay"
              style={{ backgroundColor: '#2563eb', borderColor: '#2563eb' }}
            >
              {payingRemaining ? (
                <Loader2 size={16} className="animate-spin inline mr-1" />
              ) : null}
              Thanh toán nốt qua VNPAY
            </button>
          )}
          <button
            type="button"
            onClick={onClose}
            className="booking-btn-close-modal"
          >
            Đóng
          </button>
        </div>
      </div>
    </ModalOverlay>
  );
};

export default BookingDetailModal;
