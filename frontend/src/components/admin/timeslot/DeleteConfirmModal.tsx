import React from 'react';
import { Trash2, X, Loader2 } from 'lucide-react';
import { ModalOverlay } from '../../common/ModalOverlay';
import type { TimeSlot } from '../../../types/timeslot';

/**
 * =========================================================================================
 * INTERFACE: DeleteConfirmModalProps
 * =========================================================================================
 * Định nghĩa các thuộc tính cho modal xác nhận xóa khung giờ:
 * - slot: Đối tượng khung giờ đang được chọn để xóa (null khi không mở modal).
 * - onClose: Hàm đóng modal khi hủy bỏ.
 * - onConfirm: Hàm thực hiện gọi API xóa khi người dùng bấm "Xác nhận Xóa".
 * - submitting: Cờ báo đang trong quá trình xóa dữ liệu (disable nút, xoay spinner).
 */
export interface DeleteConfirmModalProps {
  slot: TimeSlot | null;
  onClose: () => void;
  onConfirm: () => Promise<void>;
  submitting: boolean;
}

/**
 * =========================================================================================
 * COMPONENT: DeleteConfirmModal (Hộp Thoại Xác Nhận Xóa An Toàn)
 * =========================================================================================
 * Hiển thị cảnh báo trực quan trước khi thực hiện xóa ca đá:
 * 1. Nêu rõ khung giờ chuẩn bị xóa (ví dụ: "06:00 - 07:30").
 * 2. Cảnh báo ràng buộc nghiệp vụ: Nếu ca đá này có lịch đặt trong tương lai chưa hoàn tất,
 *    hệ thống sẽ tự động chặn xóa (mã lỗi 3014) để bảo vệ toàn vẹn dữ liệu.
 * 3. Cặp nút bấm "Hủy bỏ" và "Xác nhận Xóa" cân xứng 50% - 50%.
 */
export const DeleteConfirmModal: React.FC<DeleteConfirmModalProps> = ({
  slot,
  onClose,
  onConfirm,
  submitting,
}) => {
  if (!slot) return null;

  return (
    <ModalOverlay onClose={() => !submitting && onClose()} maxWidth="480px">
      {/* Tiêu đề cảnh báo xóa màu đỏ */}
      <div className="flex justify-between items-center mb-5 shrink-0">
        <h2 className="text-xl font-bold text-danger flex items-center gap-2">
          <Trash2 size={20} /> Xóa Khung Giờ
        </h2>
        <button
          type="button"
          onClick={onClose}
          disabled={submitting}
          className="p-1 rounded-lg text-muted hover:text-[var(--color-text-base)] transition-colors"
          title="Đóng hộp thoại"
        >
          <X size={20} />
        </button>
      </div>

      {/* Nội dung cảnh báo và thông tin chi tiết */}
      <div className="space-y-4 mb-6">
        <p className="text-base">
          Bạn có chắc chắn muốn xóa ca đá{' '}
          <strong className="text-[var(--color-primary)] font-bold">
            {slot.startTime} - {slot.endTime}
          </strong>{' '}
          khỏi hệ thống?
        </p>

        {/* Khung nhắc nhở về ràng buộc dữ liệu đặt sân */}
        <div
          style={{
            padding: '0.85rem 1rem',
            backgroundColor: 'rgba(239, 68, 68, 0.08)',
            border: '1px solid rgba(239, 68, 68, 0.25)',
            borderRadius: 'var(--radius-md)',
            fontSize: '0.85rem',
            color: 'var(--color-text-muted)',
            lineHeight: 1.5,
          }}
        >
          ⚠️ <strong>Lưu ý:</strong> Hệ thống sẽ tự động chặn xóa nếu khung giờ này đang có khách hàng đặt lịch trong tương lai chưa hoàn tất.
        </div>
      </div>

      {/* Chân Modal: 2 Nút Bấm CÂN XỨNG (50% - 50%) */}
      <div
        style={{
          flexShrink: 0,
          paddingTop: '1.25rem',
          borderTop: '1px solid var(--color-border)',
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: '1rem',
        }}
      >
        <button
          type="button"
          className="btn btn-secondary w-full"
          style={{ height: '46px', fontWeight: 600 }}
          onClick={onClose}
          disabled={submitting}
        >
          Hủy bỏ
        </button>
        <button
          type="button"
          className="btn btn-primary w-full"
          style={{
            height: '46px',
            backgroundColor: 'var(--color-danger)',
            borderColor: 'var(--color-danger)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '0.5rem',
            fontWeight: 600,
          }}
          onClick={onConfirm}
          disabled={submitting}
        >
          {submitting ? (
            <>
              <Loader2 size={18} className="animate-spin" />
              Đang xóa...
            </>
          ) : (
            'Xác nhận Xóa'
          )}
        </button>
      </div>
    </ModalOverlay>
  );
};

export default DeleteConfirmModal;
