import React from 'react';
import { X, CheckCircle, Play, Loader2, AlertCircle } from 'lucide-react';
import { ModalOverlay } from '../../common/ModalOverlay.tsx';
import type { AdminTimelineSlotResponse } from '../../../types/adminTimeline.ts';
import { formatPrice, formatDate } from '../../../utils/formatUtils.ts';

interface TimelineDetailModalProps {
  slot: AdminTimelineSlotResponse;
  onClose: () => void;
  onApprove?: (bookingId: number) => void;
  onCheckIn?: (slot: AdminTimelineSlotResponse) => void;
  isApproving?: boolean;
  isCheckingIn?: boolean;
}

export const canCheckInSlot = (slot: AdminTimelineSlotResponse): { canCheckIn: boolean; reason?: string } => {
  if (slot.status !== 'CONFIRMED') {
    return { canCheckIn: false, reason: 'Chỉ có thể nhận sân cho đơn đặt sân đã được xác nhận' };
  }

  const now = new Date();
  const todayStr = new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().split('T')[0];

  if (slot.bookingDate && slot.bookingDate !== todayStr) {
    return { canCheckIn: false, reason: 'Chỉ có thể nhận sân cho các ca đá diễn ra trong ngày hôm nay' };
  }

  if (slot.startTime) {
    const [h, m] = slot.startTime.split(':').map(Number);
    const slotStart = new Date();
    slotStart.setHours(h, m, 0, 0);

    if (now < slotStart) {
      return {
        canCheckIn: false,
        reason: `Chưa đến giờ nhận sân (Ca đá bắt đầu lúc ${slot.startTime.substring(0, 5)})`,
      };
    }
  }

  return { canCheckIn: true };
};

export const TimelineDetailModal: React.FC<TimelineDetailModalProps> = ({
  slot,
  onClose,
  onApprove,
  onCheckIn,
  isApproving = false,
  isCheckingIn = false,
}) => {
  const checkInCheck = canCheckInSlot(slot);

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'WAITING_APPROVAL':
        return 'Chờ duyệt';
      case 'CONFIRMED':
        return 'Đã xác nhận';
      case 'IN_PROGRESS':
        return 'Đang sử dụng';
      case 'COMPLETED':
        return 'Đã hoàn thành';
      case 'WAITING_CANCELLATION':
        return 'Yêu cầu hủy';
      case 'CANCELLED':
        return 'Đã hủy';
      case 'PENDING_HOLD':
        return 'Đang giữ chỗ';
      case 'MAINTENANCE':
        return 'Bảo trì';
      default:
        return status;
    }
  };

  return (
    <ModalOverlay onClose={onClose} maxWidth="500px">
      <div className="timeline-modal-container">
        <div className="timeline-modal-header">
          <div className="timeline-modal-header-left">
            <h3 className="timeline-modal-title">Chi tiết Ca Đá</h3>
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

        <div className="timeline-modal-body">
          <div className="timeline-modal-card">
            <div className="timeline-modal-row">
              <span className="timeline-modal-label">Mã đơn:</span>
              <span className="timeline-modal-val">
                #{slot.bookingCode ? slot.bookingCode.replace('#', '') : slot.bookingId}
              </span>
            </div>
            <div className="timeline-modal-row">
              <span className="timeline-modal-label">Sân bóng:</span>
              <span className="timeline-modal-val highlight">
                {slot.pitchName} {slot.pitchTypeName ? `(${slot.pitchTypeName})` : ''}
              </span>
            </div>
            <div className="timeline-modal-row">
              <span className="timeline-modal-label">Khung giờ:</span>
              <span className="timeline-modal-val">
                {slot.startTime?.substring(0, 5)} - {slot.endTime?.substring(0, 5)}
              </span>
            </div>
            {slot.bookingDate && (
              <div className="timeline-modal-row">
                <span className="timeline-modal-label">Ngày đá:</span>
                <span className="timeline-modal-val">{formatDate(slot.bookingDate)}</span>
              </div>
            )}
            <div className="timeline-modal-row">
              <span className="timeline-modal-label">Trạng thái:</span>
              <span className="timeline-modal-val" style={{ fontWeight: 700 }}>
                {getStatusLabel(slot.status)}
              </span>
            </div>
            {slot.guestName && (
              <div className="timeline-modal-row">
                <span className="timeline-modal-label">Khách hàng:</span>
                <span className="timeline-modal-val">
                  {slot.guestName} {slot.guestPhone ? `(${slot.guestPhone})` : ''}
                </span>
              </div>
            )}
            {slot.depositAmount != null && Number(slot.depositAmount) > 0 && (
              <div className="timeline-modal-row">
                <span className="timeline-modal-label">Tiền đã cọc:</span>
                <span className="timeline-modal-val" style={{ color: '#10b981' }}>
                  {formatPrice(Number(slot.depositAmount))}
                </span>
              </div>
            )}
            {slot.remainingAmount != null && (
              <div className="timeline-modal-row">
                <span className="timeline-modal-label">Tiền còn lại:</span>
                <span className="timeline-modal-val">
                  {formatPrice(Number(slot.remainingAmount))}
                </span>
              </div>
            )}
            {slot.totalAmount != null && (
              <div className="timeline-modal-row highlight-remaining">
                <span className="timeline-modal-label">Tổng tiền sân:</span>
                <span className="timeline-modal-val" style={{ fontSize: '1rem', color: 'var(--color-primary)' }}>
                  {formatPrice(Number(slot.totalAmount))}
                </span>
              </div>
            )}
            {slot.customerNote && (
              <div className="timeline-modal-note-box">
                <strong>Ghi chú:</strong> {slot.customerNote}
              </div>
            )}
            {slot.cancellationReason && (
              <div className="timeline-modal-cancel-reason">
                <strong>Lý do hủy:</strong> {slot.cancellationReason}
              </div>
            )}
          </div>

          {/* Hộp thông báo theo từng trạng thái */}
          {slot.status === 'WAITING_APPROVAL' && (
            <div className="timeline-modal-notice-box orange">
              <AlertCircle size={18} style={{ flexShrink: 0, marginTop: 1 }} />
              <div>
                <strong>Chờ nhân viên duyệt đơn:</strong> Đơn đặt sân đã hoàn tất đặt cọc. Nhân viên kiểm tra thông tin và bấm nút <b>"Duyệt đơn đặt sân"</b> bên dưới để chuyển sang trạng thái đã xác nhận.
              </div>
            </div>
          )}

          {slot.status === 'CONFIRMED' && (
            <div className={`timeline-modal-notice-box ${checkInCheck.canCheckIn ? 'green' : 'orange'}`}>
              <AlertCircle size={18} style={{ flexShrink: 0, marginTop: 1 }} />
              <div>
                <strong>Ca đá đã xác nhận:</strong>{' '}
                {checkInCheck.canCheckIn ? (
                  <span>Đã đến giờ ca đá! Bấm nút <b>"Nhận sân"</b> để chuyển sang trạng thái đang đá.</span>
                ) : (
                  <span>{checkInCheck.reason}</span>
                )}
              </div>
            </div>
          )}
        </div>

        <div className="timeline-modal-footer">
          <button
            type="button"
            onClick={onClose}
            className="timeline-btn-cancel-modal"
          >
            Đóng
          </button>

          {/* Nút Duyệt sân cho WAITING_APPROVAL */}
          {slot.status === 'WAITING_APPROVAL' && slot.bookingId && onApprove && (
            <button
              type="button"
              disabled={isApproving}
              onClick={() => onApprove(slot.bookingId!)}
              className="timeline-btn-confirm-checkin"
              style={{ backgroundColor: '#10b981' }}
            >
              {isApproving ? (
                <>
                  <Loader2 size={16} className="animate-spin" />
                  <span>Đang duyệt...</span>
                </>
              ) : (
                <>
                  <CheckCircle size={16} />
                  <span>Duyệt đơn đặt sân</span>
                </>
              )}
            </button>
          )}

          {/* Nút Nhận sân cho CONFIRMED */}
          {slot.status === 'CONFIRMED' && slot.bookingId && onCheckIn && (
            <button
              type="button"
              disabled={isCheckingIn || !checkInCheck.canCheckIn}
              onClick={() => onCheckIn(slot)}
              className="timeline-btn-confirm-checkin"
              title={!checkInCheck.canCheckIn ? checkInCheck.reason : 'Xác nhận nhận sân'}
            >
              {isCheckingIn ? (
                <>
                  <Loader2 size={16} className="animate-spin" />
                  <span>Đang nhận sân...</span>
                </>
              ) : (
                <>
                  <Play size={14} className="fill-current" />
                  <span>Nhận sân</span>
                </>
              )}
            </button>
          )}
        </div>
      </div>
    </ModalOverlay>
  );
};
