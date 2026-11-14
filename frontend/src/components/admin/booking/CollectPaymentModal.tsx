import React, { useState, useEffect } from 'react';
import { X, Banknote, CreditCard, Loader2 } from 'lucide-react';
import { ModalOverlay } from '../../common/ModalOverlay.tsx';
import { adminBookingService } from '../../../services/adminBookingService.ts';
import type { AdminBookingSummary, PaymentMethodCode } from '../../../types/adminBooking.ts';
import { formatPrice, formatDate } from '../../../utils/formatUtils.ts';
import { showToast } from '../../../utils/toast.ts';

export interface CollectPaymentModalProps {
  isOpen: boolean;
  booking: AdminBookingSummary | null;
  onClose: () => void;
  onSuccess: () => void;
}

export const CollectPaymentModal: React.FC<CollectPaymentModalProps> = ({
  isOpen,
  booking,
  onClose,
  onSuccess,
}) => {
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [paymentMethod, setPaymentMethod] = useState<'CASH' | 'BANK_TRANSFER'>('CASH');
  const [note, setNote] = useState<string>('');

  useEffect(() => {
    if (booking) {
      setPaymentMethod('CASH');
      setNote('');
      setError(null);
    }
  }, [booking]);

  if (!isOpen || !booking || booking.status !== 'IN_PROGRESS') return null;

  const totalAmount = Number(booking.totalAmount || 0);
  const depositAmount = Number(booking.depositAmount || 0);
  const remainingAmount = Number(booking.remainingAmount ?? Math.max(0, totalAmount - depositAmount));

  const bookingCodeDisplay = booking.bookingCode
    ? (booking.bookingCode.startsWith('#') ? booking.bookingCode : `#${booking.bookingCode}`)
    : `#${booking.id}`;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await adminBookingService.collectPayment(booking.id, {
        paymentMethod: paymentMethod as PaymentMethodCode,
        paymentNote: note.trim() || undefined,
      });
      showToast('Xác nhận thu tiền và đóng ca thành công!', 'success');
      onSuccess();
      onClose();
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Không thể thu tiền đóng ca.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <ModalOverlay onClose={onClose} maxWidth="500px">
      <div className="timeline-modal-container">
        {/* Header */}
        <div className="timeline-modal-header">
          <div className="timeline-modal-header-left">
            <div>
              <h3 className="timeline-modal-title">Trả Sân & Thanh Toán</h3>
              <p className="timeline-modal-subtitle">Thu nốt 70% tiền sân và kết thúc ca đá</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="timeline-modal-close"
            aria-label="Đóng"
          >
            <X size={20} />
          </button>
        </div>

        {/* Thông báo lỗi nếu có */}
        {error && (
          <div className="timeline-modal-notice-box red" style={{ margin: '0.75rem 1rem 0' }}>
            {error}
          </div>
        )}

        {/* Body */}
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', flex: '1 1 auto', minHeight: 0 }}>
          <div className="timeline-modal-body">
            {/* Card thông tin ca đá */}
            <div className="timeline-modal-card">
              <div className="timeline-modal-row">
                <span className="timeline-modal-label">Mã đơn:</span>
                <span className="timeline-modal-val">{bookingCodeDisplay}</span>
              </div>
              <div className="timeline-modal-row">
                <span className="timeline-modal-label">Sân bóng:</span>
                <span className="timeline-modal-val highlight">
                  {booking.pitchName} {booking.pitchTypeName ? `(${booking.pitchTypeName})` : ''}
                </span>
              </div>
              <div className="timeline-modal-row">
                <span className="timeline-modal-label">Khung giờ:</span>
                <span className="timeline-modal-val">
                  {booking.startTime?.substring(0, 5)} - {booking.endTime?.substring(0, 5)}
                </span>
              </div>
              {booking.bookingDate && (
                <div className="timeline-modal-row">
                  <span className="timeline-modal-label">Ngày đá:</span>
                  <span className="timeline-modal-val">{formatDate(booking.bookingDate)}</span>
                </div>
              )}
              {booking.customerName && (
                <div className="timeline-modal-row">
                  <span className="timeline-modal-label">Khách hàng:</span>
                  <span className="timeline-modal-val">
                    {booking.customerName} {booking.customerPhone ? `(${booking.customerPhone})` : ''}
                  </span>
                </div>
              )}
            </div>

            {/* Card chi tiết thanh toán */}
            <div className="timeline-modal-card" style={{ borderColor: 'rgba(245, 158, 11, 0.4)', backgroundColor: 'rgba(245, 158, 11, 0.04)' }}>
              {booking.totalAmount != null && (
                <div className="timeline-modal-row">
                  <span className="timeline-modal-label">Tổng tiền sân:</span>
                  <span className="timeline-modal-val">{formatPrice(totalAmount)}</span>
                </div>
              )}
              {booking.depositAmount != null && (
                <div className="timeline-modal-row">
                  <span className="timeline-modal-label">Đã cọc trước (30%):</span>
                  <span className="timeline-modal-val" style={{ color: '#10b981' }}>
                    - {formatPrice(depositAmount)}
                  </span>
                </div>
              )}
              <div className="timeline-modal-row highlight-remaining" style={{ borderTopColor: 'rgba(245, 158, 11, 0.4)' }}>
                <span className="timeline-modal-label" style={{ fontWeight: 700, color: 'var(--color-text-base)' }}>
                  Cần thu nốt (70%):
                </span>
                <span className="timeline-modal-val" style={{ fontSize: '1.2rem', fontWeight: 800, color: '#d97706' }}>
                  {formatPrice(remainingAmount)}
                </span>
              </div>
            </div>

            {/* Chọn hình thức thanh toán */}
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 700, color: 'var(--color-text-base)', marginBottom: '0.5rem' }}>
                Hình thức thanh toán nốt <span style={{ color: '#ef4444' }}>*</span>
              </label>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.65rem' }}>
                {/* Tiền mặt */}
                <label
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.65rem',
                    padding: '0.75rem 0.9rem',
                    borderRadius: 'var(--radius-md)',
                    border: paymentMethod === 'CASH' ? '2px solid #f59e0b' : '1px solid var(--color-border)',
                    backgroundColor: paymentMethod === 'CASH' ? 'rgba(245, 158, 11, 0.08)' : 'var(--color-bg-base)',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease',
                  }}
                >
                  <input
                    type="radio"
                    name="collectPaymentMethod"
                    value="CASH"
                    checked={paymentMethod === 'CASH'}
                    onChange={() => setPaymentMethod('CASH')}
                    style={{ accentColor: '#f59e0b', cursor: 'pointer' }}
                  />
                  <Banknote size={20} style={{ color: '#10b981', flexShrink: 0 }} />
                  <div>
                    <div style={{ fontWeight: 700, fontSize: '0.85rem', color: 'var(--color-text-base)' }}>Tiền mặt</div>
                    <div style={{ fontSize: '0.72rem', color: 'var(--color-text-muted)' }}>Thu tại quầy</div>
                  </div>
                </label>

                {/* Chuyển khoản */}
                <label
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.65rem',
                    padding: '0.75rem 0.9rem',
                    borderRadius: 'var(--radius-md)',
                    border: paymentMethod === 'BANK_TRANSFER' ? '2px solid #f59e0b' : '1px solid var(--color-border)',
                    backgroundColor: paymentMethod === 'BANK_TRANSFER' ? 'rgba(245, 158, 11, 0.08)' : 'var(--color-bg-base)',
                    cursor: 'pointer',
                    transition: 'all 0.15s ease',
                  }}
                >
                  <input
                    type="radio"
                    name="collectPaymentMethod"
                    value="BANK_TRANSFER"
                    checked={paymentMethod === 'BANK_TRANSFER'}
                    onChange={() => setPaymentMethod('BANK_TRANSFER')}
                    style={{ accentColor: '#f59e0b', cursor: 'pointer' }}
                  />
                  <CreditCard size={20} style={{ color: '#3b82f6', flexShrink: 0 }} />
                  <div>
                    <div style={{ fontWeight: 700, fontSize: '0.85rem', color: 'var(--color-text-base)' }}>Chuyển khoản</div>
                    <div style={{ fontSize: '0.72rem', color: 'var(--color-text-muted)' }}>Ngân hàng / QR</div>
                  </div>
                </label>
              </div>
            </div>

            {/* Ô ghi chú thu ngân */}
            <div>
              <label htmlFor="collect-note" style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: 'var(--color-text-muted)', marginBottom: '0.35rem' }}>
                Ghi chú thu ngân (tùy chọn):
              </label>
              <textarea
                id="collect-note"
                rows={2}
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder="Nhập ghi chú thu tiền nếu có (VD: Khách chuyển qua Techcombank, đã nhận đủ...)"
                style={{
                  width: '100%',
                  padding: '0.6rem 0.75rem',
                  fontSize: '0.85rem',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--color-border)',
                  backgroundColor: 'var(--color-bg-base)',
                  color: 'var(--color-text-base)',
                  outline: 'none',
                  resize: 'none',
                }}
              />
            </div>
          </div>

          {/* Footer */}
          <div className="timeline-modal-footer">
            <button
              type="button"
              disabled={submitting}
              onClick={onClose}
              className="timeline-btn-cancel-modal"
            >
              Đóng
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="timeline-btn-confirm-checkout"
            >
              {submitting ? (
                <>
                  <Loader2 size={16} className="animate-spin" />
                  <span>Đang xử lý...</span>
                </>
              ) : (
                <span>Xác nhận thu tiền & Trả sân</span>
              )}
            </button>
          </div>
        </form>
      </div>
    </ModalOverlay>
  );
};
