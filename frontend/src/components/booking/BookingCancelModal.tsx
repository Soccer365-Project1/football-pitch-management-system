import React, { useState } from 'react';
import { AlertTriangle, X, ShieldAlert } from 'lucide-react';
import { ModalOverlay } from '../common/ModalOverlay.tsx';
import type { BookingSummaryResponse } from '../../services/bookingService.ts';

export const PRESET_CANCEL_REASONS = [
  'Bận việc đột xuất',
  'Thời tiết xấu / Mưa bão',
  'Đổi lịch thi đấu với đối thủ',
  'Không đủ thành viên tham gia',
  'Đặt nhầm sân / khung giờ',
];

export interface BookingCancelModalProps {
  booking: BookingSummaryResponse | null;
  onClose: () => void;
  onConfirm: (bookingId: number, reason: string) => Promise<void>;
  formatPrice: (price: number) => string;
}

export const BookingCancelModal: React.FC<BookingCancelModalProps> = ({
  booking,
  onClose,
  onConfirm,
  formatPrice,
}) => {
  const [cancelReason, setCancelReason] = useState<string>('');

  if (!booking) return null;

  const handleClose = () => {
    setCancelReason('');
    onClose();
  };

  const handleSubmit = async () => {
    if (!cancelReason.trim()) return;
    const reason = cancelReason.trim();
    // Đóng popup modal ngay lập tức khi bấm submit theo yêu cầu
    handleClose();
    try {
      await onConfirm(booking.id, reason);
    } catch {
      // Lỗi được xử lý và hiển thị toast ở component cha
    }
  };

  const depositAmount = booking.depositAmount ?? (booking.totalAmount * 0.3);

  return (
    <ModalOverlay onClose={handleClose} maxWidth="560px">
      <div className="cancel-modal-container">
        {/* Header (Cố định ở trên) */}
        <div className="cancel-modal-header">
          <div className="cancel-modal-header-left">
            <div className="cancel-modal-icon-wrapper">
              <AlertTriangle size={22} />
            </div>
            <div>
              <h3 className="cancel-modal-title">Yêu cầu hủy đơn đặt sân</h3>
              <p className="cancel-modal-subtitle">
                Mã đơn: #{booking.bookingCode ? booking.bookingCode.replace('#', '') : booking.id}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleClose}
            className="cancel-modal-close-btn"
            aria-label="Đóng"
            title="Đóng"
          >
            <X size={20} />
          </button>
        </div>

        {/* Nội dung có thể cuộn (Scrollable Body) */}
        <div className="cancel-modal-body">
          {/* Thông tin đơn đặt sân */}
          <div className="cancel-modal-summary-card">
            <div className="cancel-modal-summary-row">
              <span className="cancel-modal-summary-label">Sân bóng:</span>
              <span className="cancel-modal-summary-val" title={booking.pitchName}>
                {booking.pitchName}
              </span>
            </div>
            <div className="cancel-modal-summary-row">
              <span className="cancel-modal-summary-label">Khung giờ:</span>
              <span className="cancel-modal-summary-val">
                {booking.bookingDate} ({booking.startTime?.substring(0, 5)} - {booking.endTime?.substring(0, 5)})
              </span>
            </div>
            <div className="cancel-modal-summary-row">
              <span className="cancel-modal-summary-label">Tiền cọc đã thanh toán:</span>
              <span className="cancel-modal-summary-val highlight">
                {formatPrice(depositAmount)}
              </span>
            </div>
          </div>

          {/* Hộp quy định hoàn tiền cọc */}
          <div className="cancel-modal-policy-card">
            <div className="cancel-modal-policy-title">
              <ShieldAlert size={16} />
              <span>Quy định hủy sân (Chính sách hoàn cọc)</span>
            </div>
            <ul className="cancel-modal-policy-list">
              <li>
                <strong>Thời hạn:</strong> Chỉ hỗ trợ hoàn cọc khi hủy trước ít nhất <strong>24 giờ</strong> so với giờ đá.
              </li>
              <li>
                <strong>Xét duyệt:</strong> Đơn sẽ chuyển sang <em>"Chờ xác nhận hủy"</em> và được nhân viên kiểm tra, xét duyệt hoàn tiền cọc.
              </li>
            </ul>
          </div>

          {/* Phần chọn & nhập lý do hủy */}
          <div className="cancel-modal-reason-section">
            <label className="cancel-modal-reason-label">
              Lý do hủy đơn <span style={{ color: '#ef4444' }}>(*)</span>
            </label>
            <div className="cancel-modal-chips">
              {PRESET_CANCEL_REASONS.map((preset) => {
                const isSelected = cancelReason === preset;
                return (
                  <button
                    key={preset}
                    type="button"
                    onClick={() => setCancelReason(preset)}
                    className={`cancel-modal-chip ${isSelected ? 'active' : ''}`}
                  >
                    {preset}
                  </button>
                );
              })}
            </div>
            <textarea
              rows={3}
              placeholder="Nhập chi tiết lý do bạn muốn hủy đơn đặt sân này (bắt buộc)..."
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              className="cancel-modal-textarea"
            />
          </div>
        </div>

        {/* Nút hành động (Luôn cố định ở đáy, không bao giờ bị che) */}
        <div className="cancel-modal-actions">
          <button
            type="button"
            onClick={handleClose}
            className="cancel-modal-btn-cancel"
          >
            Hủy bỏ
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={!cancelReason.trim()}
            className="cancel-modal-btn-confirm"
          >
            Xác nhận hủy đơn
          </button>
        </div>
      </div>
    </ModalOverlay>
  );
};

export default BookingCancelModal;
