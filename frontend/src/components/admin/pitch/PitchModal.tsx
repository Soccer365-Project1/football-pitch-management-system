import React, { useState, useEffect } from 'react';
import { X, Loader2 } from 'lucide-react';
import { ModalOverlay } from '../../common/ModalOverlay';
import type { Pitch, PitchType, PitchRequest } from '../../../types/pitch';

/**
 * =========================================================================================
 * INTERFACE: PitchModalProps
 * =========================================================================================
 * Định nghĩa các thuộc tính truyền vào component PitchModal:
 * - isOpen: Trạng thái mở hoặc đóng của modal (true = hiển thị).
 * - isEdit: Cờ phân biệt chế độ: true là Chỉnh sửa sân, false là Thêm sân mới.
 * - initialData: Dữ liệu của sân đang được chọn để sửa (null khi đang thêm mới).
 * - pitchTypes: Danh mục các loại sân bóng (để người dùng chọn trong thẻ <select>).
 * - onClose: Hàm gọi khi người dùng muốn đóng modal (bấm icon X hoặc nút Hủy).
 * - onSubmit: Hàm gửi dữ liệu sân bóng đã nhập về trang cha để gọi API Backend.
 * - submitting: Trạng thái cờ báo đang gửi dữ liệu lên server (để disable nút bấm, tránh click đúp).
 * - serverError: Thông báo lỗi từ server trả về nếu có (ví dụ: "Tên sân bóng đã tồn tại").
 */
export interface PitchModalProps {
  isOpen: boolean;
  isEdit: boolean;
  initialData?: Pitch | null;
  pitchTypes: PitchType[];
  onClose: () => void;
  onSubmit: (data: PitchRequest) => Promise<void>;
  submitting: boolean;
  serverError?: string | null;
}

/**
 * =========================================================================================
 * COMPONENT: PitchModal (Form Thêm / Sửa Sân Bóng Dùng Chung)
 * =========================================================================================
 * Hợp nhất cả hai popup Thêm mới và Chỉnh sửa vào một component duy nhất.
 * Giúp mã nguồn cực kỳ gọn gàng, không bị lặp lại HTML hay logic validate.
 */
export const PitchModal: React.FC<PitchModalProps> = ({
  isOpen,
  isEdit,
  initialData,
  pitchTypes,
  onClose,
  onSubmit,
  submitting,
  serverError,
}) => {
  // ---------------------------------------------------------------------------------------
  // 1. Quản lý trạng thái các trường nhập liệu trong Form (useState cơ bản)
  // ---------------------------------------------------------------------------------------
  /** Tên của sân bóng (ví dụ: "Sân 5 số 1", "Sân 7 VIP") */
  const [name, setName] = useState<string>('');

  /** Mã ID của loại sân (ví dụ: 1 là Sân 5, 2 là Sân 7) */
  const [pitchTypeId, setPitchTypeId] = useState<number | string>('');

  /** Mô tả hoặc ghi chú thêm về sân bóng (tùy chọn) */
  const [description, setDescription] = useState<string>('');

  /** Lưu trữ thông báo lỗi validation hiển thị ngay dưới từng ô nhập */
  const [errors, setErrors] = useState<{ name?: string; typeId?: string }>({});

  // ---------------------------------------------------------------------------------------
  // 2. Tự động điền dữ liệu khi mở Modal (useEffect)
  // ---------------------------------------------------------------------------------------
  useEffect(() => {
    if (isOpen) {
      if (isEdit && initialData) {
        // Chế độ Chỉnh sửa: Điền sẵn thông tin sân hiện tại vào các ô nhập
        setName(initialData.name || '');
        setPitchTypeId(initialData.pitchType?.id || '');
        setDescription(initialData.description || '');
      } else {
        // Chế độ Thêm mới: Đặt lại form về trạng thái trống ban đầu
        setName('');
        // Mặc định chọn loại sân đầu tiên trong danh mục nếu có
        setPitchTypeId(pitchTypes.length > 0 ? pitchTypes[0].id : '');
        setDescription('');
      }
      // Xóa các lỗi validation cũ khi mở lại modal
      setErrors({});
    }
  }, [isOpen, isEdit, initialData, pitchTypes]);

  // Nếu modal không được bật thì không render gì cả
  if (!isOpen) return null;

  // ---------------------------------------------------------------------------------------
  // 3. Xử lý khi người dùng bấm nút Submit (Lưu / Tạo sân)
  // ---------------------------------------------------------------------------------------
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const newErrors: { name?: string; typeId?: string } = {};

    // Ca biên 1: Kiểm tra không được để trống tên sân bóng
    if (!name.trim()) {
      newErrors.name = 'Vui lòng nhập tên sân bóng';
    }

    // Ca biên 2: Kiểm tra phải chọn một loại sân hợp lệ
    if (!pitchTypeId) {
      newErrors.typeId = 'Vui lòng chọn loại sân bóng';
    }

    // Nếu phát hiện có lỗi thì hiển thị thông báo và dừng lại, không gọi API
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    // Đóng gói dữ liệu theo chuẩn PitchRequest gửi cho trang cha xử lý
    const payload: PitchRequest = {
      name: name.trim(),
      pitchTypeId: Number(pitchTypeId),
      description: description.trim(),
    };

    await onSubmit(payload);
  };

  return (
    <ModalOverlay onClose={() => !submitting && onClose()} maxWidth="500px">
      {/* Tiêu đề Modal và Nút X để đóng */}
      <div className="flex justify-between items-center mb-6 shrink-0">
        <div>
          <h2 className="text-xl font-bold tracking-tight">
            {isEdit ? 'Chỉnh Sửa Sân Bóng' : 'Thêm Sân Bóng Mới'}
          </h2>
          <p className="text-xs text-muted mt-1">
            {isEdit ? 'Cập nhật tên, loại sân hoặc thông tin mô tả' : 'Nhập thông tin sân bóng mới vào hệ thống'}
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

      {/* Form nhập liệu */}
      <form onSubmit={handleSubmit} className="flex flex-col" style={{ flex: 1 }}>
        <div className="grid gap-4 mb-6" style={{ overflowY: 'auto', paddingRight: '0.25rem' }}>
          {/* Ô nhập 1: Tên Sân Bóng */}
          <div>
            <label className="block text-sm font-semibold mb-2">
              Tên sân bóng <span className="text-danger">*</span>
            </label>
            <input
              type="text"
              className="w-full"
              placeholder="Ví dụ: Sân 5 số 1, Sân VIP..."
              value={name}
              maxLength={100}
              disabled={submitting}
              onChange={(e) => {
                setName(e.target.value);
                if (errors.name) setErrors((prev) => ({ ...prev, name: undefined }));
              }}
              style={{
                height: '44px',
                padding: '0.6rem 1rem',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-md)',
                background: 'var(--color-bg-base)',
                color: 'var(--color-text-base)',
                outline: 'none',
              }}
            />
            {/* Hiển thị lỗi validation hoặc thông báo lỗi từ server */}
            {(errors.name || serverError) && (
              <p className="text-xs text-danger mt-1 font-medium">{errors.name || serverError}</p>
            )}
          </div>

          {/* Ô nhập 2: Chọn Loại Sân Bóng (Dropdown) */}
          <div>
            <label className="block text-sm font-semibold mb-2">
              Loại sân bóng <span className="text-danger">*</span>
            </label>
            <select
              className="w-full"
              value={pitchTypeId}
              disabled={submitting}
              onChange={(e) => {
                setPitchTypeId(Number(e.target.value));
                if (errors.typeId) setErrors((prev) => ({ ...prev, typeId: undefined }));
              }}
              style={{
                height: '44px',
                padding: '0.6rem 1rem',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-md)',
                background: 'var(--color-bg-base)',
                color: 'var(--color-text-base)',
                outline: 'none',
              }}
            >
              {pitchTypes.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name} (Tối đa {t.playerCapacity} người)
                </option>
              ))}
            </select>
            {errors.typeId && <p className="text-xs text-danger mt-1 font-medium">{errors.typeId}</p>}
          </div>

          {/* Ô nhập 3: Mô Tả Sân Bóng */}
          <div>
            <label className="block text-sm font-semibold mb-2">Mô tả (tùy chọn)</label>
            <textarea
              rows={3}
              className="w-full"
              placeholder="Ghi chú về mặt cỏ, hệ thống chiếu sáng, vị trí sân..."
              value={description}
              disabled={submitting}
              onChange={(e) => setDescription(e.target.value)}
              style={{
                padding: '0.6rem 1rem',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-md)',
                background: 'var(--color-bg-base)',
                color: 'var(--color-text-base)',
                resize: 'none',
                outline: 'none',
              }}
            />
          </div>
        </div>

        {/* Chân Modal: Các nút bấm hành động */}
        <div
          style={{
            flexShrink: 0,
            paddingTop: '1rem',
            marginTop: 'auto',
            borderTop: '1px solid var(--color-border)',
          }}
        >
          {isEdit ? (
            /* Chế độ Chỉnh sửa: 2 Nút Hủy và Lưu chia đều 50% - 50% */
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
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
                style={{ height: '44px', fontWeight: 600 }}
              >
                {submitting ? (
                  <span className="flex items-center justify-center gap-2">
                    <Loader2 size={18} className="animate-spin" /> Đang lưu...
                  </span>
                ) : (
                  'Lưu Thay Đổi'
                )}
              </button>
            </div>
          ) : (
            /* Chế độ Thêm mới: 1 Nút Tạo sân mới chiếm toàn bộ chiều rộng */
            <button
              type="submit"
              disabled={submitting}
              className="btn btn-primary w-full"
              style={{ height: '44px', fontWeight: 600 }}
            >
              {submitting ? (
                <span className="flex items-center justify-center gap-2">
                  <Loader2 size={18} className="animate-spin" /> Đ đang tạo...
                </span>
              ) : (
                'Tạo Sân Mới'
              )}
            </button>
          )}
        </div>
      </form>
    </ModalOverlay>
  );
};

export default PitchModal;
