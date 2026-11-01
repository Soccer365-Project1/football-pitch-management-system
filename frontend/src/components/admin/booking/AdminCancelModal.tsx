import React, { useState, useEffect } from 'react';
import { X, Loader2, CheckCircle2, Circle } from 'lucide-react';
import { ModalOverlay } from '../../common/ModalOverlay';
import { adminBookingService } from '../../../services/adminBookingService';
import type { AdminBookingSummary, PaymentMethodCode } from '../../../types/adminBooking';
import { formatPrice, formatDate } from '../../../utils/formatUtils';
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
  const [isRefund, setIsRefund] = useState<boolean>(true);
  const [refundMethod, setRefundMethod] = useState<PaymentMethodCode>('VIETQR');
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (booking) {
      const deposit = booking.depositAmount || 0;
      setAdminCancelReason('');
      setIsRefund(deposit > 0);
      setRefundMethod('VIETQR');
      setError(null);
    }
  }, [booking]);

  if (!isOpen || !booking) return null;

  const deposit = booking.depositAmount || 0;
  const bookingCodeDisplay = booking.bookingCode?.startsWith('#')
    ? booking.bookingCode
    : `#${booking.bookingCode}`;

  const handleCancel = async () => {
    const trimmedReason = adminCancelReason.trim();
    if (trimmedReason.length < 5) {
      setError('Vui lòng nhập lý do hủy đơn tối thiểu từ 5 ký tự.');
      return;
    }
    if (trimmedReason.length > 500) {
      setError('Lý do hủy đơn không được vượt quá 500 ký tự.');
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      await adminBookingService.cancelBooking(booking.id, {
        cancelReason: trimmedReason,
        isRefund,
        refundMethod: isRefund ? refundMethod : undefined,
        refundAmount: isRefund ? deposit : 0,
      });
      showToast(isRefund ? 'Hủy đơn và hoàn tiền cọc thành công!' : 'Hủy đơn đặt sân thành công!', 'success');
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
    <ModalOverlay onClose={onClose} maxWidth="540px">
      {/* 1. Tiêu đề - không có icon theo yêu cầu */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.85rem' }}>
        <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0, color: 'var(--color-danger)' }}>
          Hủy Đơn Đặt Sân
        </h2>
        <button
          type="button"
          onClick={onClose}
          style={{
            color: 'var(--color-text-muted)',
            cursor: 'pointer',
            padding: '0.25rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
          aria-label="Đóng"
        >
          <X size={22} />
        </button>
      </div>

      {error && (
        <div style={{
          padding: '0.5rem 0.75rem',
          marginBottom: '0.75rem',
          fontSize: '0.825rem',
          borderRadius: 'var(--radius-md)',
          backgroundColor: '#fef2f2',
          color: 'var(--color-danger)',
          border: '1px solid #fecaca',
        }}>
          {error}
        </div>
      )}

      {/* 2. Bảng tóm tắt thông tin đơn hàng vừa vặn */}
      <div
        style={{
          backgroundColor: '#f9fafb',
          border: '1px solid #e5e7eb',
          padding: '0.75rem 1rem',
          borderRadius: 'var(--radius-md)',
          marginBottom: '0.85rem',
          fontSize: '0.85rem',
        }}
      >
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.35rem 1.25rem', marginBottom: '0.5rem' }}>
          <div>
            <span style={{ color: 'var(--color-text-muted)' }}>Mã đơn: </span>
            <span style={{ fontWeight: 700, color: 'var(--color-text-base)' }}>{bookingCodeDisplay}</span>
          </div>
          <div>
            <span style={{ color: 'var(--color-text-muted)' }}>Khách hàng: </span>
            <span style={{ fontWeight: 700, color: 'var(--color-text-base)' }}>
              {booking.customerName}
            </span>
          </div>
          <div>
            <span style={{ color: 'var(--color-text-muted)' }}>Sân bóng: </span>
            <span style={{ fontWeight: 600, color: 'var(--color-text-base)' }}>{booking.pitchName}</span>
          </div>
          <div>
            <span style={{ color: 'var(--color-text-muted)' }}>Ca đá: </span>
            <span style={{ fontWeight: 600, color: 'var(--color-text-base)' }}>
              {booking.startTime?.slice(0, 5)} - {booking.endTime?.slice(0, 5)} ({formatDate(booking.bookingDate)})
            </span>
          </div>
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '0.5rem', borderTop: '1px solid #e5e7eb' }}>
          <span style={{ color: 'var(--color-text-muted)', fontWeight: 500 }}>Số tiền đã cọc:</span>
          <span style={{ fontWeight: 700, fontSize: '1.1rem', color: 'var(--color-danger)' }}>
            {formatPrice(deposit)}
          </span>
        </div>
      </div>

      {/* 3. Chính sách xử lý tiền cọc: 2 Card rõ ràng */}
      <div style={{ marginBottom: '0.85rem' }}>
        <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.45rem', color: 'var(--color-text-base)' }}>
          Chính sách xử lý tiền cọc
        </label>
        
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.65rem', marginBottom: '0.75rem' }}>
          {/* Card 1: Có hoàn tiền cọc */}
          <div
            role="button"
            tabIndex={0}
            onClick={() => setIsRefund(true)}
            onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') setIsRefund(true); }}
            style={{
              boxSizing: 'border-box',
              display: 'flex',
              flexDirection: 'column',
              padding: '0.65rem 0.75rem',
              borderRadius: '8px',
              border: isRefund ? '1.5px solid var(--color-primary)' : '1px solid var(--color-border)',
              backgroundColor: isRefund ? 'var(--color-primary-light, #ecfdf5)' : 'var(--color-bg-surface)',
              cursor: 'pointer',
              userSelect: 'none',
              transition: 'all 0.15s ease',
              minHeight: '66px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem', marginBottom: '0.2rem' }}>
              {isRefund ? (
                <CheckCircle2 size={17} color="var(--color-primary)" style={{ flexShrink: 0 }} />
              ) : (
                <Circle size={17} color="var(--color-text-muted)" style={{ flexShrink: 0 }} />
              )}
              <span style={{ fontWeight: 600, fontSize: '0.85rem', color: isRefund ? 'var(--color-primary)' : 'var(--color-text-base)' }}>
                Có hoàn tiền cọc
              </span>
            </div>
            <div style={{ fontSize: '0.73rem', color: 'var(--color-text-muted)', paddingLeft: '1.5rem', lineHeight: 1.3 }}>
              Hoàn tiền và chuyển đơn sang ĐÃ HOÀN TIỀN
            </div>
          </div>

          {/* Card 2: Không hoàn tiền */}
          <div
            role="button"
            tabIndex={0}
            onClick={() => setIsRefund(false)}
            onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') setIsRefund(false); }}
            style={{
              boxSizing: 'border-box',
              display: 'flex',
              flexDirection: 'column',
              padding: '0.65rem 0.75rem',
              borderRadius: '8px',
              border: !isRefund ? '1.5px solid var(--color-primary)' : '1px solid var(--color-border)',
              backgroundColor: !isRefund ? 'var(--color-primary-light, #ecfdf5)' : 'var(--color-bg-surface)',
              cursor: 'pointer',
              userSelect: 'none',
              transition: 'all 0.15s ease',
              minHeight: '66px',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem', marginBottom: '0.2rem' }}>
              {!isRefund ? (
                <CheckCircle2 size={17} color="var(--color-primary)" style={{ flexShrink: 0 }} />
              ) : (
                <Circle size={17} color="var(--color-text-muted)" style={{ flexShrink: 0 }} />
              )}
              <span style={{ fontWeight: 600, fontSize: '0.85rem', color: !isRefund ? 'var(--color-primary)' : 'var(--color-text-base)' }}>
                Không hoàn tiền
              </span>
            </div>
            <div style={{ fontSize: '0.73rem', color: 'var(--color-text-muted)', paddingLeft: '1.5rem', lineHeight: 1.3 }}>
              Khách mất cọc / chuyển sang ĐÃ HỦY
            </div>
          </div>
        </div>

        {/* Cấu hình số tiền và phương thức hoàn tiền khi chọn Có hoàn tiền cọc */}
        {isRefund && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', marginBottom: '0.65rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
              <label style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--color-text-base)' }}>
                Số tiền hoàn trả (VNĐ):
              </label>
              <input
                type="text"
                readOnly
                value={deposit}
                style={{
                  width: '160px',
                  textAlign: 'right',
                  padding: '0.4rem 0.65rem',
                  fontSize: '0.85rem',
                  fontWeight: 600,
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--color-border)',
                  backgroundColor: '#f3f4f6',
                  color: 'var(--color-text-base)',
                  outline: 'none',
                  boxSizing: 'border-box',
                }}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem', color: 'var(--color-text-base)' }}>
                Phương thức hoàn tiền
              </label>
              <select
                style={{
                  width: '100%',
                  padding: '0.5rem 0.75rem',
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'var(--color-bg-surface)',
                  color: 'var(--color-text-base)',
                  outline: 'none',
                  fontSize: '0.85rem',
                  boxSizing: 'border-box',
                }}
                value={refundMethod}
                onChange={(e) => setRefundMethod(e.target.value as PaymentMethodCode)}
              >
                <option value="VIETQR">Chuyển khoản QR (VietQR)</option>
                <option value="CASH">Tiền mặt tại quầy (CASH)</option>
              </select>
            </div>
          </div>
        )}
      </div>

      {/* 4. Nhập lý do hủy */}
      <div style={{ marginBottom: '0.85rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.35rem' }}>
          <label style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--color-text-base)' }}>
            Lý do hủy đơn (Bắt buộc từ 5 - 500 ký tự) <span style={{ color: 'var(--color-danger)' }}>*</span>
          </label>
          <span style={{ fontSize: '0.72rem', color: 'var(--color-text-muted)' }}>
            {adminCancelReason.length}/500
          </span>
        </div>
        <textarea
          rows={2}
          placeholder="Nhập lý do chi tiết (VD: Khách gọi điện báo hủy do sự cố, sân ngập nước vì mưa bão...)"
          value={adminCancelReason}
          onChange={(e) => {
            setAdminCancelReason(e.target.value);
            if (error) setError(null);
          }}
          maxLength={500}
          style={{
            width: '100%',
            padding: '0.55rem 0.75rem',
            border: error && adminCancelReason.trim().length < 5 ? '1px solid var(--color-danger)' : '1px solid var(--color-border)',
            borderRadius: 'var(--radius-md)',
            backgroundColor: '#ffffff',
            color: 'var(--color-text-base)',
            outline: 'none',
            resize: 'none',
            fontSize: '0.85rem',
            fontFamily: 'inherit',
            boxSizing: 'border-box',
          }}
        />
      </div>

      {/* 5. Nút bấm thao tác - luôn cố định ở chân modal, không bao giờ bị mất */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'flex-end',
          gap: '0.75rem',
          marginTop: 'auto',
          paddingTop: '0.75rem',
          position: 'sticky',
          bottom: 0,
          backgroundColor: 'var(--color-bg-surface)',
          zIndex: 10,
        }}
      >
        <button
          type="button"
          className="btn btn-secondary"
          onClick={onClose}
          disabled={submitting}
          style={{ height: '38px', padding: '0 1.35rem', fontSize: '0.875rem', fontWeight: 600 }}
        >
          Quay lại
        </button>
        <button
          type="button"
          className="btn btn-primary"
          style={{
            height: '38px',
            padding: '0 1.35rem',
            backgroundColor: 'var(--color-danger)',
            color: '#ffffff',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            cursor: submitting ? 'not-allowed' : 'pointer',
            opacity: submitting ? 0.7 : 1,
            fontWeight: 600,
            fontSize: '0.875rem',
          }}
          disabled={submitting}
          onClick={handleCancel}
        >
          {submitting && <Loader2 size={16} className="animate-spin" />}
          Xác nhận Hủy Đơn
        </button>
      </div>
    </ModalOverlay>
  );
};
