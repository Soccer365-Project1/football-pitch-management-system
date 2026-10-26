import React from 'react';
import { X } from 'lucide-react';
import { ModalOverlay } from '../../common/ModalOverlay.tsx';
import type { AdminTimelineSlotResponse } from '../../../types/adminTimeline.ts';
import { formatPrice } from '../../../utils/formatUtils.ts';

interface TimelineDetailModalProps {
  slot: AdminTimelineSlotResponse;
  onClose: () => void;
}

export const TimelineDetailModal: React.FC<TimelineDetailModalProps> = ({
  slot,
  onClose,
}) => {
  return (
    <ModalOverlay onClose={onClose} maxWidth="480px">
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
              <span className="timeline-modal-val highlight">{slot.pitchName}</span>
            </div>
            <div className="timeline-modal-row">
              <span className="timeline-modal-label">Khung giờ:</span>
              <span className="timeline-modal-val">
                {slot.startTime?.substring(0, 5)} - {slot.endTime?.substring(0, 5)}
              </span>
            </div>
            <div className="timeline-modal-row">
              <span className="timeline-modal-label">Trạng thái:</span>
              <span className="timeline-modal-val" style={{ fontWeight: 700 }}>
                {slot.status === 'COMPLETED'
                  ? 'Đã hoàn thành'
                  : slot.status === 'WAITING_APPROVAL'
                  ? 'Chờ duyệt'
                  : slot.status}
              </span>
            </div>
            {slot.guestName && (
              <div className="timeline-modal-row">
                <span className="timeline-modal-label">Khách hàng:</span>
                <span className="timeline-modal-val">
                  {slot.guestName} ({slot.guestPhone || '---'})
                </span>
              </div>
            )}
            {slot.totalAmount != null && (
              <div className="timeline-modal-row">
                <span className="timeline-modal-label">Tổng tiền:</span>
                <span className="timeline-modal-val">{formatPrice(Number(slot.totalAmount))}</span>
              </div>
            )}
          </div>
        </div>

        <div className="timeline-modal-footer">
          <button
            type="button"
            onClick={onClose}
            className="timeline-btn-cancel-modal"
            style={{ width: '100%' }}
          >
            Đóng
          </button>
        </div>
      </div>
    </ModalOverlay>
  );
};
