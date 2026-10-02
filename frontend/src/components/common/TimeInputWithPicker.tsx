import React, { useRef } from 'react';
import { Clock } from 'lucide-react';
import { isValidTimeFormat } from '../../utils/timeUtils';

/**
 * =========================================================================================
 * INTERFACE: TimeInputWithPickerProps
 * =========================================================================================
 * Định nghĩa các thuộc tính cho ô nhập giờ thông minh:
 * - label: Tiêu đề hiển thị phía trên ô nhập (ví dụ: "Giờ bắt đầu", "Giờ kết thúc").
 * - value: Giá trị giờ hiện tại dạng chuỗi "HH:mm" (ví dụ: "06:00").
 * - onChange: Hàm callback truyền ngược lại giá trị chuỗi đã gõ hoặc đã chọn cho component cha.
 * - placeholder: Văn bản gợi ý trong ô input (mặc định: "HH:mm (VD: 06:00)").
 * - disabled: Cờ vô hiệu hóa ô nhập khi đang gửi dữ liệu (submitting).
 */
export interface TimeInputWithPickerProps {
  label: string;
  value: string;
  onChange: (val: string) => void;
  placeholder?: string;
  disabled?: boolean;
  hasError?: boolean;
}

/**
 * =========================================================================================
 * COMPONENT: TimeInputWithPicker (Ô Nhập Giờ 2 Trong 1)
 * =========================================================================================
 * Giải quyết trải nghiệm người dùng cực kỳ linh hoạt:
 * 1. "Vừa cho nhập tay": Người dùng có thể gõ bàn phím bình thường.
 *    - CHẶN HOÀN TOÀN chữ cái (a-z, A-Z), chỉ cho phép gõ số (0-9) và dấu hai chấm (:).
 *    - Hỗ trợ người dùng gõ nhanh 4 chữ số: tự động chèn dấu ":" vào giữa (vd: "0600" -> "06:00").
 *    - GIỮ NGUYÊN giá trị người dùng nhập, tuyệt đối KHÔNG tự ý ép/sửa số sai khi blur
 *      (để hệ thống hiển thị thông báo lỗi rõ ràng theo ticket TC_TIMESLOT_28).
 * 2. "Vừa cho chọn đồng hồ": Có nút icon chiếc đồng hồ bên phải. Bấm vào sẽ mở popup
 *    chọn giờ gốc của trình duyệt (HTML5 Time Picker).
 */
export const TimeInputWithPicker: React.FC<TimeInputWithPickerProps> = ({
  label,
  value,
  onChange,
  placeholder = 'HH:mm (VD: 06:00)',
  disabled = false,
  hasError = false,
}) => {
  // Tham chiếu đến thẻ input type="time" ẩn, dùng để kích hoạt popup đồng hồ
  const hiddenTimePickerRef = useRef<HTMLInputElement>(null);

  /**
   * Hàm xử lý khi người dùng gõ trực tiếp từ bàn phím:
   * - Dùng regex loại bỏ ngay lập tức bất kỳ ký tự nào không phải là số (0-9) hoặc dấu hai chấm (:).
   * - Hỗ trợ người dùng gõ nhanh 4 chữ số: tự động chèn dấu ":" vào giữa.
   * - Giới hạn tối đa 5 ký tự (chuẩn độ dài "HH:mm").
   * - Không tự ý can thiệp hay ép số sai, giữ nguyên chuỗi người dùng nhập.
   */
  const handleTextChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    // Lọc bỏ chữ cái và ký tự đặc biệt
    let raw = e.target.value.replace(/[^0-9:]/g, '');

    // Nếu gõ 4 số liên tiếp không có dấu hai chấm (ví dụ "0600"), tự động đổi thành "06:00"
    if (/^\d{4}$/.test(raw) && !raw.includes(':')) {
      raw = `${raw.slice(0, 2)}:${raw.slice(2)}`;
    }

    // Cắt ngắn nếu dài quá 5 ký tự
    if (raw.length > 5) {
      raw = raw.slice(0, 5);
    }

    onChange(raw);
  };

  /**
   * Hàm mở bảng chọn giờ gốc của trình duyệt khi bấm vào nút icon chiếc đồng hồ:
   * - Sử dụng API hiện đại `showPicker()` của thẻ HTML5 input.
   * - Nếu trình duyệt cũ chưa hỗ trợ showPicker thì fallback sang lệnh `focus()`.
   */
  const handleOpenPicker = () => {
    if (disabled) return;
    if (hiddenTimePickerRef.current) {
      try {
        hiddenTimePickerRef.current.showPicker();
      } catch {
        hiddenTimePickerRef.current.focus();
      }
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column' }}>
      {/* Tiêu đề nhãn của ô nhập */}
      <label className="block text-sm font-semibold mb-2">{label}</label>

      {/* Vùng chứa ô nhập text và nút icon đồng hồ lồng bên trong */}
      <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
        {/* Ô nhập văn bản hỗ trợ gõ tay */}
        <input
          type="text"
          placeholder={placeholder}
          value={value}
          onChange={handleTextChange}
          disabled={disabled}
          maxLength={5}
          style={{
            height: '46px',
            padding: '0.65rem 2.85rem 0.65rem 1rem', // Chừa 2.85rem bên phải để không bị chữ đè lên icon
            border: hasError ? '1.5px solid var(--color-danger, #ef4444)' : '1px solid var(--color-border)',
            borderRadius: 'var(--radius-md)',
            background: 'var(--color-bg-base)',
            color: 'var(--color-text-base)',
            fontFamily: 'inherit',
            fontSize: '0.95rem',
            width: '100%',
            outline: 'none',
          }}
        />

        {/* Input ẩn type="time" chuyên phục vụ gọi hàm showPicker() */}
        <input
          ref={hiddenTimePickerRef}
          type="time"
          value={isValidTimeFormat(value) ? value : ''}
          onChange={(e) => onChange(e.target.value)}
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

        {/* Nút icon chiếc đồng hồ đặt ở góc phải ô nhập */}
        <button
          type="button"
          onClick={handleOpenPicker}
          disabled={disabled}
          title="Bấm để chọn giờ từ đồng hồ"
          style={{
            position: 'absolute',
            right: '0.4rem',
            height: '36px',
            width: '36px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: 'transparent',
            border: 'none',
            borderRadius: 'var(--radius-md)',
            color: 'var(--color-text-muted)',
            cursor: disabled ? 'not-allowed' : 'pointer',
            transition: 'all 0.15s ease',
          }}
          className="hover:text-primary hover:bg-[var(--color-bg-surface)]"
        >
          <Clock size={19} />
        </button>
      </div>
    </div>
  );
};

export default TimeInputWithPicker;
