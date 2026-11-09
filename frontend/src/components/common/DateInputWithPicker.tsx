import React, { useState, useEffect, useRef } from 'react';
import { Calendar } from 'lucide-react';
import { showToast } from '../../utils/toast.ts';

export interface DateInputWithPickerProps {
  /** Giá trị ngày chuẩn ISO: "YYYY-MM-DD" hoặc chuỗi rỗng "" */
  value: string;
  /** Callback trả về chuỗi ISO "YYYY-MM-DD" hoặc "" khi thay đổi */
  onChange: (val: string) => void;
  placeholder?: string;
  title?: string;
  label?: string; // Tên nhãn để hiển thị trong thông báo lỗi (vd: "Thời gian bắt đầu (Từ ngày)")
  disabled?: boolean;
  min?: string; // Giới hạn ngày tối thiểu (ISO YYYY-MM-DD)
  max?: string; // Giới hạn ngày tối đa (ISO YYYY-MM-DD)
  className?: string;
  style?: React.CSSProperties;
  inputStyle?: React.CSSProperties;
}

/**
 * Chuyển ISO "YYYY-MM-DD" sang định dạng hiển thị "dd/mm/yyyy"
 */
export const isoToDisplay = (iso?: string | null): string => {
  if (!iso) return '';
  const clean = iso.includes('T') ? iso.split('T')[0] : iso;
  const parts = clean.split('-');
  if (parts.length === 3) {
    const [year, month, day] = parts;
    return `${day}/${month}/${year}`;
  }
  return '';
};

/**
 * Kiểm tra và parse chuỗi hiển thị "dd/mm/yyyy" sang chuẩn ISO "YYYY-MM-DD"
 */
export const parseDisplayToIso = (display: string): string | null => {
  const match = display.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
  if (!match) return null;
  const day = parseInt(match[1], 10);
  const month = parseInt(match[2], 10);
  const year = parseInt(match[3], 10);

  if (month < 1 || month > 12 || day < 1 || day > 31 || year < 1900 || year > 2100) {
    return null;
  }

  const date = new Date(year, month - 1, day);
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) {
    return null;
  }

  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
};

/**
 * Component ô nhập ngày linh hoạt:
 * 1. Cho phép nhập tay trực tiếp từ bàn phím theo định dạng ngày/tháng/năm (dd/mm/yyyy),
 *    tự động chèn dấu '/' khi gõ số và xác thực ngày hợp lệ (xét năm nhuận, số ngày trong tháng).
 * 2. Tích hợp nút icon lịch để mở popup chọn ngày gốc của trình duyệt (HTML5 date picker).
 * 3. Hỗ trợ ràng buộc min/max (start time <= end time) kèm hiển thị lỗi Toast và đổi viền đỏ cảnh báo.
 */
export const DateInputWithPicker: React.FC<DateInputWithPickerProps> = ({
  value,
  onChange,
  placeholder = 'dd/mm/yyyy',
  title = 'Chọn hoặc nhập ngày (dd/mm/yyyy)',
  label = 'Ngày',
  disabled = false,
  min,
  max,
  className = '',
  style,
  inputStyle,
}) => {
  const [textValue, setTextValue] = useState<string>(() => isoToDisplay(value));
  const [hasError, setHasError] = useState<boolean>(false);
  const hiddenDatePickerRef = useRef<HTMLInputElement>(null);

  // Đồng bộ lại textValue khi value từ component cha thay đổi
  useEffect(() => {
    setTextValue(isoToDisplay(value));
    setHasError(false);
  }, [value]);

  const handleTextChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const isDelete = (e.nativeEvent as InputEvent)?.inputType?.startsWith('delete');
    let val = e.target.value.replace(/[^0-9/]/g, '').slice(0, 10);

    // Xử lý tự động chèn dấu '/' khi gõ phím hoặc dán số
    if (!isDelete) {
      const digits = val.replace(/\D/g, '');
      if (!val.includes('/') && digits.length >= 3) {
        if (digits.length <= 4) {
          val = `${digits.slice(0, 2)}/${digits.slice(2)}`;
        } else {
          val = `${digits.slice(0, 2)}/${digits.slice(2, 4)}/${digits.slice(4, 8)}`;
        }
      } else if (val.length === 2 && !val.includes('/') && textValue.length === 1) {
        val += '/';
      } else if (val.length === 5 && val.split('/').length === 2 && textValue.length === 4) {
        val += '/';
      }
    }

    setTextValue(val);

    // Người dùng xóa trống ô nhập
    if (val === '') {
      setHasError(false);
      if (value !== '') {
        onChange('');
      }
      return;
    }

    // Nếu chưa gõ đủ 10 ký tự, tắt trạng thái lỗi để người dùng gõ tiếp
    if (val.length < 10) {
      setHasError(false);
      return;
    }

    // Khi đã gõ đủ 10 ký tự (dd/mm/yyyy), tiến hành validate
    const iso = parseDisplayToIso(val);
    if (!iso) {
      setHasError(true);
      showToast(`${label} '${val}' không tồn tại trên lịch`, 'error');
      return;
    }

    if (min && iso < min) {
      setHasError(true);
      showToast(`${label} không được nhỏ hơn ${isoToDisplay(min)}`, 'error');
      return;
    }

    if (max && iso > max) {
      setHasError(true);
      showToast(`${label} không được lớn hơn ${isoToDisplay(max)}`, 'error');
      return;
    }

    // Giá trị hợp lệ
    setHasError(false);
    if (iso !== value) {
      onChange(iso);
    }
  };

  const handleBlur = () => {
    if (!textValue) {
      setHasError(false);
      if (value !== '') {
        onChange('');
      }
      return;
    }

    const iso = parseDisplayToIso(textValue);
    if (!iso) {
      showToast(`${label} '${textValue}' không hợp lệ, đã đặt lại`, 'warning');
      setTextValue(isoToDisplay(value));
      setHasError(false);
      return;
    }

    if (min && iso < min) {
      showToast(`${label} không được nhỏ hơn ${isoToDisplay(min)}, đã đặt lại`, 'warning');
      setTextValue(isoToDisplay(value));
      setHasError(false);
      return;
    }

    if (max && iso > max) {
      showToast(`${label} không được lớn hơn ${isoToDisplay(max)}, đã đặt lại`, 'warning');
      setTextValue(isoToDisplay(value));
      setHasError(false);
      return;
    }

    setHasError(false);
    if (iso !== value) {
      onChange(iso);
    }
  };

  const handleOpenPicker = () => {
    if (disabled) return;
    if (hiddenDatePickerRef.current) {
      try {
        hiddenDatePickerRef.current.showPicker();
      } catch {
        hiddenDatePickerRef.current.focus();
      }
    }
  };

  return (
    <div
      style={{
        position: 'relative',
        display: 'inline-flex',
        alignItems: 'center',
        width: '140px',
        ...style,
      }}
    >
      {/* Ô nhập text cho phép gõ định dạng dd/mm/yyyy */}
      <input
        type="text"
        value={textValue}
        onChange={handleTextChange}
        onBlur={handleBlur}
        placeholder={placeholder}
        title={title}
        disabled={disabled}
        maxLength={10}
        className={className || 'bookings-date-input'}
        style={{
          width: '100%',
          minWidth: 0,
          paddingRight: '2rem',
          borderColor: hasError ? 'var(--color-danger, #ef4444)' : undefined,
          boxShadow: hasError ? '0 0 0 2px rgba(239, 68, 68, 0.25)' : undefined,
          ...inputStyle,
        }}
      />

      {/* Input type="date" ẩn chuyên phục vụ mở bảng chọn lịch */}
      <input
        ref={hiddenDatePickerRef}
        type="date"
        value={value || ''}
        min={min}
        max={max}
        onChange={(e) => {
          const newIso = e.target.value;
          if (!newIso) {
            setTextValue('');
            setHasError(false);
            onChange('');
            return;
          }
          if (min && newIso < min) {
            showToast(`${label} không được nhỏ hơn ${isoToDisplay(min)}`, 'error');
            setHasError(true);
            return;
          }
          if (max && newIso > max) {
            showToast(`${label} không được lớn hơn ${isoToDisplay(max)}`, 'error');
            setHasError(true);
            return;
          }
          setHasError(false);
          setTextValue(isoToDisplay(newIso));
          onChange(newIso);
        }}
        disabled={disabled}
        style={{
          position: 'absolute',
          opacity: 0,
          pointerEvents: 'none',
          width: 0,
          height: 0,
          bottom: 0,
          right: 0,
        }}
        tabIndex={-1}
      />

      {/* Nút icon mở popup lịch */}
      <button
        type="button"
        onClick={handleOpenPicker}
        disabled={disabled}
        title="Bấm để mở lịch chọn ngày"
        style={{
          position: 'absolute',
          right: '0.45rem',
          height: '28px',
          width: '28px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: 'transparent',
          border: 'none',
          borderRadius: 'var(--radius-sm, 0.375rem)',
          color: hasError ? 'var(--color-danger, #ef4444)' : 'var(--color-text-muted)',
          cursor: disabled ? 'not-allowed' : 'pointer',
          padding: 0,
        }}
      >
        <Calendar size={15} />
      </button>
    </div>
  );
};

export default DateInputWithPicker;
