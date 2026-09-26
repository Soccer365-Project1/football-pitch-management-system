import React, { useState, useEffect, useCallback, useRef } from 'react';
import { createPortal } from 'react-dom';
import { Plus, Edit, Trash2, X, Clock, Loader2, AlertCircle, Check, Zap } from 'lucide-react';
import { timeslotService } from '../../services/timeslotService';
import type { TimeSlot, TimeSlotRequest } from '../../types/timeslot';
import { showToast, showAlert } from '../../utils/toast';

/**
 * =========================================================================================
 * HÀM HELPER TOÀN CỤC: Kiểm tra định dạng thời gian 24h chuẩn (HH:mm)
 * =========================================================================================
 * @param time Chuỗi thời gian cần kiểm tra (ví dụ: "06:00", "17:30")
 * @returns true nếu đúng chuẩn từ "00:00" đến "23:59", ngược lại false
 */
const isValidTimeFormat = (time: string): boolean => {
  if (!time) return false;
  return /^([01]\d|2[0-3]):([0-5]\d)$/.test(time.trim());
};

/**
 * =========================================================================================
 * COMPONENT PHỤ TRỢ 1: ModalOverlay (Hộp thoại tương tác nổi)
 * =========================================================================================
 * Sử dụng kỹ thuật React Portal (`createPortal`) để đưa cây DOM của Modal ra trực tiếp thẻ `document.body`.
 * Mục đích:
 * 1. Đảm bảo Modal không bao giờ bị cắt xén (overflow clipping) bởi các container cha có `overflow: hidden`.
 * 2. Phủ lớp nền đen mờ (Backdrop: rgba(0,0,0,0.55)) hỗ trợ đóng modal khi bấm ra ngoài.
 * 3. Hộp thoại con ở trung tâm được thiết kế rộng rãi, thoáng đãng (maxWidth: 540px, padding: 1.75rem),
 *    giúp các trường nhập liệu và nút bấm có khoảng cách rộng rãi, không bị dính sát nhau.
 */
interface ModalOverlayProps {
  children: React.ReactNode;
  onClose: () => void;
}

const ModalOverlay: React.FC<ModalOverlayProps> = ({ children, onClose }) => {
  return createPortal(
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.55)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 9999,
        padding: '1.25rem',
      }}
    >
      {/* Vùng nhấn backdrop trong suốt bao phủ toàn màn hình để đóng modal */}
      <div
        style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0 }}
        onClick={onClose}
      />

      {/* Khung chứa nội dung chính của Modal (Card nổi sang trọng) */}
      <div
        className="card"
        style={{
          position: 'relative',
          zIndex: 10000,
          width: '100%',
          maxWidth: '540px',
          maxHeight: '90vh',
          display: 'flex',
          flexDirection: 'column',
          transform: 'none', // Vô hiệu hóa hiệu ứng hover nhấc card của CSS toàn cục
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.2), 0 10px 10px -5px rgba(0, 0, 0, 0.1)',
          padding: '1.75rem',
          borderRadius: '16px',
        }}
      >
        {children}
      </div>
    </div>,
    document.body
  );
};

/**
 * =========================================================================================
 * COMPONENT PHỤ TRỢ 2: TimeInputWithPicker (Ô nhập giờ kết hợp: Vừa gõ phím vừa chọn đồng hồ)
 * =========================================================================================
 * Giải quyết dứt điểm nhu cầu nhập liệu linh hoạt của người dùng:
 * 1. "Vừa cho nhập": Người dùng có thể click chuột vào ô text và gõ trực tiếp bằng bàn phím.
 *    - CHẶN HOÀN TOÀN chữ cái (a-z, A-Z), chỉ cho phép gõ số (0-9) và dấu hai chấm (:).
 *    - Tự động chuẩn hóa khi rời ô (blur) hoặc gõ liền 4 số (VD: gõ "1730" -> tự thành "17:30").
 * 2. "Vừa cho chọn": Nút icon đồng hồ ở góc phải cho phép click để mở popup chọn giờ trực quan (showPicker)
 *    như trước mà không bị hiện tượng ép mở popup khi người dùng chỉ muốn click để gõ phím.
 */
interface TimeInputWithPickerProps {
  label: string;
  value: string;
  onChange: (val: string) => void;
  placeholder?: string;
}

const TimeInputWithPicker: React.FC<TimeInputWithPickerProps> = ({
  label,
  value,
  onChange,
  placeholder = 'HH:mm (VD: 06:00)',
}) => {
  /** Tham chiếu đến thẻ input type="time" ẩn dùng để kích hoạt bộ chọn đồng hồ của trình duyệt */
  const hiddenTimePickerRef = useRef<HTMLInputElement>(null);

  /**
   * Xử lý khi người dùng gõ trực tiếp bằng bàn phím:
   * - LỌC BỎ NGAY LẬP TỨC các ký tự không phải số hoặc dấu : (chặn gõ chữ cái)
   * - Tự động chèn dấu ":" nếu người dùng gõ 4 chữ số liên tiếp
   * - Giới hạn tối đa 5 ký tự ("HH:mm")
   */
  const handleTextChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    // Chỉ giữ lại chữ số và dấu hai chấm, chặn hoàn toàn chữ cái như "dsdfd"
    let raw = e.target.value.replace(/[^0-9:]/g, '');

    // Nếu gõ 4 chữ số liền nhau (VD: "0600", "1730"), tự động chèn dấu ":"
    if (/^\d{4}$/.test(raw) && !raw.includes(':')) {
      raw = `${raw.slice(0, 2)}:${raw.slice(2)}`;
    }

    if (raw.length > 5) {
      raw = raw.slice(0, 5);
    }

    onChange(raw);
  };

  /**
   * Tự động chuẩn hóa định dạng thời gian khi người dùng rời khỏi ô nhập (onBlur)
   * Ví dụ: Người dùng gõ "6" -> "06:00", "17" -> "17:00", "6:30" -> "06:30"
   */
  const handleBlur = () => {
    if (!value) return;
    const trimmed = value.trim();

    // Trường hợp chỉ gõ 1 hoặc 2 chữ số (VD "6" -> "06:00")
    if (/^\d{1,2}$/.test(trimmed)) {
      const h = Math.min(23, Math.max(0, parseInt(trimmed, 10)));
      onChange(`${h.toString().padStart(2, '0')}:00`);
      return;
    }

    // Trường hợp gõ dạng "6:3" hoặc "6:30"
    const parts = trimmed.split(':');
    if (parts.length === 2) {
      let h = parseInt(parts[0], 10);
      let m = parseInt(parts[1], 10);
      if (!isNaN(h) && !isNaN(m)) {
        h = Math.min(23, Math.max(0, h));
        m = Math.min(59, Math.max(0, m));
        const formatted = `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}`;
        onChange(formatted);
      }
    }
  };

  /**
   * Mở bộ chọn giờ (Time Picker popup) khi người dùng bấm vào icon đồng hồ
   */
  const handleOpenPicker = () => {
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
      <label className="block text-sm font-semibold mb-2">{label}</label>
      <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
        {/* Ô nhập văn bản hỗ trợ gõ tay 24 giờ trực tiếp (đã lọc chỉ nhận số và :) */}
        <input
          type="text"
          placeholder={placeholder}
          value={value}
          onChange={handleTextChange}
          onBlur={handleBlur}
          maxLength={5}
          style={{
            height: '46px',
            padding: '0.65rem 2.85rem 0.65rem 1rem', // Chừa khoảng trống bên phải cho nút icon đồng hồ
            border: '1px solid var(--color-border)',
            borderRadius: 'var(--radius-md)',
            background: 'var(--color-bg-base)',
            color: 'var(--color-text-base)',
            fontFamily: 'inherit',
            fontSize: '0.95rem',
            width: '100%',
            outline: 'none',
          }}
        />

        {/* Input ẩn type="time" để phục vụ kích hoạt showPicker() */}
        <input
          ref={hiddenTimePickerRef}
          type="time"
          value={isValidTimeFormat(value) ? value : ''}
          onChange={(e) => onChange(e.target.value)}
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

        {/* Nút icon đồng hồ: Bấm vào để mở bảng chọn giờ như hiện tại */}
        <button
          type="button"
          onClick={handleOpenPicker}
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
            cursor: 'pointer',
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

/**
 * =========================================================================================
 * COMPONENT CHÍNH: AdminTimeSlots (Màn hình Quản trị Khung giờ)
 * =========================================================================================
 * Đáp ứng đầy đủ các tiêu chuẩn nghiệp vụ và kỹ thuật theo đặc tả SRS Mục 3.4.15 (Hình 3.62 - 3.68):
 * 1. Bảng danh sách ca đá sắp xếp theo giờ bắt đầu tăng dần, hiển thị đơn giá thật từ CSDL qua API.
 * 2. Header được thiết kế chuẩn: Tiêu đề bên trái, Nút thêm bên phải gọn gàng, không bị tràn dài 100%.
 * 3. Popup Modal có khoảng cách (spacing) rộng rãi, các ô nhập liệu, ô phân loại và cặp nút bấm
 *    hành động đều có kích thước đối xứng, hoàn toàn bằng nhau (50% - 50%).
 * 4. Không bị lỗi hiển thị trùng lặp: Thông báo lỗi chỉ hiển thị 1 vị trí tập trung; dòng hướng dẫn thời lượng
 *    chỉ hiển thị khi cả 2 ô giờ đã nhập đúng chuẩn HH:mm hợp lệ.
 * 5. Hỗ trợ nhập liệu 2 chiều: Vừa cho phép gõ tay bàn phím 24h, vừa có icon đồng hồ để mở bảng chọn giờ.
 * 6. Ràng buộc thời lượng ca đá bắt buộc từ 1 giờ (60 phút) đến 2 giờ (120 phút).
 * 7. Xử lý xóa an toàn: Tự động cập nhật Optimistic UI loại bỏ ngay lập tức ca đá bị xóa.
 */
const AdminTimeSlots: React.FC = () => {
  // =======================================================================================
  // 1. Quản lý trạng thái Dữ liệu (State Management)
  // =======================================================================================
  /** Danh sách khung giờ hiển thị trong bảng (kèm theo đơn giá thật từ API) */
  const [timeSlots, setTimeSlots] = useState<TimeSlot[]>([]);

  /** Trạng thái cờ báo đang tải dữ liệu danh sách khung giờ từ máy chủ */
  const [loading, setLoading] = useState<boolean>(true);

  /** Trạng thái cờ báo đang thực hiện thao tác gửi request (Thêm / Sửa / Xóa) để disable nút bấm */
  const [submitting, setSubmitting] = useState<boolean>(false);

  // =======================================================================================
  // 2. Quản lý trạng thái Mở/Đóng Modal
  // =======================================================================================
  /** Trạng thái mở/đóng Modal "Thêm Khung Giờ Mới" */
  const [isAddModalOpen, setIsAddModalOpen] = useState<boolean>(false);

  /** Đối tượng khung giờ đang được chọn để Chỉnh sửa (null khi không mở modal sửa) */
  const [editSlot, setEditSlot] = useState<TimeSlot | null>(null);

  /** Đối tượng khung giờ đang được chọn để Xóa (null khi không mở modal xóa) */
  const [deleteSlot, setDeleteSlot] = useState<TimeSlot | null>(null);

  // =======================================================================================
  // 3. Quản lý dữ liệu Form Thêm mới Khung giờ
  // =======================================================================================
  /** Giờ bắt đầu cho ca mới (định dạng "HH:mm") */
  const [addStartTime, setAddStartTime] = useState<string>('');

  /** Giờ kết thúc cho ca mới (định dạng "HH:mm") */
  const [addEndTime, setAddEndTime] = useState<string>('');

  /** Phân loại ca mới: 'NORMAL' (Giờ thường) hoặc 'PEAK' (Giờ vàng) */
  const [addType, setAddType] = useState<'NORMAL' | 'PEAK'>('NORMAL');

  /** Thông báo lỗi hiển thị inline trong Form Thêm mới */
  const [addError, setAddError] = useState<string | null>(null);

  // =======================================================================================
  // 4. Quản lý dữ liệu Form Chỉnh sửa Khung giờ
  // =======================================================================================
  /** Giờ bắt đầu khi chỉnh sửa (định dạng "HH:mm") */
  const [editStartTime, setEditStartTime] = useState<string>('');

  /** Giờ kết thúc khi chỉnh sửa (định dạng "HH:mm") */
  const [editEndTime, setEditEndTime] = useState<string>('');

  /** Phân loại khi chỉnh sửa: 'NORMAL' (Giờ thường) hoặc 'PEAK' (Giờ vàng) */
  const [editType, setEditType] = useState<'NORMAL' | 'PEAK'>('NORMAL');

  /** Thông báo lỗi hiển thị inline trong Form Chỉnh sửa */
  const [editError, setEditError] = useState<string | null>(null);

  // =======================================================================================
  // 5. Hàm Helper Tính Toán Thời Lượng Ca Đá (Phút)
  // =======================================================================================
  /**
   * Tính chênh lệch thời gian giữa Giờ bắt đầu và Giờ kết thúc thành đơn vị phút
   * Chỉ tính toán khi cả hai giờ đều đúng định dạng HH:mm hợp lệ
   */
  const calculateDurationMinutes = (start: string, end: string): number => {
    if (!start || !end || !isValidTimeFormat(start) || !isValidTimeFormat(end)) return 0;
    const [startH, startM] = start.split(':').map(Number);
    const [endH, endM] = end.split(':').map(Number);
    return endH * 60 + endM - (startH * 60 + startM);
  };

  // =======================================================================================
  // 6. Logic Tải Dữ Liệu Danh Sách Khung Giờ (API Backend Thật)
  // =======================================================================================
  /**
   * Gọi API Backend: `GET /api/v1/admin/time-slots?activeOnly=true`
   * - Mặc định chỉ lấy các khung giờ đang hoạt động (`isActive = true`), loại bỏ các ca đã xóa mềm.
   * - Lọc bổ sung phía client (`slot.isActive !== false`) để đảm bảo tính an toàn dữ liệu tuyệt đối.
   */
  const loadTimeSlots = useCallback(async () => {
    setLoading(true);
    try {
      const data = await timeslotService.getTimeSlots(true);
      const activeSlots = (data || []).filter((slot) => slot.isActive !== false);
      setTimeSlots(activeSlots);
    } catch (err: any) {
      const errorMsg =
        err.response?.data?.message || 'Không thể tải danh sách khung giờ từ máy chủ. Vui lòng thử lại!';
      showToast(errorMsg, 'error');
      setTimeSlots([]);
    } finally {
      setLoading(false);
    }
  }, []);

  /** Tự động gọi API tải danh sách khung giờ khi component được mount lần đầu */
  useEffect(() => {
    loadTimeSlots();
  }, [loadTimeSlots]);

  // =======================================================================================
  // 7. Đồng Bộ Dữ Liệu Vào Form Chỉnh Sửa
  // =======================================================================================
  /** Khi người dùng click nút Sửa ca đá, điền sẵn dữ liệu hiện tại vào form chỉnh sửa */
  useEffect(() => {
    if (editSlot) {
      setEditStartTime(editSlot.startTime || '');
      setEditEndTime(editSlot.endTime || '');
      setEditType(editSlot.isPeakHour ? 'PEAK' : 'NORMAL');
      setEditError(null);
    }
  }, [editSlot]);

  // =======================================================================================
  // 8. Khởi Tạo Form Thêm Mới
  // =======================================================================================
  /** Mở Modal Thêm mới và đặt lại các trường nhập liệu về giá trị mặc định */
  const handleOpenAddModal = () => {
    setAddStartTime('');
    setAddEndTime('');
    setAddType('NORMAL');
    setAddError(null);
    setIsAddModalOpen(true);
  };

  // =======================================================================================
  // 9. Xử Lý Submit Thêm Mới Khung Giờ (Create TimeSlot)
  // =======================================================================================
  const handleAddSubmit = async () => {
    setAddError(null);

    // Ca biên 1: Không được để trống bất kỳ trường giờ nào
    if (!addStartTime.trim() || !addEndTime.trim()) {
      setAddError('Vui lòng nhập đầy đủ giờ bắt đầu và giờ kết thúc!');
      return;
    }

    // Ca biên 2: Định dạng giờ phải đúng chuẩn HH:mm (00:00 - 23:59)
    if (!isValidTimeFormat(addStartTime) || !isValidTimeFormat(addEndTime)) {
      setAddError('Định dạng giờ không hợp lệ! Vui lòng nhập định dạng HH:mm (ví dụ: 06:00, 17:30).');
      return;
    }

    // Ca biên 3: Giờ kết thúc phải lớn hơn giờ bắt đầu
    if (addEndTime <= addStartTime) {
      setAddError('Giờ kết thúc phải lớn hơn giờ bắt đầu!');
      return;
    }

    // Ca biên 4: Ràng buộc nghiệp vụ thời lượng ca đá phải từ 1h đến 2h (60p - 120p)
    const duration = calculateDurationMinutes(addStartTime, addEndTime);
    if (duration < 60 || duration > 120) {
      setAddError(
        `Thời lượng ca đá hiện tại là ${duration} phút. Quy định ca đá phải từ 1 giờ (60 phút) đến 2 giờ (120 phút)!`
      );
      return;
    }

    setSubmitting(true);
    const payload: TimeSlotRequest = {
      startTime: addStartTime,
      endTime: addEndTime,
      isPeakHour: addType === 'PEAK',
    };

    try {
      await timeslotService.createTimeSlot(payload);
      showToast('Thêm khung giờ mới thành công!', 'success');
      setIsAddModalOpen(false);
      await loadTimeSlots();
    } catch (err: any) {
      // Xử lý mã lỗi từ Backend (3013: Chồng chéo ca đá; 3015: Sai thời lượng)
      const errorMsg =
        err.response?.data?.message ||
        'Không thể thêm khung giờ. Vui lòng kiểm tra lại thời gian (tránh chồng chéo với ca khác)!';
      setAddError(errorMsg);
    } finally {
      setSubmitting(false);
    }
  };

  // =======================================================================================
  // 10. Xử Lý Submit Chỉnh Sửa Khung Giờ (Update TimeSlot)
  // =======================================================================================
  const handleEditSubmit = async () => {
    if (!editSlot) return;
    setEditError(null);

    // Ca biên 1: Không được để trống
    if (!editStartTime.trim() || !editEndTime.trim()) {
      setEditError('Vui lòng nhập đầy đủ giờ bắt đầu và giờ kết thúc!');
      return;
    }

    // Ca biên 2: Định dạng giờ phải đúng chuẩn HH:mm
    if (!isValidTimeFormat(editStartTime) || !isValidTimeFormat(editEndTime)) {
      setEditError('Định dạng giờ không hợp lệ! Vui lòng nhập định dạng HH:mm (ví dụ: 06:00, 17:30).');
      return;
    }

    // Ca biên 3: Giờ kết thúc phải lớn hơn giờ bắt đầu
    if (editEndTime <= editStartTime) {
      setEditError('Giờ kết thúc phải lớn hơn giờ bắt đầu!');
      return;
    }

    // Ca biên 4: Ràng buộc thời lượng 1h - 2h (60p - 120p)
    const duration = calculateDurationMinutes(editStartTime, editEndTime);
    if (duration < 60 || duration > 120) {
      setEditError(
        `Thời lượng ca đá hiện tại là ${duration} phút. Quy định ca đá phải từ 1 giờ (60 phút) đến 2 giờ (120 phút)!`
      );
      return;
    }

    setSubmitting(true);
    const payload: TimeSlotRequest = {
      startTime: editStartTime,
      endTime: editEndTime,
      isPeakHour: editType === 'PEAK',
    };

    try {
      await timeslotService.updateTimeSlot(editSlot.id, payload);
      showToast('Cập nhật khung giờ thành công!', 'success');
      setEditSlot(null);
      await loadTimeSlots();
    } catch (err: any) {
      const errorMsg =
        err.response?.data?.message || 'Không thể cập nhật khung giờ. Vui lòng kiểm tra lại thời gian!';
      setEditError(errorMsg);
    } finally {
      setSubmitting(false);
    }
  };

  // =======================================================================================
  // 11. Xử Lý Xóa Khung Giờ An Toàn & Optimistic Update (Delete TimeSlot)
  // =======================================================================================
  const handleDeleteConfirm = async () => {
    if (!deleteSlot) return;

    setSubmitting(true);
    const deletedId = deleteSlot.id;

    try {
      await timeslotService.deleteTimeSlot(deletedId);

      // Optimistic Update: Lập tức loại bỏ ca đá khỏi danh sách giao diện
      setTimeSlots((prev) => prev.filter((slot) => slot.id !== deletedId));

      showToast('Xóa khung giờ thành công!', 'success');
      setDeleteSlot(null);

      // Nạp lại danh sách mới từ Backend để đảm bảo đồng bộ 100% CSDL
      await loadTimeSlots();
    } catch (err: any) {
      setDeleteSlot(null);
      // Xử lý mã lỗi 3014: Khung giờ đang có đơn đặt lịch trong tương lai chưa hoàn tất
      const errorMsg =
        err.response?.data?.message ||
        'Không thể xóa khung giờ do đang có khách hàng đặt lịch trong những ngày sắp tới!';
      showAlert('Không thể xóa khung giờ', errorMsg, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  // Tính thời lượng tức thì cho badge preview khi cả 2 giờ đều đúng chuẩn HH:mm
  const isAddBothTimeValid = isValidTimeFormat(addStartTime) && isValidTimeFormat(addEndTime);
  const addDuration = isAddBothTimeValid ? calculateDurationMinutes(addStartTime, addEndTime) : 0;

  const isEditBothTimeValid = isValidTimeFormat(editStartTime) && isValidTimeFormat(editEndTime);
  const editDuration = isEditBothTimeValid ? calculateDurationMinutes(editStartTime, editEndTime) : 0;

  return (
    // Khung bao ngoài chính: Áp dụng class .admin-timeslots-container (hỗ trợ responsive chiều cao)
    <div className="w-full admin-timeslots-container">
      {/* Khung Card chính: Bo viền, đổ bóng, padding co giãn (p-4 sm:p-6), chứa tiêu đề, bảng ca đá */}
      <div
        className="card p-4 sm:p-6"
        style={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
          transform: 'none', // Vô hiệu hóa hiệu ứng hover nhấc card của CSS gốc
        }}
      >
        {/* ===============================================================================
            11.1. HEADER QUẢN TRỊ TRÊN CÙNG (.admin-timeslots-header)
            - Desktop: Tiêu đề bên trái, nút "+ Thêm khung giờ mới" bên phải gọn gàng, không bị kéo dài full màn hình
            - Mobile (<= 640px): Tự động chuyển dạng cột, nút bấm tự động co dãn vừa vặn
        =============================================================================== */}
        <div className="admin-timeslots-header">
          <div>
            <h2 className="text-xl sm:text-2xl font-bold tracking-tight">Danh sách Khung giờ</h2>
            <p className="text-sm text-muted mt-1">
              Quản lý các ca đá cố định trong ngày (thời lượng 1h - 2h), phân định Giờ thường và Giờ vàng cao điểm
            </p>
          </div>
          <button
            className="btn btn-primary shadow-sm hover:shadow transition-all"
            style={{
              height: '42px',
              padding: '0 1.25rem',
              whiteSpace: 'nowrap',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '0.5rem',
              fontWeight: 600,
            }}
            onClick={handleOpenAddModal}
          >
            <Plus size={18} /> Thêm khung giờ mới
          </button>
        </div>

        {/* ===============================================================================
            11.2. BẢNG DỮ LIỆU KHUNG GIỜ (TABLE CÓ CUỘN NGANG & DỌC MƯỢT MÀ)
            - Hỗ trợ Sticky Header cố định khi cuộn danh sách dài
            - Thiết lập `minWidth: 650px` để các cột không bị ép méo vỡ chữ trên màn hình điện thoại
        =============================================================================== */}
        <div
          style={{
            flex: 1,
            overflowY: 'auto',
            overflowX: 'auto',
            position: 'relative',
          }}
        >
          {loading ? (
            /* Trạng thái đang tải dữ liệu */
            <div
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                height: '100%',
                minHeight: '260px',
                gap: '1rem',
                color: 'var(--color-text-muted)',
              }}
            >
              <Loader2 size={32} className="animate-spin text-primary" />
              <span>Đang tải danh sách khung giờ từ máy chủ...</span>
            </div>
          ) : timeSlots.length === 0 ? (
            /* Trạng thái danh sách rỗng */
            <div
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                height: '100%',
                minHeight: '260px',
                gap: '0.75rem',
                color: 'var(--color-text-muted)',
              }}
            >
              <Clock size={40} className="text-muted" />
              <p className="font-semibold">Chưa có khung giờ nào trong hệ thống</p>
              <p className="text-sm">Bấm "Thêm khung giờ mới" ở trên để tạo ca đá đầu tiên.</p>
            </div>
          ) : (
            /* Bảng hiển thị danh sách các khung giờ */
            <table
              style={{
                width: '100%',
                minWidth: '650px',
                borderCollapse: 'collapse',
                textAlign: 'left',
              }}
            >
              <thead
                style={{
                  position: 'sticky',
                  top: 0,
                  backgroundColor: 'var(--color-bg-surface)',
                  zIndex: 10,
                }}
              >
                <tr style={{ borderBottom: '1px solid var(--color-border)' }}>
                  <th className="p-4 font-semibold text-muted text-sm" style={{ width: '22%' }}>
                    KHUNG GIỜ
                  </th>
                  <th className="p-4 font-semibold text-muted text-sm" style={{ width: '20%' }}>
                    PHÂN LOẠI
                  </th>
                  <th className="p-4 font-semibold text-muted text-sm" style={{ width: '20%' }}>
                    GIÁ SÂN 5 (VNĐ)
                  </th>
                  <th className="p-4 font-semibold text-muted text-sm" style={{ width: '20%' }}>
                    GIÁ SÂN 7 (VNĐ)
                  </th>
                  <th className="p-4 font-semibold text-muted text-sm text-left" style={{ width: '18%' }}>
                    THAO TÁC
                  </th>
                </tr>
              </thead>
              <tbody>
                {timeSlots.map((slot) => {
                  return (
                    <tr
                      key={slot.id}
                      style={{ borderBottom: '1px solid var(--color-border)' }}
                      className="hover:bg-[var(--color-bg-base)] transition-colors"
                    >
                      {/* Cột 1: Thời gian bắt đầu - kết thúc */}
                      <td className="p-4 font-semibold text-base sm:text-lg">
                        {slot.startTime} - {slot.endTime}
                      </td>

                      {/* Cột 2: Huy hiệu phân loại Giờ vàng hoặc Giờ thường */}
                      <td className="p-4">
                        {slot.isPeakHour ? (
                          <span
                            className="badge badge-warning"
                            style={{
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '0.25rem',
                              padding: '0.35rem 0.65rem',
                              fontSize: '0.8rem',
                            }}
                          >
                            <Zap size={13} className="fill-current" /> Giờ vàng
                          </span>
                        ) : (
                          <span
                            className="badge badge-success"
                            style={{
                              backgroundColor: 'rgba(16, 185, 129, 0.12)',
                              color: 'var(--color-primary)',
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '0.25rem',
                              padding: '0.35rem 0.65rem',
                              fontSize: '0.8rem',
                            }}
                          >
                            Giờ thường
                          </span>
                        )}
                      </td>

                      {/* Cột 3: Đơn giá Sân 5 thật từ Backend API (0 đ nếu chưa có cấu hình) */}
                      <td className="p-4 font-semibold text-muted">
                        {slot.pricePitch5 != null && slot.pricePitch5 > 0
                          ? `${new Intl.NumberFormat('vi-VN').format(slot.pricePitch5)} đ`
                          : '0 đ'}
                      </td>

                      {/* Cột 4: Đơn giá Sân 7 thật từ Backend API (0 đ nếu chưa có cấu hình) */}
                      <td className="p-4 font-semibold text-muted">
                        {slot.pricePitch7 != null && slot.pricePitch7 > 0
                          ? `${new Intl.NumberFormat('vi-VN').format(slot.pricePitch7)} đ`
                          : '0 đ'}
                      </td>

                      {/* Cột 5: Nút bấm Sửa và Xóa */}
                      <td className="p-4 text-left">
                        <div className="flex gap-2 justify-start items-center">
                          {/* Nút Chỉnh sửa */}
                          <button
                            className="btn btn-secondary"
                            style={{
                              padding: '0.5rem',
                              borderRadius: 'var(--radius-md)',
                              transition: 'all 0.15s ease',
                            }}
                            title="Chỉnh sửa ca đá"
                            onClick={() => setEditSlot(slot)}
                          >
                            <Edit size={16} />
                          </button>

                          {/* Nút Xóa ca đá */}
                          <button
                            className="btn btn-secondary text-danger hover:bg-red-500/10"
                            style={{
                              padding: '0.5rem',
                              borderRadius: 'var(--radius-md)',
                              transition: 'all 0.15s ease',
                            }}
                            title="Xóa ca đá"
                            onClick={() => setDeleteSlot(slot)}
                          >
                            <Trash2 size={16} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* ===================================================================================
          12. MODAL THÊM MỚI KHUNG GIỜ (ADD TIME SLOT MODAL)
          - Spacing rộng rãi (gap: 1.5rem), các trường có khoảng cách thoáng mắt
          - Các ô nhập liệu vừa cho gõ bàn phím (đã chặn chữ) vừa cho bấm chọn từ đồng hồ
          - Dòng thời lượng chỉ xuất hiện khi cả 2 giờ đều đúng định dạng hợp lệ
          - Các ô phân loại và cặp nút bấm hoàn toàn bằng nhau (50% - 50%)
      =================================================================================== */}
      {isAddModalOpen && (
        <ModalOverlay onClose={() => !submitting && setIsAddModalOpen(false)}>
          {/* Header của Modal */}
          <div className="flex justify-between items-center mb-6 shrink-0">
            <div>
              <h2 className="text-xl font-bold">Thêm Khung Giờ Mới</h2>
              <p className="text-sm text-muted mt-1">Thiết lập khoảng giờ và loại giá áp dụng</p>
            </div>
            <button
              onClick={() => !submitting && setIsAddModalOpen(false)}
              className="p-1.5 rounded-lg text-muted hover:text-[var(--color-text-base)] hover:bg-[var(--color-bg-base)] transition-colors"
              disabled={submitting}
              title="Đóng hộp thoại"
            >
              <X size={20} />
            </button>
          </div>

          {/* Hộp thông báo lỗi duy nhất trên đỉnh khi submit bị vi phạm */}
          {addError && (
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
              <span>{addError}</span>
            </div>
          )}

          {/* Form nội dung nhập liệu: Khoảng cách giữa các khối là 1.5rem (24px) thoáng đãng */}
          <div
            style={{
              overflowY: 'auto',
              flex: 1,
              display: 'flex',
              flexDirection: 'column',
              gap: '1.5rem',
              paddingRight: '0.25rem',
            }}
          >
            {/* KHỐI 1: Cặp ô chọn giờ bắt đầu & giờ kết thúc BẰNG NHAU (50% - 50%) - HỖ TRỢ VỪA GÕ VỪA CHỌN */}
            <div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <TimeInputWithPicker
                  label="Giờ bắt đầu"
                  value={addStartTime}
                  onChange={(val) => {
                    setAddStartTime(val);
                    if (addError) setAddError(null); // Tự động xóa banner lỗi trên đỉnh khi người dùng sửa
                  }}
                  placeholder="HH:mm (VD: 06:00)"
                />

                <TimeInputWithPicker
                  label="Giờ kết thúc"
                  value={addEndTime}
                  onChange={(val) => {
                    setAddEndTime(val);
                    if (addError) setAddError(null); // Tự động xóa banner lỗi trên đỉnh khi người dùng sửa
                  }}
                  placeholder="HH:mm (VD: 07:30)"
                />
              </div>

              {/* Dòng hướng dẫn thời lượng: CHỈ HIỂN THỊ KHI CẢ 2 GIỜ ĐÃ NHẬP ĐÚNG CHUẨN ĐỊNH DẠNG HH:mm */}
              {isAddBothTimeValid && (
                <div
                  style={{
                    marginTop: '0.75rem',
                    fontSize: '0.85rem',
                    fontWeight: 500,
                    padding: '0.65rem 0.85rem',
                    borderRadius: 'var(--radius-md)',
                    backgroundColor:
                      addEndTime <= addStartTime
                        ? 'rgba(239, 68, 68, 0.1)'
                        : addDuration >= 60 && addDuration <= 120
                        ? 'rgba(16, 185, 129, 0.1)'
                        : 'rgba(245, 158, 11, 0.1)',
                    color:
                      addEndTime <= addStartTime
                        ? 'var(--color-danger)'
                        : addDuration >= 60 && addDuration <= 120
                        ? 'var(--color-primary)'
                        : '#d97706',
                    border: `1px solid ${
                      addEndTime <= addStartTime
                        ? 'rgba(239, 68, 68, 0.3)'
                        : addDuration >= 60 && addDuration <= 120
                        ? 'rgba(16, 185, 129, 0.3)'
                        : 'rgba(245, 158, 11, 0.3)'
                    }`,
                  }}
                >
                  {addEndTime <= addStartTime ? (
                    '⚠ Giờ kết thúc phải lớn hơn giờ bắt đầu!'
                  ) : addDuration >= 60 && addDuration <= 120 ? (
                    `✓ Thời lượng ca: ${addDuration} phút (Hợp lệ: từ 1h đến 2h)`
                  ) : (
                    `⚠ Thời lượng ca: ${addDuration} phút (Yêu cầu quy định: từ 1h đến 2h)`
                  )}
                </div>
              )}
            </div>

            {/* KHỐI 2: Bộ chọn phân loại Khung giờ - 2 Ô BẰNG NHAU (50% - 50%), ĐỐI XỨNG TUYỆT ĐỐI */}
            <div>
              <label className="block text-sm font-semibold mb-2">Phân loại khung giờ</label>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                {/* Ô Lựa chọn 1: Giờ thường */}
                <button
                  type="button"
                  onClick={() => setAddType('NORMAL')}
                  style={{
                    minHeight: '84px',
                    padding: '0.85rem 1rem',
                    borderRadius: 'var(--radius-md)',
                    border:
                      addType === 'NORMAL'
                        ? '2px solid var(--color-primary)'
                        : '1px solid var(--color-border)',
                    backgroundColor:
                      addType === 'NORMAL'
                        ? 'rgba(16, 185, 129, 0.08)'
                        : 'var(--color-bg-base)',
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
                        color:
                          addType === 'NORMAL'
                            ? 'var(--color-primary)'
                            : 'var(--color-text-base)',
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
                        opacity: addType === 'NORMAL' ? 1 : 0.6,
                        backgroundColor:
                          addType === 'NORMAL'
                            ? 'rgba(16, 185, 129, 0.2)'
                            : 'rgba(107, 114, 128, 0.1)',
                        color:
                          addType === 'NORMAL'
                            ? 'var(--color-primary)'
                            : 'var(--color-text-muted)',
                      }}
                    >
                      {addType === 'NORMAL' && <Check size={12} />} Tiêu chuẩn
                    </span>
                  </div>
                  <span className="text-xs text-muted mt-1">Khung giờ tiêu chuẩn, giá thường</span>
                </button>

                {/* Ô Lựa chọn 2: Giờ vàng (Cao điểm) */}
                <button
                  type="button"
                  onClick={() => setAddType('PEAK')}
                  style={{
                    minHeight: '84px',
                    padding: '0.85rem 1rem',
                    borderRadius: 'var(--radius-md)',
                    border:
                      addType === 'PEAK'
                        ? '2px solid #f59e0b'
                        : '1px solid var(--color-border)',
                    backgroundColor:
                      addType === 'PEAK'
                        ? 'rgba(245, 158, 11, 0.1)'
                        : 'var(--color-bg-base)',
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
                        color: addType === 'PEAK' ? '#d97706' : 'var(--color-text-base)',
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
                        opacity: addType === 'PEAK' ? 1 : 0.6,
                        backgroundColor:
                          addType === 'PEAK'
                            ? 'rgba(245, 158, 11, 0.2)'
                            : 'rgba(107, 114, 128, 0.1)',
                        color:
                          addType === 'PEAK' ? '#d97706' : 'var(--color-text-muted)',
                      }}
                    >
                      {addType === 'PEAK' && <Check size={12} />} Cao điểm
                    </span>
                  </div>
                  <span className="text-xs text-muted mt-1">Khung giờ cao điểm có giá cao hơn</span>
                </button>
              </div>
            </div>
          </div>

          {/* KHỐI 3: FOOTER NÚT BẤM HÀNH ĐỘNG - 2 NÚT BẰNG NHAU HOÀN TOÀN (50% - 50%) */}
          <div
            style={{
              flexShrink: 0,
              paddingTop: '1.5rem',
              marginTop: '1.5rem',
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
              onClick={() => !submitting && setIsAddModalOpen(false)}
              disabled={submitting}
            >
              Hủy
            </button>
            <button
              type="button"
              className="btn btn-primary w-full"
              style={{
                height: '46px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '0.5rem',
                fontWeight: 600,
              }}
              onClick={handleAddSubmit}
              disabled={submitting}
            >
              {submitting ? (
                <>
                  <Loader2 size={18} className="animate-spin" />
                  Đang thêm...
                </>
              ) : (
                'Thêm khung giờ'
              )}
            </button>
          </div>
        </ModalOverlay>
      )}

      {/* ===================================================================================
          13. MODAL CHỈNH SỬA KHUNG GIỜ (EDIT TIME SLOT MODAL)
          - Spacing rộng rãi (gap: 1.5rem), các ô và cặp nút bấm hoàn toàn bằng nhau (50% - 50%)
          - Hỗ trợ cả gõ phím trực tiếp lẫn chọn đồng hồ
      =================================================================================== */}
      {editSlot && (
        <ModalOverlay onClose={() => !submitting && setEditSlot(null)}>
          {/* Header của Modal */}
          <div className="flex justify-between items-center mb-6 shrink-0">
            <div>
              <h2 className="text-xl font-bold">Chỉnh Sửa Khung Giờ</h2>
              <p className="text-sm text-muted mt-1">Cập nhật thời gian hoặc phân loại giờ vàng/thường</p>
            </div>
            <button
              onClick={() => !submitting && setEditSlot(null)}
              className="p-1.5 rounded-lg text-muted hover:text-[var(--color-text-base)] hover:bg-[var(--color-bg-base)] transition-colors"
              disabled={submitting}
              title="Đóng hộp thoại"
            >
              <X size={20} />
            </button>
          </div>

          {/* Hộp thông báo lỗi duy nhất trên đỉnh khi submit bị vi phạm */}
          {editError && (
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
              <span>{editError}</span>
            </div>
          )}

          {/* Form nội dung chỉnh sửa: Khoảng cách giữa các khối là 1.5rem (24px) thoáng đãng */}
          <div
            style={{
              overflowY: 'auto',
              flex: 1,
              display: 'flex',
              flexDirection: 'column',
              gap: '1.5rem',
              paddingRight: '0.25rem',
            }}
          >
            {/* KHỐI 1: Cặp ô giờ bắt đầu & giờ kết thúc BẰNG NHAU (50% - 50%) */}
            <div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <TimeInputWithPicker
                  label="Giờ bắt đầu"
                  value={editStartTime}
                  onChange={(val) => {
                    setEditStartTime(val);
                    if (editError) setEditError(null);
                  }}
                  placeholder="HH:mm (VD: 06:00)"
                />

                <TimeInputWithPicker
                  label="Giờ kết thúc"
                  value={editEndTime}
                  onChange={(val) => {
                    setEditEndTime(val);
                    if (editError) setEditError(null);
                  }}
                  placeholder="HH:mm (VD: 07:30)"
                />
              </div>

              {/* Dòng hướng dẫn thời lượng ca đá khi sửa: CHỈ HIỂN THỊ KHI CẢ 2 GIỜ HỢP LỆ */}
              {isEditBothTimeValid && (
                <div
                  style={{
                    marginTop: '0.75rem',
                    fontSize: '0.85rem',
                    fontWeight: 500,
                    padding: '0.65rem 0.85rem',
                    borderRadius: 'var(--radius-md)',
                    backgroundColor:
                      editEndTime <= editStartTime
                        ? 'rgba(239, 68, 68, 0.1)'
                        : editDuration >= 60 && editDuration <= 120
                        ? 'rgba(16, 185, 129, 0.1)'
                        : 'rgba(245, 158, 11, 0.1)',
                    color:
                      editEndTime <= editStartTime
                        ? 'var(--color-danger)'
                        : editDuration >= 60 && editDuration <= 120
                        ? 'var(--color-primary)'
                        : '#d97706',
                    border: `1px solid ${
                      editEndTime <= editStartTime
                        ? 'rgba(239, 68, 68, 0.3)'
                        : editDuration >= 60 && editDuration <= 120
                        ? 'rgba(16, 185, 129, 0.3)'
                        : 'rgba(245, 158, 11, 0.3)'
                    }`,
                  }}
                >
                  {editEndTime <= editStartTime ? (
                    '⚠ Giờ kết thúc phải lớn hơn giờ bắt đầu!'
                  ) : editDuration >= 60 && editDuration <= 120 ? (
                    `✓ Thời lượng ca: ${editDuration} phút (Hợp lệ: từ 1h đến 2h)`
                  ) : (
                    `⚠ Thời lượng ca: ${editDuration} phút (Yêu cầu quy định: từ 1h đến 2h)`
                  )}
                </div>
              )}
            </div>

            {/* KHỐI 2: Bộ chọn phân loại Khung giờ - 2 Ô BẰNG NHAU (50% - 50%), ĐỐI XỨNG TUYỆT ĐỐI */}
            <div>
              <label className="block text-sm font-semibold mb-2">Phân loại khung giờ</label>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <button
                  type="button"
                  onClick={() => setEditType('NORMAL')}
                  style={{
                    minHeight: '84px',
                    padding: '0.85rem 1rem',
                    borderRadius: 'var(--radius-md)',
                    border:
                      editType === 'NORMAL'
                        ? '2px solid var(--color-primary)'
                        : '1px solid var(--color-border)',
                    backgroundColor:
                      editType === 'NORMAL'
                        ? 'rgba(16, 185, 129, 0.08)'
                        : 'var(--color-bg-base)',
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
                        color:
                          editType === 'NORMAL'
                            ? 'var(--color-primary)'
                            : 'var(--color-text-base)',
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
                        opacity: editType === 'NORMAL' ? 1 : 0.6,
                        backgroundColor:
                          editType === 'NORMAL'
                            ? 'rgba(16, 185, 129, 0.2)'
                            : 'rgba(107, 114, 128, 0.1)',
                        color:
                          editType === 'NORMAL'
                            ? 'var(--color-primary)'
                            : 'var(--color-text-muted)',
                      }}
                    >
                      {editType === 'NORMAL' && <Check size={12} />} Tiêu chuẩn
                    </span>
                  </div>
                  <span className="text-xs text-muted mt-1">Khung giờ tiêu chuẩn, giá thường</span>
                </button>

                <button
                  type="button"
                  onClick={() => setEditType('PEAK')}
                  style={{
                    minHeight: '84px',
                    padding: '0.85rem 1rem',
                    borderRadius: 'var(--radius-md)',
                    border:
                      editType === 'PEAK'
                        ? '2px solid #f59e0b'
                        : '1px solid var(--color-border)',
                    backgroundColor:
                      editType === 'PEAK'
                        ? 'rgba(245, 158, 11, 0.1)'
                        : 'var(--color-bg-base)',
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
                        color: editType === 'PEAK' ? '#d97706' : 'var(--color-text-base)',
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
                        opacity: editType === 'PEAK' ? 1 : 0.6,
                        backgroundColor:
                          editType === 'PEAK'
                            ? 'rgba(245, 158, 11, 0.2)'
                            : 'rgba(107, 114, 128, 0.1)',
                        color:
                          editType === 'PEAK' ? '#d97706' : 'var(--color-text-muted)',
                      }}
                    >
                      {editType === 'PEAK' && <Check size={12} />} Cao điểm
                    </span>
                  </div>
                  <span className="text-xs text-muted mt-1">Khung giờ cao điểm có giá cao hơn</span>
                </button>
              </div>
            </div>
          </div>

          {/* KHỐI 3: FOOTER NÚT BẤM HÀNH ĐỘNG - 2 NÚT BẰNG NHAU HOÀN TOÀN (50% - 50%) */}
          <div
            style={{
              flexShrink: 0,
              paddingTop: '1.5rem',
              marginTop: '1.5rem',
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
              onClick={() => setEditSlot(null)}
              disabled={submitting}
            >
              Hủy bỏ
            </button>
            <button
              type="button"
              className="btn btn-primary w-full"
              style={{
                height: '46px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '0.5rem',
                fontWeight: 600,
              }}
              onClick={handleEditSubmit}
              disabled={submitting}
            >
              {submitting ? (
                <>
                  <Loader2 size={18} className="animate-spin" />
                  Đang lưu...
                </>
              ) : (
                'Lưu Thay Đổi'
              )}
            </button>
          </div>
        </ModalOverlay>
      )}

      {/* ===================================================================================
          14. MODAL XÁC NHẬN XÓA KHUNG GIỜ (DELETE CONFIRMATION MODAL)
          - 2 Nút bấm "Hủy bỏ" và "Xác nhận Xóa" hoàn toàn bằng nhau (50% - 50%)
      =================================================================================== */}
      {deleteSlot && (
        <ModalOverlay onClose={() => !submitting && setDeleteSlot(null)}>
          {/* Header cảnh báo xóa màu đỏ */}
          <div className="flex justify-between items-center mb-5 shrink-0">
            <h2 className="text-xl font-bold text-danger flex items-center gap-2">
              <Trash2 size={20} /> Xóa Khung Giờ
            </h2>
            <button
              onClick={() => !submitting && setDeleteSlot(null)}
              className="p-1.5 rounded-lg text-muted hover:text-[var(--color-text-base)] hover:bg-[var(--color-bg-base)] transition-colors"
              disabled={submitting}
              title="Đóng hộp thoại"
            >
              <X size={20} />
            </button>
          </div>

          {/* Thông tin chi tiết ca đá và nhắc nhở ràng buộc dữ liệu */}
          <div className="space-y-4 mb-6">
            <p className="text-base">
              Bạn có chắc chắn muốn xóa ca đá{' '}
              <strong className="text-[var(--color-primary)] font-bold">
                {deleteSlot.startTime} - {deleteSlot.endTime}
              </strong>{' '}
              khỏi hệ thống?
            </p>
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
              ⚠️ <strong>Lưu ý quan trọng:</strong> Hệ thống sẽ tự động chặn xóa nếu khung giờ này đang có khách hàng đặt lịch trong tương lai chưa hoàn tất.
            </div>
          </div>

          {/* Footer nút hành động xóa - 2 Nút bấm BẰNG NHAU (50% - 50%) */}
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
              onClick={() => setDeleteSlot(null)}
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
              onClick={handleDeleteConfirm}
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
      )}
    </div>
  );
};

export default AdminTimeSlots;
