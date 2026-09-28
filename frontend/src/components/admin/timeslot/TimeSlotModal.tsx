import React, { useState, useEffect } from 'react';
import { X, Loader2, AlertCircle, Check, Zap } from 'lucide-react';
import { ModalOverlay } from '../../common/ModalOverlay';
import { TimeInputWithPicker } from '../../common/TimeInputWithPicker';
import { isValidTimeFormat, calculateDurationMinutes, formatDuration } from '../../../utils/timeUtils';
import type { TimeSlot, TimeSlotRequest } from '../../../types/timeslot';

/**
 * =========================================================================================
 * INTERFACE: TimeSlotModalProps
 * =========================================================================================
 * Định nghĩa các thuộc tính truyền vào component TimeSlotModal:
 * - isOpen: Trạng thái mở/đóng modal (true = hiển thị).
 * - isEdit: Cờ phân biệt: true là Sửa ca đá, false là Thêm ca mới.
 * - initialData: Dữ liệu ca đá đang được chọn khi sửa (null khi thêm mới).
 * - onClose: Hàm gọi khi người dùng muốn đóng popup (bấm icon X hoặc nút Hủy).
 * - onSubmit: Hàm gửi dữ liệu TimeSlotRequest về trang cha để gọi API Backend.
 * - submitting: Cờ báo đang lưu (để disable các nút bấm, chống click đúp).
 */
export interface TimeSlotModalProps {
  isOpen: boolean;
  isEdit: boolean;
  initialData?: TimeSlot | null;
  onClose: () => void;
  onSubmit: (data: TimeSlotRequest) => Promise<void>;
  submitting: boolean;
}

/**
 * =========================================================================================
 * COMPONENT: TimeSlotModal (Form Thêm / Sửa Khung Giờ Dùng Chung)
 * =========================================================================================
 * Hợp nhất cả hai popup Thêm mới và Chỉnh sửa khung giờ vào một component duy nhất.
 * - Đầy đủ các tính năng:
 *   1. Vừa cho gõ 24h trên bàn phím (đã chặn chữ) vừa cho bấm icon đồng hồ chọn giờ.
 *   2. Tự động tính và hiển thị thời lượng (phút và giờ) khi cả 2 ô giờ hợp lệ.
 *   3. Ràng buộc nghiệp vụ: Thời lượng ca đá bắt buộc từ 1 giờ (60 phút) đến 2 giờ (120 phút).
 *   4. Hai ô phân loại Giờ thường / Giờ vàng và hai nút hành động cân xứng 50% - 50%.
 */
export const TimeSlotModal: React.FC<TimeSlotModalProps> = ({
  isOpen,
  isEdit,
  initialData,
  onClose,
  onSubmit,
  submitting,
}) => {
  // ---------------------------------------------------------------------------------------
  // 1. Quản lý trạng thái nhập liệu (useState cơ bản)
  // ---------------------------------------------------------------------------------------
  /** Giờ bắt đầu ca đá (định dạng "HH:mm") */
  const [startTime, setStartTime] = useState<string>('');

  /** Giờ kết thúc ca đá (định dạng "HH:mm") */
  const [endTime, setEndTime] = useState<string>('');

  /** Phân loại ca đá: 'NORMAL' (Giờ thường) hoặc 'PEAK' (Giờ vàng cao điểm) */
  const [type, setType] = useState<'NORMAL' | 'PEAK'>('NORMAL');

  /** Thông báo lỗi hiển thị tập trung tại 1 banner trên đỉnh form */
  const [error, setError] = useState<string | null>(null);

  // ---------------------------------------------------------------------------------------
  // 2. Tự động điền dữ liệu khi mở Modal (useEffect)
  // ---------------------------------------------------------------------------------------
  useEffect(() => {
    if (isOpen) {
      if (isEdit && initialData) {
        // Chế độ Sửa: Điền thông tin ca đá hiện tại
        setStartTime(initialData.startTime || '');
        setEndTime(initialData.endTime || '');
        setType(initialData.isPeakHour ? 'PEAK' : 'NORMAL');
      } else {
        // Chế độ Thêm mới: Đặt lại giá trị trống
        setStartTime('');
        setEndTime('');
        setType('NORMAL');
      }
      setError(null);
    }
  }, [isOpen, isEdit, initialData]);

  if (!isOpen) return null;

  // ---------------------------------------------------------------------------------------
  // 3. Tính toán thời lượng tức thì khi người dùng nhập liệu
  // ---------------------------------------------------------------------------------------
  // Chỉ tính thời lượng khi cả 2 ô giờ đều đã nhập đúng định dạng 24h chuẩn (HH:mm)
  const isBothTimeValid = isValidTimeFormat(startTime) && isValidTimeFormat(endTime);
  const duration = isBothTimeValid ? calculateDurationMinutes(startTime, endTime) : 0;
  const isDurationValid = duration >= 60 && duration <= 120;

  // ---------------------------------------------------------------------------------------
  // 4. Xử lý khi người dùng bấm Lưu / Tạo ca đá
  // ---------------------------------------------------------------------------------------
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // Ca biên 1: Không được để trống
    if (!startTime.trim() || !endTime.trim()) {
      setError('Vui lòng nhập đầy đủ giờ bắt đầu và giờ kết thúc!');
      return;
    }

    // Ca biên 2: Định dạng phải đúng chuẩn HH:mm
    if (!isValidTimeFormat(startTime) || !isValidTimeFormat(endTime)) {
      setError('Định dạng giờ không hợp lệ! Vui lòng nhập định dạng HH:mm (ví dụ: 06:00, 17:30).');
      return;
    }

    // Ca biên 3: Giờ kết thúc phải lớn hơn giờ bắt đầu
    if (endTime <= startTime) {
      setError('Giờ kết thúc phải lớn hơn giờ bắt đầu!');
      return;
    }

    // Ca biên 4: Ràng buộc thời lượng ca đá bắt buộc từ 1h đến 2h (60p - 120p)
    if (duration < 60 || duration > 120) {
      setError(
        `Thời lượng ca đá hiện tại là ${duration} phút. Quy định ca đá phải từ 1 giờ (60 phút) đến 2 giờ (120 phút)!`
      );
      return;
    }

    // Đóng gói payload gửi về cho trang cha
    const payload: TimeSlotRequest = {
      startTime: startTime.trim(),
      endTime: endTime.trim(),
      isPeakHour: type === 'PEAK',
    };

    try {
      await onSubmit(payload);
    } catch (err: any) {
      const errorMsg =
        err.response?.data?.message ||
        'Không thể lưu khung giờ. Vui lòng kiểm tra lại thời gian (tránh trùng với ca khác)!';
      setError(errorMsg);
    }
  };

  return (
    <ModalOverlay onClose={() => !submitting && onClose()} maxWidth="540px">
      {/* Tiêu đề Modal và Nút X */}
      <div className="flex justify-between items-center mb-6 shrink-0">
        <div>
          <h2 className="text-xl font-bold tracking-tight">
            {isEdit ? 'Chỉnh Sửa Khung Giờ' : 'Thêm Khung Giờ Mới'}
          </h2>
          <p className="text-xs text-muted mt-1">
            {isEdit ? 'Cập nhật khoảng thời gian và loại giá ca đá' : 'Thiết lập khoảng giờ (1h - 2h) và loại giá áp dụng'}
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

      {/* Banner thông báo lỗi duy nhất trên đỉnh khi người dùng vi phạm */}
      {error && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            backgroundColor: 'rgba(239, 68, 68, 0.1)',
            border: '1px solid var(--color-danger)',
            color: 'var(--color-danger)',
            padding: '0.85rem 1rem',
            borderRadius: 'var(--radius-md)',
            marginBottom: '1.25rem',
            fontSize: '0.875rem',
            lineHeight: 1.4,
          }}
        >
          <AlertCircle size={18} style={{ flexShrink: 0 }} />
          <span>{error}</span>
        </div>
      )}

      {/* Form nhập liệu nội dung */}
      <form onSubmit={handleSubmit} className="flex flex-col" style={{ flex: 1 }}>
        <div
          style={{
            overflowY: 'auto',
            display: 'flex',
            flexDirection: 'column',
            gap: '1.5rem',
            paddingRight: '0.25rem',
            marginBottom: '1.5rem',
          }}
        >
          {/* KHỐI 1: Cặp ô chọn giờ bắt đầu & giờ kết thúc CÂN XỨNG (50% - 50%) */}
          <div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <TimeInputWithPicker
                label="Giờ bắt đầu"
                value={startTime}
                disabled={submitting}
                onChange={(val) => {
                  setStartTime(val);
                  if (error) setError(null); // Tự động xóa banner lỗi khi người dùng sửa
                }}
                placeholder="HH:mm (VD: 06:00)"
              />

              <TimeInputWithPicker
                label="Giờ kết thúc"
                value={endTime}
                disabled={submitting}
                onChange={(val) => {
                  setEndTime(val);
                  if (error) setError(null);
                }}
                placeholder="HH:mm (VD: 07:30)"
              />
            </div>

            {/* Dòng hướng dẫn thời lượng: CHỈ HIỂN THỊ KHI CẢ 2 GIỜ ĐỀU ĐÃ NHẬP ĐÚNG CHUẨN HH:mm */}
            {isBothTimeValid && (
              <div
                style={{
                  marginTop: '0.75rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  fontSize: '0.85rem',
                  padding: '0.5rem 0.75rem',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: isDurationValid ? 'rgba(16, 185, 129, 0.1)' : 'rgba(239, 68, 68, 0.1)',
                  color: isDurationValid ? 'var(--color-primary)' : 'var(--color-danger)',
                  border: `1px solid ${isDurationValid ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)'}`,
                }}
              >
                {isDurationValid ? <Check size={16} /> : <AlertCircle size={16} />}
                <span>
                  Thời lượng ca đá: <strong>{duration} phút</strong> ({formatDuration(duration)})
                  {isDurationValid ? ' — Hợp lệ (1h - 2h)' : ' — Không hợp lệ (Quy định từ 60 đến 120 phút)'}
                </span>
              </div>
            )}
          </div>

          {/* KHỐI 2: Chọn Phân Loại Ca Đá (2 Card BẰNG NHAU HOÀN TOÀN 50% - 50%) */}
          <div>
            <label className="block text-sm font-semibold mb-2">Phân loại khung giờ</label>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              {/* Card 1: Giờ thường */}
              <button
                type="button"
                onClick={() => setType('NORMAL')}
                disabled={submitting}
                style={{
                  minHeight: '84px',
                  padding: '0.85rem 1rem',
                  borderRadius: 'var(--radius-md)',
                  border: type === 'NORMAL' ? '2px solid var(--color-primary)' : '1px solid var(--color-border)',
                  backgroundColor: type === 'NORMAL' ? 'rgba(16, 185, 129, 0.1)' : 'var(--color-bg-base)',
                  cursor: 'pointer',
                  textAlign: 'left',
                  transition: 'all 0.2s ease',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                }}
              >
                <div className="flex items-center justify-between w-full">
                  <span
                    style={{
                      fontWeight: 600,
                      fontSize: '0.95rem',
                      color: type === 'NORMAL' ? 'var(--color-primary)' : 'var(--color-text-base)',
                    }}
                  >
                    Giờ thường
                  </span>
                  <span
                    className="badge badge-success"
                    style={{
                      fontSize: '0.725rem',
                      padding: '0.15rem 0.45rem',
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '0.2rem',
                      opacity: type === 'NORMAL' ? 1 : 0.6,
                      backgroundColor: type === 'NORMAL' ? 'rgba(16, 185, 129, 0.2)' : 'rgba(107, 114, 128, 0.1)',
                      color: type === 'NORMAL' ? 'var(--color-primary)' : 'var(--color-text-muted)',
                    }}
                  >
                    {type === 'NORMAL' && <Check size={12} />} Tiêu chuẩn
                  </span>
                </div>
                <span className="text-xs text-muted mt-1">Khung giờ tiêu chuẩn, giá thường</span>
              </button>

              {/* Card 2: Giờ vàng */}
              <button
                type="button"
                onClick={() => setType('PEAK')}
                disabled={submitting}
                style={{
                  minHeight: '84px',
                  padding: '0.85rem 1rem',
                  borderRadius: 'var(--radius-md)',
                  border: type === 'PEAK' ? '2px solid #f59e0b' : '1px solid var(--color-border)',
                  backgroundColor: type === 'PEAK' ? 'rgba(245, 158, 11, 0.1)' : 'var(--color-bg-base)',
                  cursor: 'pointer',
                  textAlign: 'left',
                  transition: 'all 0.2s ease',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                }}
              >
                <div className="flex items-center justify-between w-full">
                  <span
                    style={{
                      fontWeight: 600,
                      fontSize: '0.95rem',
                      color: type === 'PEAK' ? '#d97706' : 'var(--color-text-base)',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.25rem',
                    }}
                  >
                    <Zap size={14} className="fill-current" /> Giờ vàng
                  </span>
                  <span
                    className="badge badge-warning"
                    style={{
                      fontSize: '0.725rem',
                      padding: '0.15rem 0.45rem',
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '0.2rem',
                      opacity: type === 'PEAK' ? 1 : 0.6,
                      backgroundColor: type === 'PEAK' ? 'rgba(245, 158, 11, 0.2)' : 'rgba(107, 114, 128, 0.1)',
                      color: type === 'PEAK' ? '#d97706' : 'var(--color-text-muted)',
                    }}
                  >
                    {type === 'PEAK' && <Check size={12} />} Cao điểm
                  </span>
                </div>
                <span className="text-xs text-muted mt-1">Khung giờ cao điểm có giá cao hơn</span>
              </button>
            </div>
          </div>
        </div>

        {/* Chân Modal: 2 Nút Bấm Hành Động CÂN XỨNG (50% - 50%) */}
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
            style={{ height: '46px', fontWeight: 600 }}
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
              height: '46px',
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
                {isEdit ? 'Đang lưu...' : 'Đang tạo...'}
              </>
            ) : isEdit ? (
              'Lưu Thay Đổi'
            ) : (
              'Tạo Khung Giờ'
            )}
          </button>
        </div>
      </form>
    </ModalOverlay>
  );
};

export default TimeSlotModal;
