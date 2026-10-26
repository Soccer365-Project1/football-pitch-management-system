import React, { useState, useEffect } from 'react';
import { X, Loader2 } from 'lucide-react';
import { ModalOverlay } from '../../common/ModalOverlay';
import { adminBookingService } from '../../../services/adminBookingService';
import type { AdminBookingSummary, PaymentMethodCode } from '../../../types/adminBooking';
import { formatPrice } from '../../../utils/formatUtils';
import { showToast } from '../../../utils/toast';

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

  const [paymentMethod, setPaymentMethod] = useState<PaymentMethodCode>('CASH');
  const [additionalFee, setAdditionalFee] = useState<number>(0);

  useEffect(() => {
    if (booking) {
      setPaymentMethod('CASH');
      setAdditionalFee(0);
      setError(null);
    }
  }, [booking]);

  if (!isOpen || !booking) return null;

  const price = booking.totalAmount || 0;
  const deposit = booking.depositAmount || 0;
  const remaining = Math.max(0, price - deposit) + Number(additionalFee || 0);

  const bookingCodeDisplay = booking.bookingCode?.startsWith('#')
    ? booking.bookingCode
    : `#${booking.bookingCode}`;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    setSubmitting(true);
    try {
      await adminBookingService.collectPayment(booking.id, {
        additionalFee: Number(additionalFee || 0),
        paymentMethod,
        paymentNote: additionalFee > 0 ? `Thu tiền đóng ca + phụ phí ${formatPrice(additionalFee)}` : undefined,
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
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-xl font-bold">Thanh Toán & Đóng Ca</h2>
        <button onClick={onClose} className="text-muted hover:text-[var(--color-text-base)]">
          <X size={24} />
        </button>
      </div>

      {error && (
        <div className="p-3 mb-4 text-xs rounded-md bg-red-50 text-danger border border-red-200">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit}>
        <div
          style={{
            background: 'var(--color-bg-base)',
            padding: '1rem',
            borderRadius: 'var(--radius-md)',
            marginBottom: '1.5rem',
          }}
        >
          <div className="flex justify-between mb-2">
            <span className="text-muted">Đơn hàng:</span>
            <span className="font-semibold">{bookingCodeDisplay}</span>
          </div>
          <div className="flex justify-between mb-2">
            <span className="text-muted">Khách hàng:</span>
            <span className="font-semibold">{booking.customerName}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted">Sân / Khung giờ:</span>
            <span className="font-semibold">
              {booking.pitchName} ({booking.startTime?.slice(0, 5)} - {booking.endTime?.slice(0, 5)})
            </span>
          </div>
        </div>

        <div className="mb-6">
          <div className="flex justify-between mb-2 text-lg">
            <span>Tổng tiền sân:</span>
            <span className="font-semibold">{formatPrice(price)}</span>
          </div>
          <div className="flex justify-between mb-4 text-success">
            <span>Đã thanh toán (Cọc):</span>
            <span>- {formatPrice(deposit)}</span>
          </div>

          {/* Phụ phí phát sinh nếu có */}
          <div className="flex justify-between items-center mb-3 text-sm">
            <span className="text-muted">Phụ phí phát sinh (nước uống, bóng...):</span>
            <input
              type="number"
              min={0}
              step={5000}
              value={additionalFee}
              onChange={(e) => setAdditionalFee(Math.max(0, Number(e.target.value)))}
              className="text-right font-semibold"
              style={{
                width: '130px',
                padding: '0.4rem 0.6rem',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-md)',
                background: 'var(--color-bg-surface)',
              }}
              placeholder="0 đ"
            />
          </div>

          <div style={{ height: 1, backgroundColor: 'var(--color-border)', margin: '1rem 0' }} />

          <div className="flex justify-between text-xl font-bold text-danger mb-4">
            <span>Khách cần thanh toán:</span>
            <span>{formatPrice(remaining)}</span>
          </div>

          <div>
            <label className="block text-sm font-semibold mb-2">Phương thức thanh toán</label>
            <select
              className="w-full"
              style={{
                padding: '0.6rem 1rem',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-md)',
                background: 'var(--color-bg-surface)',
              }}
              value={paymentMethod}
              onChange={(e) => setPaymentMethod(e.target.value as PaymentMethodCode)}
            >
              <option value="CASH">Tiền mặt</option>
              <option value="VIETQR">Chuyển khoản</option>
            </select>
          </div>
        </div>

        <button
          type="submit"
          className="btn btn-primary w-full flex items-center justify-center gap-2"
          style={{
            backgroundColor: 'var(--color-secondary)',
            fontSize: '1.1rem',
            padding: '0.75rem',
          }}
          disabled={submitting}
        >
          {submitting && <Loader2 size={18} className="animate-spin" />}
          Xác nhận Thu Tiền
        </button>
      </form>
    </ModalOverlay>
  );
};
