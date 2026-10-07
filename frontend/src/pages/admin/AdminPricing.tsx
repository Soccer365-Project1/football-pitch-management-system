import React, { useState, useEffect, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { Plus, Trash2, Save, CalendarDays, SunDim, Gift, Clock, Zap, MapPin, X, AlertTriangle } from 'lucide-react';
import { pricingService } from '../../services/pricingService';
import { holidayService } from '../../services/holidayService';
import { pitchService } from '../../services/pitchService';
import { DayType, type PriceRateItemRequest } from '../../types/pricing';
import type { Holiday } from '../../types/holiday';
import type { PitchType } from '../../types/pitch';
import { showToast } from '../../utils/toast';
import { formatDate } from '../../utils/formatUtils';

/** Nhãn hiển thị chi tiết của từng ô trong ma trận giá để thông báo lỗi rõ ràng */
const CELL_LABELS: Record<string, string> = {
  'WEEKDAY_false': 'Trong tuần - Giờ thường',
  'WEEKDAY_true': 'Trong tuần - Giờ vàng',
  'WEEKEND_false': 'Cuối tuần - Giờ thường',
  'WEEKEND_true': 'Cuối tuần - Giờ vàng',
  'HOLIDAY_false': 'Ngày Lễ - Giờ thường',
  'HOLIDAY_true': 'Ngày Lễ - Giờ vàng',
};

/** Cấu hình hiển thị 3 hàng ma trận (Trong tuần, Cuối tuần, Ngày lễ) */
interface MatrixRowConfig {
  dayType: DayType;
  title: string;
  subTitle: string;
  icon: React.ElementType;
  iconColor: string;
  iconBg: string;
  iconBorder?: string;
  textColor?: string;
  normalInputColor?: string;
  peakInputColor: string;
  rowBg?: string;
}

const ROW_CONFIGS: MatrixRowConfig[] = [
  {
    dayType: DayType.WEEKDAY,
    title: 'Trong tuần',
    subTitle: 'Thứ 2 - Thứ 6',
    icon: CalendarDays,
    iconColor: 'var(--color-text-muted)',
    iconBg: 'var(--color-bg-base)',
    peakInputColor: 'var(--color-warning)',
  },
  {
    dayType: DayType.WEEKEND,
    title: 'Cuối tuần',
    subTitle: 'Thứ 7, Chủ Nhật',
    icon: SunDim,
    iconColor: 'var(--color-secondary)',
    iconBg: 'var(--color-bg-base)',
    textColor: 'var(--color-secondary)',
    normalInputColor: 'var(--color-secondary)',
    peakInputColor: 'var(--color-warning)',
  },
  {
    dayType: DayType.HOLIDAY,
    title: 'Ngày Lễ',
    subTitle: 'Áp dụng phụ thu',
    icon: Gift,
    iconColor: 'var(--color-danger)',
    iconBg: 'rgba(239, 68, 68, 0.1)',
    iconBorder: '1px solid rgba(239, 68, 68, 0.2)',
    textColor: 'var(--color-danger)',
    normalInputColor: 'var(--color-danger)',
    peakInputColor: 'var(--color-danger)',
    rowBg: 'rgba(239, 68, 68, 0.02)',
  },
];

/** Component Modal Overlay tái sử dụng theo thiết kế chuẩn của hệ thống */
const ModalOverlay = ({ children, onClose }: { children: React.ReactNode; onClose: () => void }) => {
  return createPortal(
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0,0,0,0.5)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 9999,
        padding: '1rem',
      }}
    >
      <div style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0 }} onClick={onClose} />
      <div
        className="card"
        style={{
          position: 'relative',
          zIndex: 10000,
          width: '100%',
          maxWidth: '450px',
          maxHeight: '90vh',
          overflowY: 'auto',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        {children}
      </div>
    </div>,
    document.body
  );
};

const AdminPricing: React.FC = () => {
  // ---------------------------------------------------------------------------------------
  // 1. Quản lý trạng thái Loại sân & Ma trận Biểu giá
  // ---------------------------------------------------------------------------------------
  const [pitchTypes, setPitchTypes] = useState<PitchType[]>([]);
  const [selectedPitchTypeId, setSelectedPitchTypeId] = useState<number>(0);
  const [savingPricing, setSavingPricing] = useState<boolean>(false);

  /**
   * Lưu trữ chuỗi giá trị đang hiển thị trong các ô input ma trận:
   * Key: `${dayType}_${isPeakHour}` (VD: 'WEEKDAY_false', 'WEEKDAY_true',...)
   * Value: Chuỗi hiển thị (ví dụ "200,000", "0", "" nếu chưa có giá)
   */
  const [ratesMap, setRatesMap] = useState<Record<string, string>>({});
  const [originalRatesMap, setOriginalRatesMap] = useState<Record<string, string>>({});

  /** Danh sách các ô bị lỗi validation (để viền đỏ cảnh báo) */
  const [invalidCellKeys, setInvalidCellKeys] = useState<string[]>([]);

  /** Trạng thái mở Popup xác nhận Lưu bảng giá */
  const [showSaveConfirm, setShowSaveConfirm] = useState<boolean>(false);

  /** Trạng thái mở Popup xác nhận Hủy thay đổi bảng giá */
  const [showCancelConfirm, setShowCancelConfirm] = useState<boolean>(false);

  // ---------------------------------------------------------------------------------------
  // 2. Quản lý trạng thái Danh mục Ngày lễ
  // ---------------------------------------------------------------------------------------
  const [holidays, setHolidays] = useState<Holiday[]>([]);
  const [addingHoliday, setAddingHoliday] = useState<boolean>(false);
  const [newHolidayDate, setNewHolidayDate] = useState<string>('');
  const [newHolidayName, setNewHolidayName] = useState<string>('');

  /** Trạng thái mở Popup xác nhận Thêm ngày lễ */
  const [showAddHolidayConfirm, setShowAddHolidayConfirm] = useState<boolean>(false);

  /** Đối tượng ngày lễ đang chọn để Xóa */
  const [deleteHoliday, setDeleteHoliday] = useState<Holiday | null>(null);
  const [deletingHoliday, setDeletingHoliday] = useState<boolean>(false);

  // ---------------------------------------------------------------------------------------
  // 3. Tải danh mục Loại sân bóng
  // ---------------------------------------------------------------------------------------
  const loadPitchTypes = useCallback(async () => {
    try {
      const types = await pitchService.getPitchTypes();
      if (types && types.length > 0) {
        setPitchTypes(types);
        setSelectedPitchTypeId(types[0].id);
      } else {
        setPitchTypes([]);
      }
    } catch {
      showToast('Không thể tải danh mục loại sân từ máy chủ', 'error');
    }
  }, []);

  // ---------------------------------------------------------------------------------------
  // 4. Tải Ma trận Bảng giá cho Loại sân đang chọn
  // ---------------------------------------------------------------------------------------
  const loadPricingForPitchType = useCallback(async (typeId: number) => {
    if (!typeId) return;
    try {
      const matrix = await pricingService.getPricingByPitchTypeId(typeId);
      const newMap: Record<string, string> = {};

      if (matrix && matrix.rates && matrix.rates.length > 0) {
        matrix.rates.forEach((item) => {
          const key = `${item.dayType}_${item.isPeakHour}`;
          newMap[key] = item.price ? Number(item.price).toLocaleString('en-US') : '';
        });
      }

      setRatesMap(newMap);
      setOriginalRatesMap(newMap);
      setInvalidCellKeys([]);
    } catch {
      setRatesMap({});
      setOriginalRatesMap({});
      setInvalidCellKeys([]);
    }
  }, []);

  // ---------------------------------------------------------------------------------------
  // 5. Tải Danh sách Ngày lễ từ Backend
  // ---------------------------------------------------------------------------------------
  const loadHolidays = useCallback(async () => {
    try {
      const data = await holidayService.getAllHolidays();
      const sorted = (data || []).sort((a, b) => a.holidayDate.localeCompare(b.holidayDate));
      setHolidays(sorted);
    } catch {
      showToast('Không thể tải danh sách ngày lễ từ máy chủ', 'error');
    }
  }, []);

  useEffect(() => {
    loadPitchTypes();
    loadHolidays();
  }, [loadPitchTypes, loadHolidays]);

  useEffect(() => {
    if (selectedPitchTypeId) {
      loadPricingForPitchType(selectedPitchTypeId);
    }
  }, [selectedPitchTypeId, loadPricingForPitchType]);

  // ---------------------------------------------------------------------------------------
  // 6. Xử lý Thay đổi & Validation Ma trận Bảng giá
  // ---------------------------------------------------------------------------------------
  /** Kiểm tra xem bảng giá hiện tại có thay đổi so với bản gốc đã lưu hay không */
  const hasPriceChanges = useCallback(() => {
    const allKeys = new Set([...Object.keys(ratesMap), ...Object.keys(originalRatesMap)]);
    for (const k of allKeys) {
      const current = (ratesMap[k] || '').trim();
      const original = (originalRatesMap[k] || '').trim();
      if (current !== original) {
        return true;
      }
    }
    return false;
  }, [ratesMap, originalRatesMap]);

  /** Mức giá tối đa quy định cho một ca đá bóng: 50.000.000 VNĐ */
  const MAX_PRICE = 50_000_000;

  /** Cập nhật mức giá khi người dùng gõ phím */
  const handlePriceCellChange = (dayType: DayType, isPeakHour: boolean, rawValue: string) => {
    const numericStr = rawValue.replace(/\D/g, '');
    const key = `${dayType}_${isPeakHour}`;

    // Giới hạn tối đa 9 chữ số (tối đa đến 999.999.999đ) để tránh tràn số
    if (numericStr.length > 9) return;

    if (!numericStr) {
      setRatesMap((prev) => ({ ...prev, [key]: '' }));
    } else {
      let formatted = numericStr;
      if (numericStr.length > 1 && numericStr.startsWith('0')) {
        const trimmed = numericStr.replace(/^0+/, '') || '0';
        formatted = trimmed.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
      } else {
        formatted = numericStr.replace(/\B(?=(\d{3})+(?!\d))/g, ',');
      }

      setRatesMap((prev) => ({ ...prev, [key]: formatted }));
    }

    if (invalidCellKeys.includes(key)) {
      setInvalidCellKeys((prev) => prev.filter((k) => k !== key));
    }
  };

  /**
   * Mở Popup xác nhận HỦY thay đổi bảng giá:
   * Nếu chưa chỉnh sửa gì -> thông báo toast nhắc người dùng.
   * Nếu có chỉnh sửa dở dang -> mở Popup xác nhận trước khi khôi phục.
   */
  const handleOpenCancelConfirm = () => {
    if (!hasPriceChanges()) {
      showToast('Bảng giá hiện tại chưa có thay đổi nào để hủy!', 'info');
      return;
    }
    setShowCancelConfirm(true);
  };

  /** Thực hiện Hủy thay đổi sau khi xác nhận trong Popup */
  const handleConfirmCancelPricing = () => {
    setRatesMap(originalRatesMap);
    setInvalidCellKeys([]);
    setShowCancelConfirm(false);
    showToast('Đã khôi phục bảng giá về trạng thái ban đầu!', 'info');
  };

  /**
   * Mở Popup xác nhận LƯU bảng giá:
   * - Kiểm tra các ô đã nhập giá: bắt buộc 1.000đ <= giá <= 50.000.000đ.
   * - Nếu chưa nhập ô nào cả: yêu cầu nhập ít nhất 1 ô.
   * - Nếu hợp lệ: mở Popup xác nhận.
   */
  const handlePreSaveCheck = () => {
    if (!selectedPitchTypeId) {
      showToast('Vui lòng chọn loại sân trước khi lưu!', 'error');
      return;
    }

    const errors: string[] = [];
    const badKeys: string[] = [];
    let filledCount = 0;

    for (const dt of [DayType.WEEKDAY, DayType.WEEKEND, DayType.HOLIDAY]) {
      for (const isPeak of [false, true]) {
        const key = `${dt}_${isPeak}`;
        const valStr = ratesMap[key]?.trim();
        const label = CELL_LABELS[key] || key;

        if (valStr && valStr !== '') {
          filledCount++;
          const numericStr = valStr.replace(/\D/g, '');
          const price = numericStr ? parseInt(numericStr, 10) : 0;

          if (price < 1000) {
            errors.push(`Mức giá ô "${label}" phải từ 1.000đ trở lên (hiện tại: ${price.toLocaleString('en-US')}đ).`);
            badKeys.push(key);
          } else if (price > MAX_PRICE) {
            errors.push(
              `Mức giá ô "${label}" không được vượt quá ${MAX_PRICE.toLocaleString('en-US')}đ (hiện tại: ${price.toLocaleString('en-US')}đ).`
            );
            badKeys.push(key);
          }
        }
      }
    }

    if (errors.length > 0) {
      setInvalidCellKeys(badKeys);
      if (errors.length === 1) {
        showToast(errors[0], 'error');
      } else {
        showToast(`Có ${errors.length} ô giá chưa hợp lệ! ${errors[0]}`, 'error');
      }
      return;
    }

    if (filledCount === 0) {
      showToast('Vui lòng nhập giá cho ít nhất một khung giờ trước khi lưu!', 'error');
      return;
    }

    setInvalidCellKeys([]);
    setShowSaveConfirm(true);
  };

  /** Thực hiện Lưu bảng giá xuống Backend sau khi xác nhận trong Popup */
  const handleConfirmSavePricing = async () => {
    const rates: PriceRateItemRequest[] = [];

    for (const dt of [DayType.WEEKDAY, DayType.WEEKEND, DayType.HOLIDAY]) {
      for (const isPeak of [false, true]) {
        const key = `${dt}_${isPeak}`;
        const valStr = ratesMap[key]?.trim();

        if (valStr && valStr !== '') {
          const numericStr = valStr.replace(/\D/g, '');
          const price = numericStr ? parseInt(numericStr, 10) : 0;
          if (price >= 1000 && price <= MAX_PRICE) {
            rates.push({
              dayType: dt,
              isPeakHour: isPeak,
              price,
            });
          }
        }
      }
    }

    if (rates.length === 0) {
      showToast('Vui lòng nhập giá cho ít nhất một khung giờ!', 'error');
      setShowSaveConfirm(false);
      return;
    }

    setSavingPricing(true);
    try {
      await pricingService.savePricingForPitchType(selectedPitchTypeId, { rates });
      showToast(`Lưu bảng giá (${rates.length} khung giờ) thành công!`, 'success');
      setOriginalRatesMap(ratesMap);
      setShowSaveConfirm(false);
    } catch (err: any) {
      const backendErrors = err.response?.data?.errors;
      let msg = err.response?.data?.message;
      if (backendErrors && backendErrors.length > 0) {
        msg = backendErrors[0].message || msg;
      }
      showToast(msg || 'Lỗi khi lưu bảng giá. Vui lòng thử lại!', 'error');
    } finally {
      setSavingPricing(false);
    }
  };

  // ---------------------------------------------------------------------------------------
  // 7. Xử lý Thêm & Xóa Ngày lễ (Đều có Popup xác nhận)
  // ---------------------------------------------------------------------------------------
  /**
   * Mở Popup xác nhận THÊM Ngày Lễ:
   * Kiểm tra tính hợp lệ trước khi mở popup.
   */
  const handlePreAddHolidayCheck = () => {
    if (!newHolidayDate) {
      showToast('Vui lòng chọn ngày diễn ra sự kiện!', 'error');
      return;
    }
    const trimmedName = newHolidayName.trim();
    if (!trimmedName) {
      showToast('Vui lòng nhập tên sự kiện ngày lễ!', 'error');
      return;
    }
    if (newHolidayName.length > 150) {
      showToast(
        `Tên ngày lễ dài ${newHolidayName.length} ký tự, vượt quá giới hạn cho phép (tối đa 150 ký tự)!`,
        'error'
      );
      return;
    }
    if (holidays.some((h) => h.holidayDate === newHolidayDate)) {
      showToast(`Ngày ${formatDate(newHolidayDate)} đã tồn tại trong danh sách ngày lễ!`, 'error');
      return;
    }
    if (holidays.some((h) => h.name.trim().toLowerCase() === trimmedName.toLowerCase())) {
      showToast(`Tên sự kiện "${trimmedName}" đã tồn tại trong danh sách!`, 'error');
      return;
    }

    // Hợp lệ -> mở Popup xác nhận
    setShowAddHolidayConfirm(true);
  };

  /** Thực hiện Thêm ngày lễ sau khi xác nhận trong Popup */
  const handleConfirmAddHoliday = async () => {
    const trimmedName = newHolidayName.trim();
    setAddingHoliday(true);
    try {
      await holidayService.createHoliday({
        holidayDate: newHolidayDate,
        name: trimmedName,
        description: 'Áp dụng phụ thu ngày lễ',
      });
      showToast('Thêm ngày lễ thành công!', 'success');
      setNewHolidayDate('');
      setNewHolidayName('');
      setShowAddHolidayConfirm(false);
      await loadHolidays();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Không thể thêm ngày lễ. Vui lòng thử lại!';
      showToast(msg, 'error');
    } finally {
      setAddingHoliday(false);
    }
  };

  /** Thực hiện Xóa ngày lễ sau khi xác nhận trong Popup */
  const handleConfirmDeleteHoliday = async () => {
    if (!deleteHoliday) return;
    setDeletingHoliday(true);
    try {
      await holidayService.deleteHoliday(deleteHoliday.id);
      showToast('Đã xóa ngày lễ khỏi hệ thống!', 'success');
      setDeleteHoliday(null);
      await loadHolidays();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Lỗi khi xóa ngày lễ. Vui lòng thử lại!';
      showToast(msg, 'error');
    } finally {
      setDeletingHoliday(false);
    }
  };

  const currentPitchType = pitchTypes.find((p) => p.id === selectedPitchTypeId);

  // ---------------------------------------------------------------------------------------
  // 8. Render Giao diện (Khớp 100% thiết kế Hình 1)
  // ---------------------------------------------------------------------------------------
  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1.5rem' }}>
        {/* LÊN BẢNG GIÁ MA TRẬN */}
        <div className="card" style={{ flex: '1 1 600px' }}>
          <div className="flex justify-between items-center mb-6">
            <div>
              <h2 className="text-xl font-bold">Bảng giá sân</h2>
              <p className="text-sm text-muted mt-2">Hệ thống tự động áp dụng giá theo thời điểm đặt sân.</p>
            </div>

            <div className="flex items-center gap-4">
              <div
                className="flex items-center gap-2"
                style={{
                  border: '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-full)',
                  padding: '0.35rem 1rem',
                  backgroundColor: 'var(--color-bg-base)',
                  color: 'var(--color-text-base)',
                }}
              >
                <MapPin size={16} className="text-muted" />
                <select
                  value={selectedPitchTypeId}
                  onChange={(e) => setSelectedPitchTypeId(Number(e.target.value))}
                  style={{
                    background: 'transparent',
                    border: 'none',
                    outline: 'none',
                    color: 'inherit',
                    fontWeight: 'bold',
                    cursor: 'pointer',
                    fontFamily: 'inherit',
                  }}
                >
                  {pitchTypes.map((pt) => (
                    <option key={pt.id} value={pt.id} style={{ backgroundColor: 'var(--color-bg-surface)' }}>
                      {pt.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>

          <div style={{ borderRadius: 'var(--radius-md)', overflowX: 'auto', border: '1px solid var(--color-border)' }}>
            <table style={{ width: '100%', minWidth: '600px', borderCollapse: 'collapse', textAlign: 'left' }}>
              <thead>
                <tr>
                  <th
                    className="font-bold text-sm"
                    style={{
                      padding: '1rem',
                      border: '1px solid var(--color-border)',
                      backgroundColor: 'var(--color-bg-base)',
                    }}
                  >
                    PHÂN LOẠI NGÀY
                  </th>
                  <th
                    className="font-bold text-sm"
                    style={{
                      padding: '1rem',
                      border: '1px solid var(--color-border)',
                      backgroundColor: 'rgba(16, 185, 129, 0.05)',
                      color: 'var(--color-primary)',
                    }}
                  >
                    <div className="flex items-center gap-2">
                      <Clock size={18} /> GIỜ THƯỜNG
                    </div>
                  </th>
                  <th
                    className="font-bold text-sm"
                    style={{
                      padding: '1rem',
                      border: '1px solid var(--color-border)',
                      backgroundColor: 'rgba(245, 158, 11, 0.05)',
                      color: 'var(--color-warning)',
                    }}
                  >
                    <div className="flex items-center gap-2">
                      <Zap size={18} /> GIỜ VÀNG (CAO ĐIỂM)
                    </div>
                  </th>
                </tr>
              </thead>
              <tbody>
                {ROW_CONFIGS.map((row) => {
                  const IconComp = row.icon;
                  const normalKey = `${row.dayType}_false`;
                  const peakKey = `${row.dayType}_true`;
                  const isNormalBad = invalidCellKeys.includes(normalKey);
                  const isPeakBad = invalidCellKeys.includes(peakKey);

                  return (
                    <tr key={row.dayType} style={{ backgroundColor: row.rowBg || 'transparent' }}>
                      <td style={{ padding: '1.25rem 1rem', border: '1px solid var(--color-border)' }}>
                        <div className="flex items-center gap-4">
                          <div
                            style={{
                              padding: '0.5rem',
                              borderRadius: 'var(--radius-md)',
                              backgroundColor: row.iconBg,
                              border: row.iconBorder || '1px solid var(--color-border)',
                            }}
                          >
                            <IconComp size={20} style={{ color: row.iconColor }} />
                          </div>
                          <div>
                            <div className="font-bold" style={{ color: row.textColor || 'inherit' }}>
                              {row.title}
                            </div>
                            <div className="text-sm text-muted mt-1" style={{ whiteSpace: 'nowrap' }}>
                              {row.subTitle}
                            </div>
                          </div>
                        </div>
                      </td>

                      {/* Giờ thường */}
                      <td style={{ padding: '1.25rem 1rem', border: '1px solid var(--color-border)' }}>
                        <div
                          className="flex items-center gap-2"
                          style={{
                            backgroundColor: 'var(--color-bg-base)',
                            padding: '0.75rem',
                            borderRadius: 'var(--radius-md)',
                            border: isNormalBad ? '1.5px solid var(--color-danger)' : '1px solid var(--color-border)',
                            transition: 'border 0.2s ease',
                          }}
                        >
                          <input
                            type="text"
                            value={ratesMap[normalKey] ?? ''}
                            onChange={(e) => handlePriceCellChange(row.dayType, false, e.target.value)}
                            placeholder="Chưa có"
                            style={{
                              width: '100%',
                              textAlign: 'right',
                              fontSize: '1.125rem',
                              fontWeight: 'bold',
                              background: 'transparent',
                              border: 'none',
                              outline: 'none',
                              color: row.normalInputColor || 'var(--color-text-base)',
                              fontFamily: 'inherit',
                            }}
                          />
                          <span className="font-bold text-muted">đ</span>
                        </div>
                      </td>

                      {/* Giờ vàng */}
                      <td style={{ padding: '1.25rem 1rem', border: '1px solid var(--color-border)' }}>
                        <div
                          className="flex items-center gap-2"
                          style={{
                            backgroundColor: 'var(--color-bg-base)',
                            padding: '0.75rem',
                            borderRadius: 'var(--radius-md)',
                            border: isPeakBad ? '1.5px solid var(--color-danger)' : '1px solid var(--color-border)',
                            transition: 'border 0.2s ease',
                          }}
                        >
                          <input
                            type="text"
                            value={ratesMap[peakKey] ?? ''}
                            onChange={(e) => handlePriceCellChange(row.dayType, true, e.target.value)}
                            placeholder="Chưa có"
                            style={{
                              width: '100%',
                              textAlign: 'right',
                              fontSize: '1.125rem',
                              fontWeight: 'bold',
                              background: 'transparent',
                              border: 'none',
                              outline: 'none',
                              color: row.peakInputColor,
                              fontFamily: 'inherit',
                            }}
                          />
                          <span className="font-bold text-muted">đ</span>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          <div style={{ marginTop: '1.5rem', display: 'flex', justifyContent: 'flex-end', gap: '1rem' }}>
            <button
              type="button"
              className="btn btn-secondary"
              style={{ padding: '0.75rem 2rem', fontSize: '1rem', borderRadius: 'var(--radius-md)' }}
              onClick={handleOpenCancelConfirm}
              disabled={savingPricing}
            >
              Hủy
            </button>
            <button
              type="button"
              className="btn btn-primary"
              style={{ padding: '0.75rem 2rem', fontSize: '1rem', borderRadius: 'var(--radius-md)' }}
              onClick={handlePreSaveCheck}
              disabled={savingPricing}
            >
              <Save size={18} /> Lưu Bảng giá
            </button>
          </div>
        </div>

        {/* CẤU HÌNH NGÀY LỄ */}
        <div style={{ flex: '1 1 350px', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          <div className="card" style={{ height: '530px', display: 'flex', flexDirection: 'column' }}>
            <div className="flex items-center gap-4 mb-6" style={{ flexShrink: 0 }}>
              <div
                style={{
                  padding: '0.5rem',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'rgba(239, 68, 68, 0.1)',
                }}
              >
                <Gift size={24} style={{ color: 'var(--color-danger)' }} />
              </div>
              <h2 className="text-xl font-bold">Ngày Lễ</h2>
            </div>

            <div style={{ flex: 1, overflowY: 'auto', paddingRight: '0.5rem' }}>
              <div
                style={{
                  marginBottom: '1.5rem',
                  padding: '1rem',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'var(--color-bg-base)',
                  border: '1px dashed var(--color-border)',
                }}
              >
                <div className="mb-4">
                  <label className="font-semibold text-sm" style={{ display: 'block', marginBottom: '0.5rem' }}>
                    Chọn ngày
                  </label>
                  <input
                    type="date"
                    value={newHolidayDate}
                    onChange={(e) => setNewHolidayDate(e.target.value)}
                    style={{
                      width: '100%',
                      boxSizing: 'border-box',
                      padding: '0.75rem',
                      borderRadius: 'var(--radius-md)',
                      border: '1px solid var(--color-border)',
                      backgroundColor: 'var(--color-bg-surface)',
                      color: 'var(--color-text-base)',
                      outline: 'none',
                      fontFamily: 'inherit',
                    }}
                  />
                </div>
                <div className="mb-4">
                  <label className="font-semibold text-sm" style={{ display: 'block', marginBottom: '0.5rem' }}>
                    Tên sự kiện
                  </label>
                  <input
                    type="text"
                    value={newHolidayName}
                    onChange={(e) => setNewHolidayName(e.target.value)}
                    placeholder="VD: Lễ Quốc khánh 2/9"
                    style={{
                      width: '100%',
                      boxSizing: 'border-box',
                      padding: '0.75rem',
                      borderRadius: 'var(--radius-md)',
                      border: '1px solid var(--color-border)',
                      backgroundColor: 'var(--color-bg-surface)',
                      color: 'var(--color-text-base)',
                      outline: 'none',
                      fontFamily: 'inherit',
                    }}
                  />
                </div>
                <button
                  type="button"
                  onClick={handlePreAddHolidayCheck}
                  disabled={addingHoliday}
                  className="btn btn-secondary"
                  style={{
                    width: '100%',
                    justifyContent: 'center',
                    color: 'var(--color-primary)',
                    border: '1px solid rgba(16, 185, 129, 0.5)',
                  }}
                >
                  <Plus size={18} /> Thêm vào danh sách
                </button>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                <h3 className="font-semibold text-sm text-muted mb-2">DANH SÁCH ĐÃ THÊM</h3>

                {holidays.length === 0 ? (
                  <div
                    style={{
                      textAlign: 'center',
                      padding: '1.5rem',
                      color: 'var(--color-text-muted)',
                      fontSize: '0.875rem',
                    }}
                  >
                    Chưa có ngày lễ nào trong danh sách.
                  </div>
                ) : (
                  holidays.map((item) => (
                    <div
                      key={item.id}
                      className="flex justify-between items-center"
                      style={{
                        padding: '1rem',
                        backgroundColor: 'var(--color-bg-base)',
                        borderRadius: 'var(--radius-md)',
                        borderLeft: '4px solid var(--color-danger)',
                      }}
                    >
                      <div>
                        <div className="font-bold text-lg" style={{ color: 'var(--color-danger)' }}>
                          {formatDate(item.holidayDate)}
                        </div>
                        <div className="text-sm font-semibold mt-1">{item.name}</div>
                      </div>
                      <button
                        type="button"
                        style={{
                          padding: '0.5rem',
                          color: 'var(--color-text-muted)',
                          background: 'transparent',
                          border: 'none',
                          cursor: 'pointer',
                        }}
                        onClick={() => setDeleteHoliday(item)}
                        title="Xóa ngày lễ"
                      >
                        <Trash2 size={18} />
                      </button>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 1. Modal Popup Xác nhận Lưu Bảng giá */}
      {showSaveConfirm && (
        <ModalOverlay onClose={() => !savingPricing && setShowSaveConfirm(false)}>
          <div className="flex justify-between items-center mb-6" style={{ flexShrink: 0 }}>
            <div className="flex items-center gap-3">
              <div
                style={{
                  padding: '0.5rem',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'rgba(16, 185, 129, 0.1)',
                  color: 'var(--color-primary)',
                }}
              >
                <Save size={20} />
              </div>
              <h2 className="text-xl font-bold">Xác nhận lưu Bảng giá</h2>
            </div>
            <button
              onClick={() => !savingPricing && setShowSaveConfirm(false)}
              className="text-muted hover:text-[var(--color-text-base)]"
              style={{ background: 'transparent', border: 'none', cursor: 'pointer' }}
            >
              <X size={24} />
            </button>
          </div>

          <div className="mb-6">
            <p style={{ lineHeight: '1.6' }}>
              Bạn có chắc chắn muốn lưu và áp dụng bảng giá mới cho{' '}
              <strong>{currentPitchType?.name || 'loại sân này'}</strong> không?
            </p>
            <div
              style={{
                marginTop: '1rem',
                padding: '0.875rem 1rem',
                backgroundColor: 'var(--color-bg-base)',
                borderRadius: 'var(--radius-md)',
                borderLeft: '4px solid var(--color-primary)',
                fontSize: '0.875rem',
                color: 'var(--color-text-muted)',
                lineHeight: '1.5',
              }}
            >
              Bảng giá mới gồm các khung giờ đã thiết lập sẽ được hệ thống tự động áp dụng ngay cho các đơn đặt sân mới.
            </div>
          </div>

          <div
            style={{
              flexShrink: 0,
              paddingTop: '1rem',
              marginTop: 'auto',
              borderTop: '1px solid var(--color-border)',
              display: 'flex',
              gap: '1rem',
            }}
          >
            <button
              type="button"
              className="btn btn-secondary w-1/2"
              onClick={() => setShowSaveConfirm(false)}
              disabled={savingPricing}
            >
              Hủy bỏ
            </button>
            <button
              type="button"
              className="btn btn-primary w-1/2"
              onClick={handleConfirmSavePricing}
              disabled={savingPricing}
            >
              {savingPricing ? 'Đang lưu...' : 'Xác nhận Lưu'}
            </button>
          </div>
        </ModalOverlay>
      )}

      {/* 2. Modal Popup Xác nhận HỦY thay đổi Bảng giá */}
      {showCancelConfirm && (
        <ModalOverlay onClose={() => setShowCancelConfirm(false)}>
          <div className="flex justify-between items-center mb-6" style={{ flexShrink: 0 }}>
            <div className="flex items-center gap-3">
              <div
                style={{
                  padding: '0.5rem',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'rgba(245, 158, 11, 0.1)',
                  color: 'var(--color-warning)',
                }}
              >
                <AlertTriangle size={20} />
              </div>
              <h2 className="text-xl font-bold">Xác nhận Hủy thay đổi</h2>
            </div>
            <button
              onClick={() => setShowCancelConfirm(false)}
              className="text-muted hover:text-[var(--color-text-base)]"
              style={{ background: 'transparent', border: 'none', cursor: 'pointer' }}
            >
              <X size={24} />
            </button>
          </div>

          <div className="mb-6">
            <p style={{ lineHeight: '1.6' }}>
              Bạn có chắc chắn muốn hủy bỏ các mức giá vừa chỉnh sửa cho{' '}
              <strong>{currentPitchType?.name || 'loại sân này'}</strong> không?
            </p>
            <div
              style={{
                marginTop: '1rem',
                padding: '0.875rem 1rem',
                backgroundColor: 'var(--color-bg-base)',
                borderRadius: 'var(--radius-md)',
                borderLeft: '4px solid var(--color-warning)',
                fontSize: '0.875rem',
                color: 'var(--color-text-muted)',
                lineHeight: '1.5',
              }}
            >
              Mọi thay đổi chưa lưu sẽ bị xóa và các ô giá sẽ được khôi phục về giá trị đã lưu ban đầu từ hệ thống.
            </div>
          </div>

          <div
            style={{
              flexShrink: 0,
              paddingTop: '1rem',
              marginTop: 'auto',
              borderTop: '1px solid var(--color-border)',
              display: 'flex',
              gap: '1rem',
            }}
          >
            <button
              type="button"
              className="btn btn-secondary w-1/2"
              onClick={() => setShowCancelConfirm(false)}
            >
              Tiếp tục sửa
            </button>
            <button
              type="button"
              className="btn btn-primary w-1/2"
              style={{ backgroundColor: 'var(--color-warning)', border: 'none', color: '#fff' }}
              onClick={handleConfirmCancelPricing}
            >
              Xác nhận Hủy
            </button>
          </div>
        </ModalOverlay>
      )}

      {/* 3. Modal Popup Xác nhận THÊM Ngày Lễ */}
      {showAddHolidayConfirm && (
        <ModalOverlay onClose={() => !addingHoliday && setShowAddHolidayConfirm(false)}>
          <div className="flex justify-between items-center mb-6" style={{ flexShrink: 0 }}>
            <div className="flex items-center gap-3">
              <div
                style={{
                  padding: '0.5rem',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'rgba(16, 185, 129, 0.1)',
                  color: 'var(--color-primary)',
                }}
              >
                <Gift size={20} />
              </div>
              <h2 className="text-xl font-bold">Xác nhận Thêm Ngày Lễ</h2>
            </div>
            <button
              onClick={() => !addingHoliday && setShowAddHolidayConfirm(false)}
              className="text-muted hover:text-[var(--color-text-base)]"
              style={{ background: 'transparent', border: 'none', cursor: 'pointer' }}
            >
              <X size={24} />
            </button>
          </div>

          <div className="mb-6">
            <p style={{ lineHeight: '1.6' }}>
              Bạn có chắc chắn muốn thêm ngày lễ <strong>{newHolidayName.trim()}</strong> (ngày{' '}
              <strong style={{ color: 'var(--color-danger)' }}>{formatDate(newHolidayDate)}</strong>) vào hệ thống không?
            </p>
            <div
              style={{
                marginTop: '1rem',
                padding: '0.875rem 1rem',
                backgroundColor: 'var(--color-bg-base)',
                borderRadius: 'var(--radius-md)',
                borderLeft: '4px solid var(--color-primary)',
                fontSize: '0.875rem',
                color: 'var(--color-text-muted)',
                lineHeight: '1.5',
              }}
            >
              Khi được thêm, toàn bộ các đơn đặt sân trong ngày này sẽ tự động áp dụng biểu giá Ngày Lễ (phụ thu lễ).
            </div>
          </div>

          <div
            style={{
              flexShrink: 0,
              paddingTop: '1rem',
              marginTop: 'auto',
              borderTop: '1px solid var(--color-border)',
              display: 'flex',
              gap: '1rem',
            }}
          >
            <button
              type="button"
              className="btn btn-secondary w-1/2"
              onClick={() => setShowAddHolidayConfirm(false)}
              disabled={addingHoliday}
            >
              Hủy bỏ
            </button>
            <button
              type="button"
              className="btn btn-primary w-1/2"
              onClick={handleConfirmAddHoliday}
              disabled={addingHoliday}
            >
              {addingHoliday ? 'Đang thêm...' : 'Xác nhận Thêm'}
            </button>
          </div>
        </ModalOverlay>
      )}

      {/* 4. Modal Popup Xác nhận XÓA Ngày Lễ */}
      {deleteHoliday && (
        <ModalOverlay onClose={() => setDeleteHoliday(null)}>
          <div className="flex justify-between items-center mb-6" style={{ flexShrink: 0 }}>
            <h2 className="text-xl font-bold">Xóa Ngày Lễ</h2>
            <button
              onClick={() => setDeleteHoliday(null)}
              className="text-muted hover:text-[var(--color-text-base)]"
              style={{ background: 'transparent', border: 'none', cursor: 'pointer' }}
            >
              <X size={24} />
            </button>
          </div>
          <div className="mb-6">
            <p>
              Bạn có chắc chắn muốn xóa ngày <strong>{deleteHoliday.name}</strong> ({formatDate(deleteHoliday.holidayDate)})
              khỏi danh sách ngày lễ không?
            </p>
            <p className="text-muted text-sm mt-2">
              Hành động này sẽ hủy áp dụng phụ thu lễ cho các đơn đặt sân trong ngày này.
            </p>
          </div>
          <div
            style={{
              flexShrink: 0,
              paddingTop: '1rem',
              marginTop: 'auto',
              borderTop: '1px solid var(--color-border)',
              display: 'flex',
              gap: '1rem',
            }}
          >
            <button
              className="btn btn-secondary w-1/2"
              onClick={() => setDeleteHoliday(null)}
              disabled={deletingHoliday}
            >
              Hủy bỏ
            </button>
            <button
              className="btn btn-primary w-1/2"
              style={{ backgroundColor: 'var(--color-danger)', border: 'none' }}
              onClick={handleConfirmDeleteHoliday}
              disabled={deletingHoliday}
            >
              {deletingHoliday ? 'Đang xóa...' : 'Xác nhận Xóa'}
            </button>
          </div>
        </ModalOverlay>
      )}
    </div>
  );
};

export default AdminPricing;
