import React, { useState, useEffect } from 'react';
import { X, Loader2 } from 'lucide-react';
import { ModalOverlay } from '../../common/ModalOverlay';
import { adminBookingService } from '../../../services/adminBookingService';
import { pitchService } from '../../../services/pitchService';
import type { Pitch, TimeSlot, ScheduleGridResponse } from '../../../types/pitch';
import type { PaymentMethodCode, PaymentType, CounterBookingCreationPayload } from '../../../types/adminBooking';
import { formatPrice } from '../../../utils/formatUtils';
import { showToast } from '../../../utils/toast';

export interface CounterBookingModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const CounterBookingModal: React.FC<CounterBookingModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [timeSlots, setTimeSlots] = useState<TimeSlot[]>([]);
  const [scheduleGrid, setScheduleGrid] = useState<ScheduleGridResponse | null>(null);

  const [loadingInitial, setLoadingInitial] = useState<boolean>(false);
  const [loadingSlots, setLoadingSlots] = useState<boolean>(false);
  const [submitting, setSubmitting] = useState<boolean>(false);

  // Field validation states (chỉ hiển thị inline dưới ô bị lỗi)
  const [phoneError, setPhoneError] = useState<string | null>(null);
  const [nameError, setNameError] = useState<string | null>(null);
  const [slotError, setSlotError] = useState<string | null>(null);
  const [serverError, setServerError] = useState<string | null>(null);

  // Form states
  const now = new Date();
  const todayStr = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;

  const [guestName, setGuestName] = useState<string>('Khách vãng lai');
  const [guestPhone, setGuestPhone] = useState<string>('');
  const [selectedPitchId, setSelectedPitchId] = useState<number | ''>('');
  const [bookingDate, setBookingDate] = useState<string>(todayStr);
  const [selectedSlotId, setSelectedSlotId] = useState<number | ''>('');
  const [paymentType, setPaymentType] = useState<PaymentType>('DEPOSIT_30');
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethodCode>('CASH');

  const VIETNAM_PHONE_REGEX = /^(0[3|5|7|8|9])[0-9]{8}$/;

  // Reset form khi mở modal
  useEffect(() => {
    if (isOpen) {
      setPhoneError(null);
      setNameError(null);
      setSlotError(null);
      setServerError(null);
      setGuestName('Khách vãng lai');
      setGuestPhone('');
      setSelectedSlotId('');
      setPaymentType('DEPOSIT_30');
      setPaymentMethod('CASH');
      setBookingDate(todayStr);
    }
  }, [isOpen, todayStr]);

  // 1. Tải danh mục sân và khung giờ
  useEffect(() => {
    if (!isOpen) return;

    let isMounted = true;
    setLoadingInitial(true);

    Promise.all([pitchService.getPitches(), pitchService.getTimeSlots()])
      .then(([pitchesData, slotsData]) => {
        if (!isMounted) return;
        const activePitches = Array.isArray(pitchesData)
          ? pitchesData.filter((p) => p.status === 'ACTIVE')
          : [];
        setPitches(activePitches);
        setTimeSlots(slotsData || []);

        if (activePitches.length > 0) {
          setSelectedPitchId(activePitches[0].id);
        }
      })
      .catch(() => {
        if (isMounted) setServerError('Không thể tải danh sách sân bóng hoặc khung giờ.');
      })
      .finally(() => {
        if (isMounted) setLoadingInitial(false);
      });

    return () => {
      isMounted = false;
    };
  }, [isOpen]);

  // 2. Tải Schedule Grid theo ngày đá
  useEffect(() => {
    if (!isOpen || !bookingDate) return;

    let isMounted = true;
    setLoadingSlots(true);

    pitchService
      .getScheduleGrid(bookingDate)
      .then((grid) => {
        if (isMounted) setScheduleGrid(grid);
      })
      .catch(() => {
        if (isMounted) setScheduleGrid(null);
      })
      .finally(() => {
        if (isMounted) setLoadingSlots(false);
      });

    return () => {
      isMounted = false;
    };
  }, [isOpen, bookingDate]);

  useEffect(() => {
    setSelectedSlotId('');
    setSlotError(null);
  }, [selectedPitchId, bookingDate]);

  const currentPitch = pitches.find((p) => p.id === Number(selectedPitchId));

  // Kiểm tra thời gian ca đá so với hiện tại
  const currentTimeStr = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`;
  const isToday = bookingDate === todayStr;

  const slotOptions = timeSlots.map((slot) => {
    const isBooked = scheduleGrid?.bookings?.some(
      (b) =>
        b.pitchId === Number(selectedPitchId) &&
        b.timeSlotId === slot.id &&
        b.status !== 'CANCELLED' &&
        b.status !== 'REFUNDED'
    );

    const isPassed = isToday && slot.endTime <= currentTimeStr;

    const priceObj = scheduleGrid?.prices?.find(
      (p) => p.pitchTypeId === currentPitch?.pitchType?.id && p.isPeakHour === slot.isPeakHour
    );
    const price = priceObj?.price ?? 300000;

    return {
      ...slot,
      isBooked: Boolean(isBooked),
      isPassed: Boolean(isPassed),
      price,
    };
  });

  const currentSlot = slotOptions.find((s) => s.id === Number(selectedSlotId));
  const slotPrice = currentSlot?.price || 0;
  const depositAmount = Math.round(slotPrice * 0.3);
  const paidAmount = paymentType === 'FULL_PAYMENT' ? slotPrice : depositAmount;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setServerError(null);

    let hasError = false;

    if (!guestName.trim()) {
      setNameError('Vui lòng nhập tên khách hàng');
      hasError = true;
    } else {
      setNameError(null);
    }

    if (!guestPhone.trim()) {
      setPhoneError('Vui lòng nhập số điện thoại');
      hasError = true;
    } else if (!VIETNAM_PHONE_REGEX.test(guestPhone.trim())) {
      setPhoneError('SĐT không hợp lệ (cần 10 số, đầu 03, 05, 07, 08, 09)');
      hasError = true;
    } else {
      setPhoneError(null);
    }

    if (!selectedSlotId) {
      setSlotError('Vui lòng chọn một khung giờ thi đấu');
      hasError = true;
    } else {
      setSlotError(null);
    }

    if (hasError) return;

    const payload: CounterBookingCreationPayload = {
      pitchId: Number(selectedPitchId),
      timeSlotId: Number(selectedSlotId),
      bookingDate,
      guestName: guestName.trim(),
      guestPhone: guestPhone.trim(),
      paidAmount,
      paymentType,
      paymentMethod,
    };

    setSubmitting(true);
    try {
      await adminBookingService.createCounterBooking(payload);
      showToast('Tạo đơn đặt sân tại quầy thành công!', 'success');
      onSuccess();
      onClose();
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Không thể tạo đơn tại quầy. Ca đá có thể đã có người đặt.';
      setServerError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  if (!isOpen) return null;

  return (
    <ModalOverlay onClose={onClose} maxWidth="500px">
      {/* Header */}
      <div className="flex justify-between items-center mb-5" style={{ flexShrink: 0 }}>
        <h2 className="text-xl font-bold">Tạo Đơn Tại Quầy</h2>
        <button onClick={onClose} className="text-muted hover:text-[var(--color-text-base)]">
          <X size={24} />
        </button>
      </div>

      {loadingInitial ? (
        <div className="flex flex-col items-center justify-center py-10 text-muted">
          <Loader2 className="animate-spin mb-2" size={32} />
          <p className="text-sm">Đang tải danh mục sân bóng...</p>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="flex flex-col" style={{ flex: 1 }}>
          {/* Lỗi máy chủ nếu có */}
          {serverError && (
            <div className="p-3 mb-4 text-xs rounded-md bg-red-50 text-danger border border-red-200">
              {serverError}
            </div>
          )}

          <div className="grid gap-4 mb-5" style={{ overflowY: 'auto', paddingRight: '0.25rem' }}>
            {/* Tên khách hàng */}
            <div>
              <label className="block text-sm font-semibold mb-2">Tên khách hàng</label>
              <input
                type="text"
                className="w-full"
                placeholder="Nhập tên khách..."
                value={guestName}
                onChange={(e) => {
                  setGuestName(e.target.value);
                  if (nameError) setNameError(null);
                }}
                onFocus={(e) => {
                  if (e.target.value === 'Khách vãng lai') setGuestName('');
                }}
                onBlur={(e) => {
                  if (e.target.value.trim() === '') setGuestName('Khách vãng lai');
                }}
                style={{
                  padding: '0.6rem 1rem',
                  border: nameError ? '1px solid var(--color-danger)' : '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-md)',
                  background: 'var(--color-bg-base)',
                  outline: 'none',
                }}
              />
              {nameError && <span className="text-xs text-danger mt-1 block">{nameError}</span>}
            </div>

            {/* Số điện thoại */}
            <div>
              <label className="block text-sm font-semibold mb-2">Số điện thoại</label>
              <input
                type="text"
                className="w-full"
                placeholder="Nhập SĐT khách..."
                value={guestPhone}
                onChange={(e) => {
                  setGuestPhone(e.target.value);
                  if (phoneError) setPhoneError(null);
                }}
                style={{
                  padding: '0.6rem 1rem',
                  border: phoneError ? '1px solid var(--color-danger)' : '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-md)',
                  background: 'var(--color-bg-base)',
                  outline: 'none',
                }}
              />
              {phoneError && <span className="text-xs text-danger mt-1 block">{phoneError}</span>}
            </div>

            {/* Sân bóng và Ngày đá */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-semibold mb-2">Chọn Sân</label>
                <select
                  className="w-full"
                  style={{
                    padding: '0.6rem 1rem',
                    border: '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--color-bg-base)',
                    outline: 'none',
                  }}
                  value={selectedPitchId}
                  onChange={(e) => setSelectedPitchId(Number(e.target.value))}
                >
                  {pitches.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({p.pitchType?.name || 'Sân bóng'})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-semibold mb-2">Ngày đá</label>
                <input
                  type="date"
                  className="w-full"
                  style={{
                    padding: '0.6rem 1rem',
                    border: '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--color-bg-base)',
                    outline: 'none',
                  }}
                  value={bookingDate}
                  min={todayStr}
                  onChange={(e) => setBookingDate(e.target.value)}
                />
              </div>
            </div>

            {/* Khung giờ trống */}
            <div>
              <div className="flex justify-between items-center mb-2">
                <label className="text-sm font-semibold">Khung giờ trống</label>
                {loadingSlots && (
                  <span className="text-xs text-muted flex items-center gap-1">
                    <Loader2 size={12} className="animate-spin" /> Đang tải...
                  </span>
                )}
              </div>
              <select
                className="w-full"
                style={{
                  padding: '0.6rem 1rem',
                  border: slotError ? '1px solid var(--color-danger)' : '1px solid var(--color-border)',
                  borderRadius: 'var(--radius-md)',
                  background: 'var(--color-bg-base)',
                  outline: 'none',
                }}
                value={selectedSlotId}
                onChange={(e) => {
                  setSelectedSlotId(Number(e.target.value));
                  if (slotError) setSlotError(null);
                }}
              >
                <option value="">-- Chọn khung giờ trống --</option>
                {slotOptions.map((slot) => {
                  let statusText = '';
                  if (slot.isBooked) statusText = ' (Đã có người đặt)';
                  else if (slot.isPassed) statusText = ' (Đã qua giờ)';

                  return (
                    <option
                      key={slot.id}
                      value={slot.id}
                      disabled={slot.isBooked || slot.isPassed}
                    >
                      {slot.startTime} - {slot.endTime} ({formatPrice(slot.price)}){statusText}
                    </option>
                  );
                })}
              </select>

              {slotError && <span className="text-xs text-danger mt-1 block">{slotError}</span>}
            </div>

            {/* Tiền thanh toán / Đặt cọc & Phương thức */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-semibold mb-2">Tiền thanh toán / Đặt cọc</label>
                <select
                  className="w-full"
                  style={{
                    padding: '0.6rem 1rem',
                    border: '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--color-bg-base)',
                    outline: 'none',
                  }}
                  value={paymentType}
                  onChange={(e) => setPaymentType(e.target.value as PaymentType)}
                >
                  <option value="DEPOSIT_30">
                    Cọc 30% {selectedSlotId ? `(${formatPrice(depositAmount)})` : ''}
                  </option>
                  <option value="FULL_PAYMENT">
                    Toàn bộ 100% {selectedSlotId ? `(${formatPrice(slotPrice)})` : ''}
                  </option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-semibold mb-2">Phương thức</label>
                <select
                  className="w-full"
                  style={{
                    padding: '0.6rem 1rem',
                    border: '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--color-bg-base)',
                    outline: 'none',
                  }}
                  value={paymentMethod}
                  onChange={(e) => setPaymentMethod(e.target.value as PaymentMethodCode)}
                >
                  <option value="CASH">Tiền mặt</option>
                  <option value="VIETQR">Chuyển khoản</option>
                </select>
              </div>
            </div>
          </div>

          {/* Nút bấm Tạo Đơn */}
          <div
            style={{
              flexShrink: 0,
              paddingTop: '1rem',
              marginTop: 'auto',
              borderTop: '1px solid var(--color-border)',
            }}
          >
            <button
              type="submit"
              className="btn btn-primary w-full flex items-center justify-center gap-2"
              disabled={submitting}
              style={{ height: '42px', fontSize: '1rem' }}
            >
              {submitting && <Loader2 size={16} className="animate-spin" />}
              Xác nhận Tạo Đơn
            </button>
          </div>
        </form>
      )}
    </ModalOverlay>
  );
};
