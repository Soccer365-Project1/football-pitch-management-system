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

  const [refundMethod, setRefundMethod] = useState<PaymentMethodCode>('BANK_TRANSFER');
  const [refundReason, setRefundReason] = useState<string>('');
  const [isRejecting, setIsRejecting] = useState<boolean>(false);
  const [rejectReason, setRejectReason] = useState<string>('');

  useEffect(() => {
    if (booking) {
      setRefundMethod('BANK_TRANSFER');
      setRefundReason('');
      setIsRejecting(false);
      setRejectReason('');
      setError(null);
    }
  }, [booking]);

  const handleClose = () => {
    setIsRejecting(false);
    setRejectReason('');
    setRefundReason('');
    setError(null);
    onClose();
  };

  if (!isOpen || !booking) return null;

  const deposit = booking.depositAmount || 0;
  const bookingCodeDisplay = booking.bookingCode?.startsWith('#')
    ? booking.bookingCode
    : `#${booking.bookingCode}`;

  const handleApproveRefund = async () => {
    const trimmedReason = refundReason.trim();
    if (trimmedReason.length < 5) {
      setError('Vui lòng nhập ghi chú hoàn tiền tối thiểu từ 5 ký tự.');
      return;
    }
    if (trimmedReason.length > 500) {
      setError('Ghi chú hoàn tiền không được vượt quá 500 ký tự.');
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      await adminBookingService.cancelAndRefundBooking(booking.id, {
        cancelReason: trimmedReason,
        refundMethod,
        refundAmount: deposit,
      });
      showToast('Đã xác nhận hủy đơn và hoàn tiền thành công!', 'success');
      onSuccess();
      handleClose();
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Không thể xử lý hoàn tiền đơn đặt sân.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleRejectCancel = async () => {
    const trimmedReason = rejectReason.trim();
    if (trimmedReason.length < 5) {
      setError('Vui lòng nhập lý do từ chối yêu cầu hủy tối thiểu từ 5 ký tự.');
      return;
    }
    if (trimmedReason.length > 500) {
      setError('Lý do từ chối yêu cầu hủy không được vượt quá 500 ký tự.');
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      await adminBookingService.rejectCancellation(booking.id, {
        rejectionReason: trimmedReason,
      });
      showToast('Đã từ chối yêu cầu hủy đơn. Đơn đặt sân được giữ nguyên!', 'success');
      onSuccess();
      handleClose();
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Không thể từ chối yêu cầu hủy.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <ModalOverlay
      onClose={handleClose}
      maxWidth="540px"
    >
      {/* 1. Header */}
      <div className="flex justify-between items-center" style={{ flexShrink: 0, gap: '1rem', marginBottom: '1.25rem' }}>
        <h2 className="text-xl font-bold">Xét Duyệt Yêu Cầu Hủy Đơn</h2>
        <button
          onClick={handleClose}
          className="text-muted hover:text-[var(--color-text-base)] cursor-pointer"
          style={{ padding: '0.25rem', flexShrink: 0 }}
          aria-label="Đóng"
        >
          <X size={22} />
        </button>
      </div>

      <div style={{ minHeight: 0, overflowY: 'auto', overflowWrap: 'anywhere', paddingRight: '0.25rem' }}>
        {error && (
          <div
            role="alert"
            className="text-danger"
            style={{
              padding: '0.875rem 1rem',
              marginBottom: '1rem',
              border: '1px solid #fecaca',
              borderRadius: 'var(--radius-md)',
              backgroundColor: '#fef2f2',
              fontSize: '0.875rem',
              lineHeight: 1.6,
            }}
          >
            {error}
          </div>
        )}

        {/* 2. Khung thông tin chi tiết đơn hàng */}
        <div
          style={{
            background: 'var(--color-bg-base)',
            padding: '1.5rem',
            borderRadius: 'var(--radius-md)',
            marginBottom: '1.25rem',
          }}
        >
          <p className="mb-2 text-sm leading-relaxed">
            Khách hàng <strong style={{ color: 'var(--color-text-base)' }}>{booking.customerName}</strong> đã gửi
            yêu cầu hủy đơn <strong style={{ color: 'var(--color-text-base)' }}>{bookingCodeDisplay}</strong>.
          </p>

          <div className="text-xs text-muted mb-4 leading-relaxed">
            <div>
              {booking.pitchName} • {formatDate(booking.bookingDate)} ({booking.startTime?.slice(0, 5)} -{' '}
              {booking.endTime?.slice(0, 5)})
            </div>
            {booking.createdAt && (
              <div className="mt-1">Thời gian gửi: {formatDate(booking.createdAt)}</div>
            )}
          </div>

          {/* Box hiển thị lý do hủy từ khách (BR3, BR4) */}
          <div
            className="mb-4 rounded-md text-xs"
            style={{
              padding: '0.9rem 1.25rem',
              backgroundColor: 'rgba(245, 158, 11, 0.08)',
              border: '1px solid rgba(245, 158, 11, 0.25)',
              lineHeight: 1.6,
            }}
          >
            <span className="font-bold text-warning block mb-1">Lý do khách xin hủy (BR3):</span>
            <span className="italic" style={{ color: 'var(--color-text-base)' }}>
              "{booking.customerNote || 'Không có ghi chú từ khách hàng'}"
            </span>
          </div>

          {!isRejecting ? (
            <>
              {/* Box số tiền cọc cần hoàn trả - viền đứt nét đỏ chuẩn Image 1 */}
              <div
                className="flex justify-between items-center mb-4"
                style={{
                  padding: '0.9rem 1.25rem',
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

              {/* Phương thức hoàn tiền */}
              <div className="mb-4">
                <label className="block text-sm font-semibold mb-2">Phương thức hoàn tiền</label>
                <select
                  className="w-full"
                  style={{
                    padding: '0.65rem 0.85rem',
                    border: '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--color-bg-surface)',
                    fontSize: '0.875rem',
                  }}
                  value={refundMethod}
                  onChange={(e) => setRefundMethod(e.target.value as PaymentMethodCode)}
                >
                  <option value="BANK_TRANSFER">Chuyển khoản</option>
                  <option value="CASH">Tiền mặt</option>
                </select>
              </div>

              {/* Ghi chú hoàn tiền bắt buộc (SRS Bảng 3.16 UC011.3) */}
              <div>
                <div className="flex justify-between items-center mb-1.5">
                  <label className="block text-sm font-semibold" style={{ color: 'var(--color-text-base)' }}>
                    Ghi chú hoàn tiền <span style={{ color: 'var(--color-danger)' }}>*</span>
                  </label>
                  <span className="text-xs text-muted">
                    {refundReason.length}/500
                  </span>
                </div>
                <textarea
                  className="w-full"
                  rows={3}
                  maxLength={500}
                  placeholder="Nhập ghi chú hoàn tiền bắt buộc (VD: Hoàn cọc 100% qua STK Vietcombank 123456789 - GD123456...)"
                  value={refundReason}
                  onChange={(e) => {
                    setRefundReason(e.target.value);
                    if (error) setError(null);
                  }}
                  style={{
                    padding: '0.85rem 1rem',
                    border: error && refundReason.trim().length < 5 ? '1px solid var(--color-danger)' : '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--color-bg-surface)',
                    color: 'var(--color-text-base)',
                    outline: 'none',
                    resize: 'none',
                    fontSize: '0.875rem',
                    fontFamily: 'inherit',
                    lineHeight: 1.5,
                    boxSizing: 'border-box',
                  }}
                />
              </div>
            </>
          ) : (
            /* Box số tiền cọc đơn (Giữ cọc, không hoàn trả) - chuẩn Image 2 */
            <div
              className="flex justify-between items-center"
              style={{
                padding: '0.9rem 1.25rem',
                border: '1px solid #e5e7eb',
                borderRadius: 'var(--radius-md)',
                backgroundColor: '#f3f4f6',
              }}
            >
              <span className="text-sm font-medium" style={{ color: 'var(--color-text-base)' }}>
                Số tiền cọc đơn:
              </span>
              <span className="text-sm font-bold" style={{ color: 'var(--color-danger)' }}>
                {formatPrice(deposit)} (Giữ cọc, không hoàn trả)
              </span>
            </div>
          )}
        </div>

        {/* 3. Khung nhập lý do từ chối hủy (khi chuyển sang trạng thái từ chối) */}
        {isRejecting && (
          <div style={{ marginBottom: '1.25rem' }}>
            <div className="flex justify-between items-center mb-1.5">
              <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, color: 'var(--color-text-base)' }}>
                Lý do từ chối hủy <span style={{ color: 'var(--color-danger)' }}>*</span>
              </label>
              <span className="text-xs text-muted">
                {rejectReason.length}/500
              </span>
            </div>
            <textarea
              className="w-full"
              rows={3}
              maxLength={500}
              placeholder="Nhập lý do khách hàng không được phép hủy đơn (VD: Sân đã bố trí nhân sự, vi phạm quy định báo trước 24h...)"
              value={rejectReason}
              onChange={(e) => {
                setRejectReason(e.target.value);
                if (error) setError(null);
              }}
              style={{
                padding: '0.85rem 1rem',
                border: error && rejectReason.trim().length < 5 ? '1px solid var(--color-danger)' : '1px solid var(--color-border)',
                borderRadius: 'var(--radius-md)',
                background: '#ffffff',
                color: 'var(--color-text-base)',
                outline: 'none',
                resize: 'none',
                fontSize: '0.875rem',
                fontFamily: 'inherit',
                lineHeight: 1.5,
                boxSizing: 'border-box',
              }}
            />
          </div>
        )}
      </div>

      {/* 4. Button thao tác */}
      <div
        style={{
          display: 'flex',
          flexShrink: 0,
          flexWrap: 'wrap',
          justifyContent: 'flex-end',
          gap: '0.75rem',
          paddingTop: '1rem',
          borderTop: '1px solid var(--color-border)',
        }}
      >
        {isRejecting ? (
          <>
            <button
              type="button"
              className="btn"
              onClick={() => {
                setIsRejecting(false);
                setRejectReason('');
                setError(null);
              }}
              disabled={submitting}
              style={{
                height: '42px',
                padding: '0 1.65rem',
                fontSize: '0.9rem',
                fontWeight: 600,
                backgroundColor: '#ffffff',
                border: '1px solid var(--color-border)',
                color: 'var(--color-text-base)',
                borderRadius: 'var(--radius-md)',
                cursor: 'pointer',
              }}
            >
              Quay lại
            </button>
            <button
              type="button"
              className="btn flex items-center gap-2"
              style={{
                backgroundColor: '#b45309',
                color: '#fff',
                border: 'none',
                fontSize: '0.9rem',
                padding: '0 1.65rem',
                height: '42px',
                borderRadius: 'var(--radius-md)',
                cursor: submitting ? 'not-allowed' : 'pointer',
                fontWeight: 600,
              }}
              disabled={submitting}
              onClick={handleRejectCancel}
            >
              {submitting && <Loader2 size={16} className="animate-spin" />}
              Xác nhận Từ chối
            </button>
          </>
        ) : (
          <>
            <button
              type="button"
              className="btn"
              style={{
                backgroundColor: '#ffffff',
                color: '#b45309',
                border: '1px solid #b45309',
                fontSize: '0.9rem',
                padding: '0 1.65rem',
                height: '42px',
                borderRadius: 'var(--radius-md)',
                fontWeight: 600,
                cursor: submitting ? 'not-allowed' : 'pointer',
              }}
              onClick={() => {
                setIsRejecting(true);
                setError(null);
              }}
              disabled={submitting}
            >
              Từ chối Hủy
            </button>
            <button
              type="button"
              className="btn btn-primary flex items-center gap-2"
              style={{
                backgroundColor: 'var(--color-danger)',
                color: '#fff',
                fontSize: '0.9rem',
                padding: '0 1.65rem',
                height: '42px',
                borderRadius: 'var(--radius-md)',
                fontWeight: 600,
                border: 'none',
                cursor: submitting ? 'not-allowed' : 'pointer',
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
