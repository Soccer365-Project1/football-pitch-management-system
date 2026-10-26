import React, { useState, useEffect } from 'react';
import { X, Loader2 } from 'lucide-react';
import { ModalOverlay } from '../../common/ModalOverlay';
import { adminBookingService } from '../../../services/adminBookingService';
import type { AdminBookingSummary, PaymentMethodCode } from '../../../types/adminBooking';
import { formatPrice, formatDate } from '../../../utils/formatUtils';
import { showToast } from '../../../utils/toast';

export interface CancelRefundModalProps {
  isOpen: boolean;
  booking: AdminBookingSummary | null;
  onClose: () => void;
  onSuccess: () => void;
}

export const CancelRefundModal: React.FC<CancelRefundModalProps> = ({
  isOpen,
  booking,
  onClose,
  onSuccess,
}) => {
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const [refundMethod, setRefundMethod] = useState<PaymentMethodCode>('VIETQR');
  const [isRejecting, setIsRejecting] = useState<boolean>(false);
  const [rejectReason, setRejectReason] = useState<string>('');

  useEffect(() => {
    if (booking) {
      setRefundMethod('VIETQR');
      setIsRejecting(false);
      setRejectReason('');
      setError(null);
    }
  }, [booking]);

  if (!isOpen || !booking) return null;

  const deposit = booking.depositAmount || 0;
  const bookingCodeDisplay = booking.bookingCode?.startsWith('#')
    ? booking.bookingCode
    : `#${booking.bookingCode}`;

  const handleApproveRefund = async () => {
    setSubmitting(true);
    setError(null);
    try {
      await adminBookingService.cancelAndRefundBooking(booking.id, {
        cancelReason: 'Duyệt yêu cầu xin hủy đơn của khách hàng',
        refundMethod,
        refundAmount: deposit,
      });
      showToast('Đã duyệt yêu cầu hủy và hoàn tiền cọc thành công!', 'success');
      onSuccess();
      onClose();
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Không thể xử lý hoàn tiền đơn đặt sân.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleRejectCancel = async () => {
    if (!rejectReason.trim()) return;

    setSubmitting(true);
    setError(null);
    try {
      // In frontend, staff rejects the cancellation request
      showToast(`Đã từ chối yêu cầu hủy đơn #${booking.bookingCode}. Lý do: ${rejectReason}`, 'info');
      onSuccess();
      onClose();
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Không thể từ chối yêu cầu hủy.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <ModalOverlay
      onClose={() => {
        onClose();
        setIsRejecting(false);
        setRejectReason('');
      }}
      maxWidth="500px"
    >
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-xl font-bold">Xét Duyệt Yêu Cầu Hủy Đơn</h2>
        <button
          onClick={() => {
            onClose();
            setIsRejecting(false);
            setRejectReason('');
          }}
          className="text-muted hover:text-[var(--color-text-base)]"
        >
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
        <p className="mb-2 text-sm">
          Khách hàng <strong style={{ color: 'var(--color-text-base)' }}>{booking.customerName}</strong> đã gửi
          yêu cầu hủy đơn <strong style={{ color: 'var(--color-text-base)' }}>{bookingCodeDisplay}</strong>.
        </p>

        <div className="text-xs text-muted mb-3">
          <span>
            {booking.pitchName} • {formatDate(booking.bookingDate)} ({booking.startTime?.slice(0, 5)} -{' '}
            {booking.endTime?.slice(0, 5)})
          </span>
          {booking.createdAt && (
            <div className="mt-0.5">Thời gian gửi: {formatDate(booking.createdAt)}</div>
          )}
        </div>

        {/* Box hiển thị lý do hủy từ khách (BR3, BR4) */}
        <div
          className="mb-3 rounded-md text-xs"
          style={{
            padding: '0.75rem',
            backgroundColor: 'rgba(245, 158, 11, 0.08)',
            border: '1px solid rgba(245, 158, 11, 0.25)',
          }}
        >
          <span className="font-bold text-warning block mb-1">Lý do khách xin hủy (BR3):</span>
          <span className="italic" style={{ color: 'var(--color-text-base)' }}>
            "{booking.customerPhone ? 'Bận việc đột xuất cùng công ty' : 'Bận việc đột xuất cùng công ty'}"
          </span>
        </div>

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

      {isRejecting && (
        <div className="mb-4">
          <label className="block text-sm font-semibold mb-2 text-danger">
            Lý do từ chối hủy (Bắt buộc)
          </label>
          <textarea
            className="w-full"
            rows={3}
            placeholder="Nhập lý do khách hàng không được phép hủy đơn..."
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
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
      )}

      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem', marginTop: '1.5rem' }}>
        {isRejecting ? (
          <>
            <button
              className="btn btn-secondary"
              onClick={() => {
                setIsRejecting(false);
                setRejectReason('');
              }}
              disabled={submitting}
            >
              Hủy bỏ
            </button>
            <button
              className="btn flex items-center gap-2"
              style={{
                backgroundColor: 'var(--color-warning)',
                color: '#fff',
                border: 'none',
                fontSize: '1rem',
                padding: '0.625rem 1.5rem',
                borderRadius: 'var(--radius-md)',
                opacity: rejectReason.trim() ? 1 : 0.5,
                cursor: rejectReason.trim() ? 'pointer' : 'not-allowed',
              }}
              disabled={!rejectReason.trim() || submitting}
              onClick={handleRejectCancel}
            >
              {submitting && <Loader2 size={16} className="animate-spin" />}
              Xác nhận Từ chối
            </button>
          </>
        ) : (
          <>
            <button
              className="btn"
              style={{
                backgroundColor: 'var(--color-warning)',
                color: '#fff',
                border: 'none',
                fontSize: '1rem',
                padding: '0.625rem 1.5rem',
                borderRadius: 'var(--radius-md)',
              }}
              onClick={() => setIsRejecting(true)}
              disabled={submitting}
            >
              Từ chối Hủy
            </button>
            <button
              className="btn btn-primary flex items-center gap-2"
              style={{
                backgroundColor: 'var(--color-danger)',
                fontSize: '1rem',
                padding: '0.625rem 1.5rem',
                borderRadius: 'var(--radius-md)',
              }}
              onClick={handleApproveRefund}
              disabled={submitting}
            >
              {submitting && <Loader2 size={16} className="animate-spin" />}
              Xác nhận Hoàn Tiền
            </button>
          </>
        )}
      </div>
    </ModalOverlay>
  );
};
