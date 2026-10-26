import React, { useState, useEffect } from 'react';
import { X, Loader2 } from 'lucide-react';
import { ModalOverlay } from '../../common/ModalOverlay';
import { adminBookingService } from '../../../services/adminBookingService';
import type { AdminBookingSummary, PaymentMethodCode } from '../../../types/adminBooking';
import { formatPrice } from '../../../utils/formatUtils';
import { showToast } from '../../../utils/toast';

export interface AdminCancelModalProps {
  isOpen: boolean;
  booking: AdminBookingSummary | null;
  onClose: () => void;
  onSuccess: () => void;
}

export const AdminCancelModal: React.FC<AdminCancelModalProps> = ({
  isOpen,
  booking,
  onClose,
  onSuccess,
}) => {
  const [adminCancelReason, setAdminCancelReason] = useState<string>('');
  const [refundMethod, setRefundMethod] = useState<PaymentMethodCode>('VIETQR');
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (booking) {
      setAdminCancelReason('');
      setRefundMethod('VIETQR');
      setError(null);
    }
  }, [booking]);

  if (!isOpen || !booking) return null;

  const deposit = booking.depositAmount || 0;

  const handleCancel = async () => {
    if (!adminCancelReason.trim()) return;

    setSubmitting(true);
    setError(null);
    try {
      await adminBookingService.cancelAndRefundBooking(booking.id, {
        cancelReason: adminCancelReason.trim(),
        refundMethod,
        refundAmount: deposit,
      });
      showToast('Hủy đơn và hoàn tiền cọc thành công!', 'success');
      onSuccess();
      onClose();
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Không thể hủy đơn đặt sân.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <ModalOverlay onClose={onClose} maxWidth="500px">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-xl font-bold text-danger">Hủy Đơn & Hoàn Tiền</h2>
        <button onClick={onClose} className="text-muted hover:text-[var(--color-text-base)]">
          <X size={24} />
        </button>
      </div>

      {error && (
        <div className="p-3 mb-4 text-xs rounded-md bg-red-50 text-danger border border-red-200">
          {error}
        </div>
      )}

      <div
        style={{
          background: 'var(--color-bg-base)',
          padding: '1.25rem',
          borderRadius: 'var(--radius-md)',
          marginBottom: '1.25rem',
        }}
      >
        <p className="mb-4 text-base">
          Hủy đơn đặt sân của khách hàng <strong>{booking.customerName}</strong>.
        </p>

        <div
          className="flex justify-between items-center mb-4"
          style={{
            padding: '0.75rem',
            border: '1px dashed var(--color-danger)',
            borderRadius: 'var(--radius-md)',
            backgroundColor: 'rgba(239, 68, 68, 0.05)',
          }}
        >
          <span className="font-semibold text-sm" style={{ color: 'var(--color-text-base)' }}>
            Số tiền cọc cần hoàn trả:
          </span>
          <span className="text-xl font-bold" style={{ color: 'var(--color-text-base)' }}>
            {formatPrice(deposit)}
          </span>
        </div>

        <div>
          <label className="block text-sm font-semibold mb-2">Phương thức hoàn tiền</label>
          <select
            className="w-full"
            style={{
              padding: '0.6rem 1rem',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-md)',
              background: 'var(--color-bg-surface)',
            }}
            value={refundMethod}
            onChange={(e) => setRefundMethod(e.target.value as PaymentMethodCode)}
          >
            <option value="VIETQR">Chuyển khoản</option>
            <option value="CASH">Tiền mặt</option>
          </select>
        </div>
      </div>

      <div className="mb-4">
        <label className="block text-sm font-semibold mb-2 text-danger">
          Lý do hủy đơn (Bắt buộc)
        </label>
        <textarea
          className="w-full"
          rows={3}
          placeholder="Nhập lý do (VD: Khách gọi điện báo hủy, sân gặp sự cố...)"
          value={adminCancelReason}
          onChange={(e) => setAdminCancelReason(e.target.value)}
          style={{
            padding: '0.75rem',
            border: '1px solid var(--color-danger)',
            borderRadius: 'var(--radius-md)',
            background: 'var(--color-bg-base)',
            outline: 'none',
            resize: 'none',
          }}
        />
      </div>

      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem' }}>
        <button className="btn btn-secondary" onClick={onClose} disabled={submitting}>
          Quay lại
        </button>
        <button
          className="btn btn-primary flex items-center gap-2"
          style={{
            backgroundColor: 'var(--color-danger)',
            opacity: adminCancelReason.trim() ? 1 : 0.5,
            cursor: adminCancelReason.trim() ? 'pointer' : 'not-allowed',
          }}
          disabled={!adminCancelReason.trim() || submitting}
          onClick={handleCancel}
        >
          {submitting && <Loader2 size={16} className="animate-spin" />}
          Xác nhận Hủy Đơn
        </button>
      </div>
    </ModalOverlay>
  );
};
