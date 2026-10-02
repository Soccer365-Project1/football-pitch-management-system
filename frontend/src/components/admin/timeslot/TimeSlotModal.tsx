import React, { useState, useEffect } from 'react';
import { X, Loader2, AlertCircle, Check, Zap, AlertTriangle, CheckCircle2 } from 'lucide-react';
import { ModalOverlay } from '../../common/ModalOverlay';
import { TimeInputWithPicker } from '../../common/TimeInputWithPicker';
import { isValidTimeFormat, calculateDurationMinutes, formatDuration, addMinutesToTime } from '../../../utils/timeUtils';
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
 * Cải tiến luồng trải nghiệm:
 * 1. Nhập giờ bắt đầu (gõ tay hoặc chọn đồng hồ, không tự ý sửa sai định dạng).
 * 2. Chọn thời lượng từ danh sách: 60 phút, 90 phút, 120 phút.
 * 3. Giờ kết thúc tự động tính toán (Read-only), hiển thị cảnh báo nếu vắt qua ngày hôm sau.
 * 4. Bắt lỗi định dạng chuẩn xác theo ticket TC_TIMESLOT_28.
 * 5. Bật popup xác nhận trước khi Lưu hoặc Hủy.
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
  // 1. Quản lý trạng thái nhập liệu & Popup xác nhận (useState)
  // ---------------------------------------------------------------------------------------
  /** Giờ bắt đầu ca đá (định dạng "HH:mm") */
  const [startTime, setStartTime] = useState<string>('');

  /** Thời lượng ca đá (phút): 60, 90 hoặc 120 (mặc định 90 phút) */
  const [durationMinutes, setDurationMinutes] = useState<number>(90);

  /** Phân loại ca đá: 'NORMAL' (Giờ thường) hoặc 'PEAK' (Giờ vàng cao điểm) */
  const [type, setType] = useState<'NORMAL' | 'PEAK'>('NORMAL');

  /** Thông báo lỗi hiển thị tập trung tại 1 banner trên đỉnh form */
  const [error, setError] = useState<string | null>(null);

  /** Trạng thái mở popup xác nhận trước khi Lưu hoặc Hủy: 'CANCEL' | 'SAVE' | null */
  const [confirmAction, setConfirmAction] = useState<'CANCEL' | 'SAVE' | null>(null);

  // ---------------------------------------------------------------------------------------
  // 2. Tự động điền dữ liệu khi mở Modal (useEffect)
  // ---------------------------------------------------------------------------------------
  useEffect(() => {
    if (isOpen) {
      if (isEdit && initialData) {
        // Chế độ Sửa: Điền thông tin ca đá hiện tại
        setStartTime(initialData.startTime || '');
        setType(initialData.isPeakHour ? 'PEAK' : 'NORMAL');

        // Suy ra thời lượng từ dữ liệu cũ nếu hợp lệ
        if (initialData.startTime && initialData.endTime) {
          const d = calculateDurationMinutes(initialData.startTime, initialData.endTime);
          if ([60, 90, 120].includes(d)) {
            setDurationMinutes(d);
          } else {
            setDurationMinutes(90);
          }
        }
      } else {
        // Chế độ Thêm mới: Đặt lại giá trị ban đầu
        setStartTime('');
        setDurationMinutes(90);
        setType('NORMAL');
      }
      setError(null);
      setConfirmAction(null);
    }
  }, [isOpen, isEdit, initialData]);

  if (!isOpen) return null;

  // ---------------------------------------------------------------------------------------
  // 3. Tự động tính Giờ kết thúc và kiểm tra tràn ngày (Next-day)
  // ---------------------------------------------------------------------------------------
  const isStartTimeValid = isValidTimeFormat(startTime);
  const calculation = isStartTimeValid ? addMinutesToTime(startTime, durationMinutes) : null;
  const calculatedEndTime = calculation ? calculation.endTime : '';
  const isNextDay = calculation ? calculation.isNextDay : false;

  // ---------------------------------------------------------------------------------------
  // 4. Xử lý yêu cầu Hủy bỏ (mở popup xác nhận)
  // ---------------------------------------------------------------------------------------
  const handleRequestClose = () => {
    if (submitting) return;
    setConfirmAction('CANCEL');
  };

  // ---------------------------------------------------------------------------------------
  // 5. Xử lý khi người dùng bấm Lưu / Tạo ca đá (Validate trước khi mở popup xác nhận)
  // ---------------------------------------------------------------------------------------
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // Ca biên 1: Không được để trống giờ bắt đầu
    if (!startTime.trim()) {
      setError('Vui lòng nhập giờ bắt đầu!');
      return;
    }

    // Ca biên 2: Định dạng giờ bắt đầu không hợp lệ (Chuẩn theo Ticket TC_TIMESLOT_28)
    if (!isValidTimeFormat(startTime)) {
      setError('Định dạng thời gian không hợp lệ.');
      return;
    }

    // Ca biên 3: Quá ngày / Vắt sang ngày hôm sau
    if (isNextDay || !calculatedEndTime) {
      setError('Khung giờ phải kết thúc trong cùng ngày (trước 24:00). Không hỗ trợ ca đá kết thúc sang ngày hôm sau!');
      return;
    }

    // Ca biên 4: Giờ kết thúc phải lớn hơn giờ bắt đầu
    if (calculatedEndTime <= startTime) {
      setError('Khung giờ phải kết thúc trong cùng ngày và giờ kết thúc phải lớn hơn giờ bắt đầu!');
      return;
    }

    // Ca biên 5: Ràng buộc thời lượng ca đá
    if (durationMinutes < 60 || durationMinutes > 120) {
      setError('Thời lượng ca đá phải từ 1 giờ (60 phút) đến 2 giờ (120 phút)!');
      return;
    }

    // Dữ liệu hợp lệ 100% -> Mở popup xác nhận Lưu / Tạo
    setConfirmAction('SAVE');
  };

  /** Hàm thực hiện gọi API gửi dữ liệu sau khi người dùng xác nhận trong popup */
  const handleConfirmSave = async () => {
    const payload: TimeSlotRequest = {
      startTime: startTime.trim(),
      endTime: calculatedEndTime,
      isPeakHour: type === 'PEAK',
    };

    try {
      await onSubmit(payload);
      setConfirmAction(null);
    } catch (err: any) {
      setConfirmAction(null);
      const errorMsg =
        err.response?.data?.message ||
        'Không thể lưu khung giờ. Vui lòng kiểm tra lại thời gian (tránh trùng với ca khác)!';
      setError(errorMsg);
    }
  };

  // Chỉ hiển thị 1 banner lỗi duy nhất trên đỉnh modal:
  // Ưu tiên error từ submit hoặc khi giờ kết thúc bị quá ngày
  const displayError =
    error ||
    (isNextDay && isStartTimeValid
      ? 'Khung giờ phải kết thúc trong cùng ngày (trước 24:00). Không hỗ trợ ca đá kết thúc sang ngày hôm sau!'
      : null);

  return (
    <ModalOverlay onClose={handleRequestClose} maxWidth="540px" closeOnClickOutside={false}>
      {/* Tiêu đề Modal và Nút X */}
      <div className="flex justify-between items-center mb-4 shrink-0">
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
          onClick={handleRequestClose}
          disabled={submitting}
          className="text-muted hover:text-[var(--color-text-base)] p-1 rounded-lg transition-colors"
          title="Đóng hộp thoại"
        >
          <X size={20} />
        </button>
      </div>

      {/* DUY NHẤT 1 BANNER THÔNG BÁO LỖI TRÊN ĐỈNH */}
      {displayError && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            backgroundColor: 'rgba(239, 68, 68, 0.1)',
            border: '1px solid var(--color-danger)',
            color: 'var(--color-danger)',
            padding: '0.75rem 1rem',
            borderRadius: 'var(--radius-md)',
            marginBottom: '1rem',
            fontSize: '0.875rem',
            lineHeight: 1.4,
            flexShrink: 0,
          }}
        >
          <AlertCircle size={18} style={{ flexShrink: 0 }} />
          <span>{displayError}</span>
        </div>
      )}

      {/* Form nhập liệu nội dung: Co dãn chuẩn trong flexbox */}
      <form
        onSubmit={handleSubmit}
        style={{
          display: 'flex',
          flexDirection: 'column',
          flex: 1,
          minHeight: 0,
        }}
      >
        <div
          style={{
            overflowY: 'auto',
            display: 'flex',
            flexDirection: 'column',
            gap: '1.25rem',
            paddingRight: '0.25rem',
            marginBottom: '1rem',
            flex: 1,
            minHeight: 0,
          }}
        >
          {/* KHỐI 1: Giờ bắt đầu & Giờ kết thúc tự động tính */}
          <div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <TimeInputWithPicker
                label="Giờ bắt đầu"
                value={startTime}
                disabled={submitting}
                hasError={!!(error && (!startTime.trim() || !isValidTimeFormat(startTime)))}
                onChange={(val) => {
                  setStartTime(val);
                  if (error) setError(null); // Tự động xóa banner lỗi khi người dùng sửa
                }}
                placeholder="HH:mm (VD: 06:00)"
              />

              {/* Ô Giờ kết thúc: CHỈ ĐỌC / TỰ ĐỘNG TÍNH */}
              <div style={{ display: 'flex', flexDirection: 'column' }}>
                <div className="flex items-center justify-between mb-2">
                  <label className="block text-sm font-semibold">Giờ kết thúc</label>
                  <span
                    style={{
                      fontSize: '0.7rem',
                      fontWeight: 500,
                      padding: '0.1rem 0.4rem',
                      borderRadius: 'var(--radius-sm, 4px)',
                      backgroundColor: 'rgba(107, 114, 128, 0.15)',
                      color: 'var(--color-text-muted)',
                    }}
                  >
                    Tự động tính
                  </span>
                </div>

                <div
                  style={{
                    height: '46px',
                    padding: '0.65rem 1rem',
                    border: isNextDay
                      ? '1.5px solid var(--color-danger, #ef4444)'
                      : '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--color-bg-muted, rgba(0, 0, 0, 0.04))',
                    color: isNextDay ? 'var(--color-danger)' : 'var(--color-text-base)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    fontFamily: 'inherit',
                    fontSize: '0.95rem',
                    fontWeight: calculatedEndTime ? 600 : 400,
                    userSelect: 'none',
                  }}
                  title={isNextDay ? 'Thời gian kết thúc vượt quá ngày' : 'Giờ kết thúc được tự động tính'}
                >
                  <span>{calculatedEndTime || '--:--'}</span>
                  {isNextDay && (
                    <span
                      style={{
                        fontSize: '0.725rem',
                        fontWeight: 600,
                        padding: '0.15rem 0.45rem',
                        borderRadius: 'var(--radius-sm, 4px)',
                        backgroundColor: 'rgba(239, 68, 68, 0.15)',
                        color: 'var(--color-danger)',
                      }}
                    >
                      Qua ngày mới
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* KHỐI 1.2: Cụm nút chọn thời lượng (60p, 90p, 120p) */}
            <div style={{ marginTop: '1rem' }}>
              <label className="block text-sm font-semibold mb-2">Thời lượng ca đá</label>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '0.75rem' }}>
                {[
                  { value: 60, label: '60 phút', desc: '1 giờ' },
                  { value: 90, label: '90 phút', desc: '1.5 giờ' },
                  { value: 120, label: '120 phút', desc: '2 giờ' },
                ].map((opt) => {
                  const isSelected = durationMinutes === opt.value;
                  return (
                    <button
                      key={opt.value}
                      type="button"
                      disabled={submitting}
                      onClick={() => {
                        setDurationMinutes(opt.value);
                        if (error) setError(null);
                      }}
                      style={{
                        padding: '0.65rem 0.5rem',
                        borderRadius: 'var(--radius-md)',
                        border: isSelected
                          ? '2px solid var(--color-primary)'
                          : '1px solid var(--color-border)',
                        backgroundColor: isSelected
                          ? 'rgba(16, 185, 129, 0.1)'
                          : 'var(--color-bg-base)',
                        cursor: 'pointer',
                        transition: 'all 0.15s ease',
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '0.2rem',
                      }}
                    >
                      <span
                        style={{
                          fontWeight: isSelected ? 700 : 500,
                          fontSize: '0.9rem',
                          color: isSelected
                            ? 'var(--color-primary)'
                            : 'var(--color-text-base)',
                        }}
                      >
                        {opt.label}
                      </span>
                      <span
                        className="text-xs"
                        style={{
                          color: isSelected
                            ? 'var(--color-primary)'
                            : 'var(--color-text-muted)',
                          opacity: isSelected ? 1 : 0.75,
                        }}
                      >
                        ({opt.desc})
                      </span>
                    </button>
                  );
                })}
              </div>

              {/* Chỉ hiển thị tóm tắt ca đá khi hợp lệ trong cùng ngày (KHÔNG duplicate lỗi đỏ) */}
              {isStartTimeValid && !isNextDay && calculatedEndTime && (
                <div
                  style={{
                    marginTop: '0.75rem',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.5rem',
                    fontSize: '0.85rem',
                    padding: '0.5rem 0.75rem',
                    borderRadius: 'var(--radius-md)',
                    backgroundColor: 'rgba(16, 185, 129, 0.1)',
                    color: 'var(--color-primary)',
                    border: '1px solid rgba(16, 185, 129, 0.3)',
                  }}
                >
                  <Check size={16} />
                  <span>
                    Ca đá: {startTime} - {calculatedEndTime} ({durationMinutes} phút / {formatDuration(durationMinutes)})
                  </span>
                </div>
              )}
            </div>
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
            paddingTop: '1rem',
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
            onClick={handleRequestClose}
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

      {/* ================================================================================= */}
      {/* 6. POPUP XÁC NHẬN HỦY BỎ (KHI BẤM NÚT HỦY HOẶC NÚT X) */}
      {/* ================================================================================= */}
      {confirmAction === 'CANCEL' && (
        <ModalOverlay onClose={() => setConfirmAction(null)} maxWidth="420px" zIndex={11000} closeOnClickOutside={false}>
          <div className="flex flex-col">
            <div className="flex items-center gap-3 mb-3">
              <div
                style={{
                  width: '42px',
                  height: '42px',
                  borderRadius: '50%',
                  backgroundColor: 'rgba(245, 158, 11, 0.15)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: '#d97706',
                  flexShrink: 0,
                }}
              >
                <AlertTriangle size={22} />
              </div>
              <div>
                <h3 className="text-lg font-bold">Xác nhận hủy thao tác</h3>
                <p className="text-xs text-muted">Hủy các thông tin vừa nhập</p>
              </div>
            </div>

            <p className="text-sm text-muted mb-5 leading-relaxed">
              Bạn có chắc chắn muốn hủy? Mọi thông tin khung giờ bạn vừa nhập hoặc thay đổi sẽ không được lưu lại.
            </p>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <button
                type="button"
                className="btn btn-secondary w-full"
                style={{ height: '42px', fontWeight: 600 }}
                onClick={() => setConfirmAction(null)}
              >
                Tiếp tục sửa
              </button>
              <button
                type="button"
                className="btn w-full"
                style={{
                  height: '42px',
                  fontWeight: 600,
                  backgroundColor: 'var(--color-danger, #ef4444)',
                  borderColor: 'var(--color-danger, #ef4444)',
                  color: '#ffffff',
                }}
                onClick={() => {
                  setConfirmAction(null);
                  onClose();
                }}
              >
                Đồng ý hủy
              </button>
            </div>
          </div>
        </ModalOverlay>
      )}

      {/* ================================================================================= */}
      {/* 7. POPUP XÁC NHẬN LƯU / TẠO KHUNG GIỜ (KHI FORM HỢP LỆ VÀ BẤM LƯU/TẠO) */}
      {/* ================================================================================= */}
      {confirmAction === 'SAVE' && (
        <ModalOverlay onClose={() => !submitting && setConfirmAction(null)} maxWidth="440px" zIndex={11000} closeOnClickOutside={false}>
          <div className="flex flex-col">
            <div className="flex items-center gap-3 mb-3">
              <div
                style={{
                  width: '42px',
                  height: '42px',
                  borderRadius: '50%',
                  backgroundColor: 'rgba(16, 185, 129, 0.15)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'var(--color-primary, #10b981)',
                  flexShrink: 0,
                }}
              >
                <CheckCircle2 size={22} />
              </div>
              <div>
                <h3 className="text-lg font-bold">
                  {isEdit ? 'Xác nhận lưu thay đổi' : 'Xác nhận tạo khung giờ'}
                </h3>
                <p className="text-xs text-muted">Vui lòng kiểm tra lại thông tin</p>
              </div>
            </div>

            <p className="text-sm text-muted mb-3 leading-relaxed">
              Bạn có chắc chắn muốn {isEdit ? 'cập nhật' : 'tạo mới'} khung giờ này vào hệ thống?
            </p>

            {/* Thẻ tóm tắt thông tin ca đá */}
            <div
              style={{
                backgroundColor: 'var(--color-bg-muted, rgba(0, 0, 0, 0.03))',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-md)',
                padding: '0.75rem 1rem',
                marginBottom: '1.25rem',
                fontSize: '0.875rem',
                display: 'flex',
                flexDirection: 'column',
                gap: '0.45rem',
              }}
            >
              <div className="flex justify-between items-center">
                <span className="text-muted text-xs">Khung giờ:</span>
                <span className="font-bold text-[var(--color-primary)]">
                  {startTime} - {calculatedEndTime}
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-muted text-xs">Thời lượng:</span>
                <span className="font-semibold">
                  {durationMinutes} phút ({formatDuration(durationMinutes)})
                </span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-muted text-xs">Phân loại:</span>
                <span
                  className="font-semibold"
                  style={{ color: type === 'PEAK' ? '#d97706' : 'var(--color-primary)' }}
                >
                  {type === 'PEAK' ? '⚡ Giờ vàng (Cao điểm)' : '✓ Giờ thường (Tiêu chuẩn)'}
                </span>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <button
                type="button"
                className="btn btn-secondary w-full"
                style={{ height: '42px', fontWeight: 600 }}
                onClick={() => setConfirmAction(null)}
                disabled={submitting}
              >
                Kiểm tra lại
              </button>
              <button
                type="button"
                className="btn btn-primary w-full"
                style={{
                  height: '42px',
                  fontWeight: 600,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '0.5rem',
                }}
                onClick={handleConfirmSave}
                disabled={submitting}
              >
                {submitting ? (
                  <>
                    <Loader2 size={16} className="animate-spin" />
                    {isEdit ? 'Đang lưu...' : 'Đang tạo...'}
                  </>
                ) : isEdit ? (
                  'Xác nhận Lưu'
                ) : (
                  'Xác nhận Tạo'
                )}
              </button>
            </div>
          </div>
        </ModalOverlay>
      )}
    </ModalOverlay>
  );
};

export default TimeSlotModal;
