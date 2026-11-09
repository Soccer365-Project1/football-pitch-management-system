import React, { useState } from 'react';
import { X, Loader2 } from 'lucide-react';
import { ModalOverlay } from '../../common/ModalOverlay';
import { adminBookingService } from '../../../services/adminBookingService';
import type { AdminBookingSummary } from '../../../types/adminBooking';
import { showToast } from '../../../utils/toast';

export interface ApproveBookingModalProps {
  isOpen: boolean;
  booking: AdminBookingSummary | null;
  onClose: () => void;
  onSuccess: () => void;
}

export const ApproveBookingModal: React.FC<ApproveBookingModalProps> = ({
  isOpen,
  booking,
  onClose,
  onSuccess,
}) => {
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  if (!isOpen || !booking) return null;

  const handleApprove = async () => {
    setSubmitting(true);
    setError(null);
    try {
      await adminBookingService.approveBooking(booking.id);
      showToast('Duyệt đơn đặt sân thành công!', 'success');
      onSuccess();
      onClose();
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Không thể duyệt đơn đặt sân.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <ModalOverlay onClose={onClose} maxWidth="480px">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-xl font-bold">Xác Nhận Duyệt Đơn</h2>
        <button onClick={onClose} className="text-muted hover:text-[var(--color-text-base)]">
          <X size={24} />
        </button>
      </div>

      {error && (
        <div className="p-3 mb-4 text-xs rounded-md bg-red-50 text-danger border border-red-200">
          {error}
        </div>
      )}

      <div className="mb-6 text-base">
        Bạn có chắc chắn muốn duyệt đơn đặt sân của khách hàng <strong>{booking.customerName}</strong> không?
      </div>

      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '1rem' }}>
        <button className="btn btn-secondary" onClick={onClose} disabled={submitting}>
          Hủy bỏ
        </button>
        <button
          className="btn btn-primary flex items-center gap-2"
          onClick={handleApprove}
          disabled={submitting}
        >
          {submitting && <Loader2 size={16} className="animate-spin" />}
          Xác nhận Duyệt
        </button>
      </div>
    </ModalOverlay>
  );
};
