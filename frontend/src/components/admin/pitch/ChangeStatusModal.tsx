import React, { useState, useEffect } from 'react';
import { X, Loader2, Check, Settings, Wrench, PowerOff } from 'lucide-react';
import { ModalOverlay } from '../../common/ModalOverlay';
import type { Pitch, PitchStatus } from '../../../types/pitch';

/**
 * =========================================================================================
 * INTERFACE: ChangeStatusModalProps
 * =========================================================================================
 * Định nghĩa các thuộc tính truyền vào component ChangeStatusModal:
 * - pitch: Đối tượng sân bóng đang cần đổi trạng thái (null khi không mở modal).
 * - onClose: Hàm gọi khi người dùng muốn đóng popup (bấm icon X hoặc nút Hủy bỏ).
 * - onConfirm: Hàm callback gọi API cập nhật trạng thái mới cho sân bóng.
 * - submitting: Cờ báo đang trong quá trình gửi request lên server (disable nút, xoay spinner).
 */
export interface ChangeStatusModalProps {
  pitch: Pitch | null;
  onClose: () => void;
  onConfirm: (newStatus: PitchStatus) => Promise<void>;
  submitting: boolean;
}

/**
 * =========================================================================================
 * COMPONENT: ChangeStatusModal (Hộp Thoại Đổi Trạng Thái Sân Bóng Trực Quan)
 * =========================================================================================
 * Thay thế cho hộp thoại SweetAlert2 radio cũ (bị lỗi chữ dồn ép thành 3 cột ngang xấu xí).
 * - Hiển thị 3 thẻ trạng thái xếp dọc (vertical stack) rộng rãi, thoáng mắt.
 * - Mỗi thẻ có Icon, Tên trạng thái và Mô tả chi tiết rõ ràng.
 * - Khi click chọn thẻ, viền sẽ sáng màu và xuất hiện dấu tích xanh.
 * - Cặp nút bấm "Hủy bỏ" và "Cập nhật" cân xứng 50% - 50%.
 */
export const ChangeStatusModal: React.FC<ChangeStatusModalProps> = ({
  pitch,
  onClose,
  onConfirm,
  submitting,
}) => {
  /** Trạng thái được chọn trong form (ACTIVE, MAINTENANCE, INACTIVE) */
  const [selectedStatus, setSelectedStatus] = useState<PitchStatus>('ACTIVE');

  // Khi modal mở lên, đồng bộ trạng thái hiện tại của sân bóng vào state
  useEffect(() => {
    if (pitch) {
      setSelectedStatus(pitch.status);
    }
  }, [pitch]);

  if (!pitch) return null;

  // Danh sách 3 trạng thái hoạt động với mô tả và màu sắc đặc trưng
  const statusOptions: Array<{
    value: PitchStatus;
    title: string;
    description: string;
    icon: React.ReactNode;
    color: string;
    borderColor: string;
    bgColor: string;
  }> = [
    {
      value: 'ACTIVE',
      title: 'Đang hoạt động',
      description: 'Mở sân đón khách đặt lịch thi đấu bình thường',
      icon: <Settings size={20} className="text-emerald-500" />,
      color: 'var(--color-primary)',
      borderColor: 'var(--color-primary)',
      bgColor: 'rgba(16, 185, 129, 0.08)',
    },
    {
      value: 'MAINTENANCE',
      title: 'Đang bảo trì',
      description: 'Tạm dừng nhận lịch đặt để sửa chữa, nâng cấp mặt cỏ',
      icon: <Wrench size={20} className="text-amber-500" />,
      color: '#d97706',
      borderColor: '#f59e0b',
      bgColor: 'rgba(245, 158, 11, 0.08)',
    },
    {
      value: 'INACTIVE',
      title: 'Ngừng hoạt động',
      description: 'Đóng sân, không nhận đặt lịch, lưu trữ dữ liệu lịch sử',
      icon: <PowerOff size={20} className="text-gray-400" />,
      color: 'var(--color-text-muted)',
      borderColor: 'var(--color-border)',
      bgColor: 'var(--color-bg-base)',
    },
  ];

  /** Xử lý khi người dùng bấm nút Cập nhật */
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedStatus === pitch.status) {
      onClose(); // Nếu không đổi trạng thái thì chỉ cần đóng modal
      return;
    }
    await onConfirm(selectedStatus);
  };

  return (
    <ModalOverlay onClose={() => !submitting && onClose()} maxWidth="500px">
      {/* Tiêu đề Modal và Nút X */}
      <div className="flex justify-between items-center mb-5 shrink-0">
        <div>
          <h2 className="text-xl font-bold tracking-tight">Cập Nhật Trạng Thái</h2>
          <p className="text-xs text-muted mt-1">
            Sân bóng: <strong className="text-[var(--color-text-base)]">{pitch.name}</strong>
          </p>
        </div>
        <button
          type="button"
          onClick={onClose}
          disabled={submitting}
          className="text-muted hover:text-[var(--color-text-base)] p-1 rounded-lg transition-colors"
          title="Đóng hộp thoại"
        >
          <X size={20} />
        </button>
      </div>

      {/* Form lựa chọn trạng thái */}
      <form onSubmit={handleSubmit} className="flex flex-col" style={{ flex: 1 }}>
        <p className="text-sm font-semibold mb-3">Chọn trạng thái hoạt động mới cho sân bóng:</p>

        {/* Danh sách các tùy chọn xếp dọc (Vertical Stack), không bị dồn ép ngang */}
        <div className="flex flex-col gap-3 mb-6">
          {statusOptions.map((opt) => {
            const isSelected = selectedStatus === opt.value;

            return (
              <button
                key={opt.value}
                type="button"
                onClick={() => setSelectedStatus(opt.value)}
                disabled={submitting}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '1rem',
                  padding: '1rem 1.15rem',
                  borderRadius: 'var(--radius-md)',
                  border: isSelected ? `2px solid ${opt.borderColor}` : '1px solid var(--color-border)',
                  backgroundColor: isSelected ? opt.bgColor : 'var(--color-bg-surface)',
                  cursor: 'pointer',
                  textAlign: 'left',
                  transition: 'all 0.2s ease',
                  width: '100%',
                }}
              >
                {/* Icon biểu tượng */}
                <div
                  style={{
                    width: '38px',
                    height: '38px',
                    borderRadius: 'var(--radius-md)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    backgroundColor: 'var(--color-bg-base)',
                    flexShrink: 0,
                  }}
                >
                  {opt.icon}
                </div>

                {/* Tiêu đề và Mô tả */}
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 600, fontSize: '0.95rem', color: isSelected ? opt.color : 'var(--color-text-base)' }}>
                    {opt.title}
                  </div>
                  <div className="text-xs text-muted mt-0.5" style={{ lineHeight: 1.4 }}>
                    {opt.description}
                  </div>
                </div>

                {/* Dấu tích tròn khi được chọn */}
                <div
                  style={{
                    width: '22px',
                    height: '22px',
                    borderRadius: '50%',
                    border: isSelected ? `2px solid ${opt.borderColor}` : '2px solid var(--color-border)',
                    backgroundColor: isSelected ? opt.borderColor : 'transparent',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: 'white',
                    flexShrink: 0,
                  }}
                >
                  {isSelected && <Check size={14} strokeWidth={3} />}
                </div>
              </button>
            );
          })}
        </div>

        {/* Chân Modal: 2 Nút Bấm CÂN XỨNG (50% - 50%) */}
        <div
          style={{
            flexShrink: 0,
            paddingTop: '1.25rem',
            marginTop: 'auto',
            borderTop: '1px solid var(--color-border)',
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            gap: '1rem',
          }}
        >
          <button
            type="button"
            className="btn btn-secondary w-full"
            style={{ height: '44px', fontWeight: 600 }}
            onClick={onClose}
            disabled={submitting}
          >
            Hủy bỏ
          </button>
          <button
            type="submit"
            disabled={submitting}
            className="btn btn-primary w-full"
            style={{
              height: '44px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.5rem',
              fontWeight: 600,
            }}
          >
            {submitting ? (
              <>
                <Loader2 size={18} className="animate-spin" />
                Đang lưu...
              </>
            ) : (
              'Cập Nhật'
            )}
          </button>
        </div>
      </form>
    </ModalOverlay>
  );
};

export default ChangeStatusModal;
