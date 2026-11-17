import React, { useState } from 'react';
import { Calendar } from 'lucide-react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { usePitches, useTimeSlots, useScheduleGrid, usePitchTypes } from '../hooks/queries/usePitchQueries.ts';
import { useScheduleWebSocket } from '../hooks/useScheduleWebSocket.ts';
import { useDocumentTitle } from '../hooks/useDocumentTitle.ts';

const getLocalDateString = (d: Date = new Date()) => {
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().split('T')[0];
};

const BookPitch: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  useDocumentTitle(
    'Tra Cứu Lịch & Đặt Sân Bóng Đá Trực Quan | Soccer365',
    'Xem lịch sân bóng đá trống theo ngày và loại sân (sân mini 5 người, sân 7 người). Chọn khung giờ vàng và đặt lịch giữ chỗ nhanh chóng chỉ trong 2 phút.'
  );

  const [selectedDate, setSelectedDate] = useState<string>(
    searchParams.get('date') || getLocalDateString()
  );
  const [selectedPitchType, setSelectedPitchType] = useState<string>(
    searchParams.get('type') || 'all'
  );

  const { data: pitches = [], isLoading: isLoadingPitches } = usePitches(selectedPitchType);
  const { data: timeSlots = [], isLoading: isLoadingSlots } = useTimeSlots();
  const { data = { bookings: [], prices: [] }, isFetching: isFetchingGrid } = useScheduleGrid(
    selectedDate,
    selectedPitchType
  );

  // Lắng nghe WebSocket để realtime update
  useScheduleWebSocket(selectedDate);
  const { data: pitchTypes = [], isLoading: isLoadingPitchTypes } = usePitchTypes();

  const isLoading = isLoadingPitches || isLoadingSlots || isFetchingGrid || isLoadingPitchTypes;

  const formatPrice = (price: number) => {
    // Rút gọn giá (VD: 250.000đ -> 250k) để giao diện không bị vỡ trên các ô nhỏ
    if (price >= 1000) {
      return price / 1000 + 'k';
    }
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const isSlotInPast = (dateStr: string, endTime: string) => {
    const now = new Date();
    const todayStr = getLocalDateString(now);

    if (dateStr < todayStr) return true;

    if (dateStr === todayStr) {
      let [hours, minutes] = endTime.split(':').map(Number);
      if (hours === 0 && minutes === 0) {
        hours = 24;
      }
      const slotTime = new Date();
      slotTime.setHours(hours, minutes, 0, 0);
      return slotTime < now;
    }

    return false;
  };

  const getSlotStatus = (pitch: any, timeSlotId: number) => {
    if (pitch.status === 'MAINTENANCE') return 'maintenance';

    const bookings = Array.isArray(data) ? data : data.bookings || [];
    const gridItem = bookings.find((g: any) => g.pitchId === pitch.id && g.timeSlotId === timeSlotId);

    if (!gridItem) return 'available';
    if (gridItem.status === 'PENDING_HOLD') return 'pending_hold';
    if (gridItem.status === 'MAINTENANCE') return 'maintenance';

    return 'booked';
  };

  const getPrice = (pitchTypeId: number, isPeakHour: boolean) => {
    const prices = Array.isArray(data) ? [] : data.prices || [];
    const priceItem = prices.find((p: any) => p.pitchTypeId === pitchTypeId && p.isPeakHour === isPeakHour);
    return priceItem ? priceItem.price : 0;
  };

  return (
    <div className="animate-fade-in pt-8 px-4 md:px-8 mx-auto" style={{ maxWidth: '1600px' }}>
      <section aria-label="Đầu trang đặt sân">
        <div
          className="mb-8 shadow-lg"
          style={{
            borderRadius: '1rem',
            background:
              'linear-gradient(135deg, rgba(5,150,105,0.92) 0%, rgba(16,185,129,0.88) 50%, rgba(6,182,212,0.92) 100%), url("https://images.unsplash.com/photo-1518605368461-1ee7e53f0b2f?q=80&w=2070&auto=format&fit=crop") center/cover no-repeat',
            color: 'white',
            padding: '3rem 2rem',
            textAlign: 'center',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <h1 className="text-2xl md:text-4xl font-bold mb-3 flex items-center justify-center gap-2 md:gap-3 text-white">
            <Calendar size={32} aria-hidden="true" />
            <span>Tra Cứu Lịch & Đặt Sân</span>
          </h1>
          <p className="text-base md:text-lg text-white opacity-95">
            Chọn ngày và sân bóng phù hợp với đội bóng của bạn để tiến hành giữ chỗ trực tuyến.
          </p>
        </div>

        <div className="card" style={{ minWidth: 0 }}>
          {/* Header Controls: Cụm Chú Thích (2 hàng) bên trái & Cụm Bộ Lọc bên phải */}
            <div
              className="flex flex-col-reverse md:flex-row md:items-start justify-between gap-4"
              style={{ minWidth: 0, marginBottom: '2.0rem' }}
            >
              {/* Cụm Chú giải trạng thái (2 hàng luôn đi cùng nhau, không bị chia tách khi co màn hình) */}
              <div
                className="flex flex-col gap-4 text-sm font-medium md:pt-2.5"
                aria-label="Chú giải trạng thái ô đặt sân"
              >
                {/* Hàng 1 chú thích */}
                <div className="flex items-center flex-wrap gap-4 sm:gap-5">
                  <div className="flex items-center gap-2 whitespace-nowrap">
                    <span
                      style={{
                        width: 16,
                        height: 16,
                        flexShrink: 0,
                        borderRadius: 4,
                        border: '1px solid #047857',
                        backgroundColor: 'var(--color-primary-light)',
                      }}
                      aria-hidden="true"
                    />
                    <span style={{ color: 'var(--color-text-base)' }}>Còn trống</span>
                  </div>
                  <div className="flex items-center gap-2 whitespace-nowrap">
                    <span
                      style={{
                        width: 16,
                        height: 16,
                        flexShrink: 0,
                        borderRadius: 4,
                        border: '1px solid #b45309',
                        backgroundColor: 'rgba(245, 158, 11, 0.15)',
                      }}
                      aria-hidden="true"
                    />
                    <span style={{ color: '#92400e', fontWeight: 600 }}>Giờ vàng</span>
                  </div>
                  <div className="flex items-center gap-2 whitespace-nowrap">
                    <span
                      style={{
                        width: 16,
                        height: 16,
                        flexShrink: 0,
                        borderRadius: 4,
                        backgroundColor: 'rgba(239, 68, 68, 0.15)',
                        border: '1px solid #b91c1c',
                      }}
                      aria-hidden="true"
                    />
                    <span style={{ color: '#b91c1c', fontWeight: 600 }}>Đã đặt</span>
                  </div>
                </div>

                {/* Hàng 2 chú thích */}
                <div className="flex items-center flex-wrap gap-4 sm:gap-5">
                  <div className="flex items-center gap-2 whitespace-nowrap">
                    <span
                      style={{
                        width: 16,
                        height: 16,
                        flexShrink: 0,
                        borderRadius: 4,
                        backgroundColor: 'rgba(245, 158, 11, 0.12)',
                        border: '1px solid #b45309',
                      }}
                      aria-hidden="true"
                    />
                    <span style={{ color: '#b45309', fontWeight: 600 }}>Chờ cọc</span>
                  </div>
                  <div className="flex items-center gap-2 whitespace-nowrap">
                    <span
                      style={{
                        width: 16,
                        height: 16,
                        flexShrink: 0,
                        borderRadius: 4,
                        backgroundColor: 'var(--color-bg-base)',
                        border: '1px dashed var(--color-border)',
                      }}
                      aria-hidden="true"
                    />
                    <span style={{ color: 'var(--color-text-muted)' }}>Bảo trì</span>
                  </div>
                </div>
              </div>

              {/* Cụm Bộ lọc Loại sân & Ngày đá (luôn nằm cùng hàng ngang) */}
              <div className="flex items-center gap-3 sm:gap-4 flex-wrap shrink-0">
                {/* Pitch Type Filter */}
                <div
                  className="flex items-center justify-between gap-2 shadow-sm transition-all"
                  style={{
                    backgroundColor: 'var(--color-bg-surface)',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--color-border)',
                    padding: '0.65rem 1rem',
                  }}
                >
                  <label htmlFor="pitch-type-filter" className="text-muted text-sm font-medium whitespace-nowrap cursor-pointer">
                    Loại sân:
                  </label>
                  <select
                    id="pitch-type-filter"
                    aria-label="Chọn loại sân bóng đá"
                    value={selectedPitchType}
                    onChange={(e) => {
                      setSelectedPitchType(e.target.value);
                      setSearchParams({ type: e.target.value, date: selectedDate }, { replace: true });
                    }}
                    style={{
                      background: 'transparent',
                      border: 'none',
                      outline: 'none',
                      color: 'var(--color-text-base)',
                      fontFamily: 'inherit',
                      fontWeight: 'bold',
                      cursor: 'pointer',
                      paddingRight: '0.5rem',
                    }}
                  >
                    <option value="all">Tất cả sân</option>
                    {pitchTypes.map((type) => (
                      <option key={type.id} value={type.id.toString()}>
                        {type.name}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Date Filter */}
                <div
                  className="flex items-center justify-between gap-2 shadow-sm transition-all"
                  style={{
                    backgroundColor: 'var(--color-bg-surface)',
                    borderRadius: 'var(--radius-md)',
                    border: '1px solid var(--color-border)',
                    padding: '0.65rem 1rem',
                  }}
                  onClick={() => {
                    const input = document.getElementById('date-picker-input') as HTMLInputElement;
                    if (input && 'showPicker' in HTMLInputElement.prototype) {
                      try {
                        input.showPicker();
                      } catch (e) { }
                    }
                  }}
                >
                  <label htmlFor="date-picker-input" className="text-muted text-sm font-medium whitespace-nowrap cursor-pointer">
                    Ngày đá:
                  </label>
                  <input
                    id="date-picker-input"
                    aria-label="Chọn ngày đá bóng"
                    type="date"
                    min={getLocalDateString()}
                    value={selectedDate}
                    onChange={(e) => {
                      setSelectedDate(e.target.value);
                      setSearchParams({ type: selectedPitchType, date: e.target.value }, { replace: true });
                    }}
                    style={{
                      background: 'transparent',
                      border: 'none',
                      outline: 'none',
                      color: 'var(--color-text-base)',
                      fontFamily: 'inherit',
                      fontWeight: 'bold',
                      cursor: 'pointer',
                    }}
                    onClick={(e) => {
                      if ('showPicker' in HTMLInputElement.prototype) {
                        try {
                          (e.target as HTMLInputElement).showPicker();
                        } catch (err) { }
                      }
                    }}
                  />
                </div>
              </div>
            </div>

            {/* Matrix Gantt for Desktop */}
              <div
                className="hidden md:block matrix-container"
                style={{ overflowX: 'auto', WebkitOverflowScrolling: 'touch', marginTop: '1.5rem' }}
              >
                <div className="matrix-header" role="row">
                  <div className="matrix-cell matrix-pitch-name font-bold">Sân / Khung giờ</div>
                  {timeSlots.map((slot) => (
                    <div key={slot.id} className="matrix-cell font-semibold">
                      <div>
                        {slot.startTime} - {slot.endTime}
                      </div>
                    </div>
                  ))}
                </div>

                {isLoading ? (
                  <div className="flex justify-center items-center py-12">
                    <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
                  </div>
                ) : (
                  pitches.map((pitch) => (
                    <div key={pitch.id} className="matrix-row" role="row">
                      <div className="matrix-cell matrix-pitch-name" style={{ overflow: 'hidden' }}>
                        <div style={{ width: '100%' }}>
                          <div
                            title={pitch.name}
                            style={{
                              display: '-webkit-box',
                              WebkitLineClamp: 2,
                              WebkitBoxOrient: 'vertical',
                              overflow: 'hidden',
                              wordBreak: 'break-word',
                              fontWeight: 700,
                            }}
                          >
                            {pitch.name}
                          </div>
                          <div className="text-xs text-muted font-normal mt-1">{pitch.pitchType?.name || 'Sân bóng'}</div>
                        </div>
                      </div>

                      {timeSlots.map((slot) => {
                        const status = getSlotStatus(pitch, slot.id);

                        if (status === 'maintenance') {
                          return (
                            <div key={slot.id} className="matrix-cell p-1">
                              <div
                                className="flex flex-col items-center justify-center font-medium"
                                style={{
                                  height: '48px',
                                  borderRadius: '6px',
                                  backgroundColor: 'var(--color-bg-base)',
                                  border: '1px dashed var(--color-border)',
                                  color: 'var(--color-text-muted)',
                                  cursor: 'not-allowed',
                                  padding: '0.25rem',
                                }}
                                aria-label={`Sân ${pitch.name}, khung giờ ${slot.startTime} - ${slot.endTime}, trạng thái: Đang bảo trì`}
                              >
                                Bảo trì
                              </div>
                            </div>
                          );
                        }

                        const isPeak = slot.isPeakHour;
                        const currentPrice =
                          getPrice(pitch.pitchType?.id, slot.isPeakHour) ||
                          (slot as any).basePrice ||
                          (isPeak ? 300000 : 250000);
                        const isPast = isSlotInPast(selectedDate, slot.endTime);

                        if (status === 'booked') {
                          return (
                            <div key={slot.id} className="matrix-cell p-1">
                              <div
                                className="flex flex-col items-center justify-center"
                                style={{
                                  height: '48px',
                                  borderRadius: '6px',
                                  backgroundColor: 'rgba(239, 68, 68, 0.08)',
                                  border: '1px solid #b91c1c',
                                  color: '#b91c1c',
                                  cursor: 'not-allowed',
                                  padding: '0.25rem',
                                }}
                                aria-label={`Sân ${pitch.name}, khung giờ ${slot.startTime} - ${slot.endTime}, trạng thái: Đã có người đặt`}
                              >
                                <span className="font-bold text-xs whitespace-nowrap">{formatPrice(currentPrice)}</span>
                                <span className="text-[10px] sm:text-xs mt-0.5 font-bold">Đã đặt</span>
                              </div>
                            </div>
                          );
                        }

                        if (status === 'pending_hold') {
                          return (
                            <div key={slot.id} className="matrix-cell p-1">
                              <div
                                className="flex flex-col items-center justify-center"
                                style={{
                                  height: '48px',
                                  borderRadius: '6px',
                                  backgroundColor: 'rgba(245, 158, 11, 0.1)',
                                  border: '1px solid #b45309',
                                  color: '#b45309',
                                  cursor: 'not-allowed',
                                  padding: '0.25rem',
                                }}
                                aria-label={`Sân ${pitch.name}, khung giờ ${slot.startTime} - ${slot.endTime}, trạng thái: Đang chờ thanh toán cọc`}
                              >
                                <span className="font-bold text-xs whitespace-nowrap">{formatPrice(currentPrice)}</span>
                                <span className="text-[10px] sm:text-xs mt-0.5 font-bold">Chờ cọc</span>
                              </div>
                            </div>
                          );
                        }

                        // Nếu ô trống nhưng đã quá giờ
                        if (isPast) {
                          return (
                            <div key={slot.id} className="matrix-cell p-1">
                              <div
                                className="flex flex-col items-center justify-center"
                                style={{
                                  height: '48px',
                                  borderRadius: '6px',
                                  backgroundColor: 'var(--color-bg-base)',
                                  border: '1px dashed var(--color-border)',
                                  color: '#4b5563',
                                  cursor: 'not-allowed',
                                  padding: '0.25rem',
                                }}
                                aria-label={`Sân ${pitch.name}, khung giờ ${slot.startTime} - ${slot.endTime}, trạng thái: Đã qua khung giờ`}
                              >
                                <span className="font-bold text-xs whitespace-nowrap">{formatPrice(currentPrice)}</span>
                                <span className="text-[10px] sm:text-xs mt-0.5 font-medium">Đã qua</span>
                              </div>
                            </div>
                          );
                        }

                        // Available (Trống có thể đặt)
                        const baseBg = isPeak ? 'rgba(245, 158, 11, 0.12)' : 'var(--color-primary-light)';
                        const baseBorder = isPeak ? '#b45309' : 'var(--color-primary)';
                        const textColor = isPeak ? '#92400e' : '#047857';

                        return (
                          <div key={slot.id} className="matrix-cell p-1">
                            <button
                              type="button"
                              tabIndex={0}
                              aria-label={`Đặt sân ${pitch.name}, khung giờ ${slot.startTime} đến ${slot.endTime}, giá ${formatPrice(currentPrice)}${isPeak ? ', Giờ vàng' : ', Khung giờ tiêu chuẩn'}`}
                              onClick={() => navigate(`/checkout/${slot.id}/${pitch.id}?date=${selectedDate}`)}
                              onKeyDown={(e) => {
                                if (e.key === 'Enter' || e.key === ' ') {
                                  e.preventDefault();
                                  navigate(`/checkout/${slot.id}/${pitch.id}?date=${selectedDate}`);
                                }
                              }}
                              className="flex flex-col items-center justify-center transition-all matrix-slot-inner w-full"
                              style={{
                                height: '48px',
                                borderRadius: '6px',
                                backgroundColor: baseBg,
                                border: `1.5px solid ${baseBorder}`,
                                cursor: 'pointer',
                                padding: '0.25rem',
                                color: textColor,
                              }}
                              title="Bấm hoặc nhấn Enter để đặt sân"
                            >
                              <span className="font-bold text-xs whitespace-nowrap">{formatPrice(currentPrice)}</span>
                              <span className="text-[10px] sm:text-xs mt-0.5 font-bold">
                                {isPeak ? 'Giờ vàng' : 'Trống'}
                              </span>
                            </button>
                          </div>
                        );
                      })}
                    </div>
                  ))
                )}
              </div>

              {/* Mobile Cards (Pitch list) */}
              <div className="md:hidden flex flex-col gap-6 mt-4">
                <div className="text-sm font-semibold text-muted px-1">
                  Hiển thị {pitches.length} sân bóng phù hợp
                </div>

                {isLoading ? (
                  <div className="flex justify-center py-8">
                    <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
                  </div>
                ) : (
                  pitches.map((pitch) => (
                    <article key={`mobile-${pitch.id}`} className="pitch-card">
                      <div
                        className="flex justify-between items-start mb-4 border-b pb-3 gap-3"
                        style={{ borderBottomColor: 'var(--color-border)' }}
                      >
                        <h3
                          className="font-bold text-lg flex-1"
                          style={{
                            color: 'var(--color-primary)',
                            display: '-webkit-box',
                            WebkitLineClamp: 2,
                            WebkitBoxOrient: 'vertical',
                            overflow: 'hidden',
                            wordBreak: 'break-word',
                            lineHeight: '1.3',
                          }}
                          title={pitch.name}
                        >
                          {pitch.name}
                        </h3>
                        <span className="badge badge-success text-sm whitespace-nowrap flex-shrink-0 mt-1">
                          {pitch.pitchType?.name || 'Sân bóng'}
                        </span>
                      </div>

                      <div className="time-pills-grid">
                        {timeSlots.map((slot) => {
                          const status = getSlotStatus(pitch, slot.id);
                          const isPeak = slot.isPeakHour;
                          const currentPrice =
                            getPrice(pitch.pitchType?.id, slot.isPeakHour) ||
                            (slot as any).basePrice ||
                            (isPeak ? 300000 : 250000);
                          const isPast = isSlotInPast(selectedDate, slot.endTime);

                          if (status === 'maintenance') {
                            return (
                              <div
                                key={slot.id}
                                className="time-pill maintenance"
                                aria-label={`Sân ${pitch.name}, ${slot.startTime} - ${slot.endTime}, Đang bảo trì`}
                              >
                                <span className="time-text">
                                  {slot.startTime} - {slot.endTime}
                                </span>
                                <span className="text-xs font-semibold mt-1">Bảo trì</span>
                              </div>
                            );
                          }

                          if (status === 'booked') {
                            return (
                              <div
                                key={slot.id}
                                className="time-pill booked"
                                aria-label={`Sân ${pitch.name}, ${slot.startTime} - ${slot.endTime}, Đã đặt`}
                              >
                                <span className="time-text">
                                  {slot.startTime} - {slot.endTime}
                                </span>
                                <div className="flex flex-col items-center mt-1">
                                  <span className="text-[10px] font-bold">{formatPrice(currentPrice)}</span>
                                  <span className="text-[10px] font-bold text-red-600">Đã đặt</span>
                                </div>
                              </div>
                            );
                          }

                          if (status === 'pending_hold') {
                            return (
                              <div
                                key={slot.id}
                                className="time-pill booked"
                                style={{
                                  borderColor: '#b45309',
                                  backgroundColor: 'rgba(245, 158, 11, 0.12)',
                                  color: '#b45309',
                                }}
                                aria-label={`Sân ${pitch.name}, ${slot.startTime} - ${slot.endTime}, Chờ cọc`}
                              >
                                <span className="time-text">
                                  {slot.startTime} - {slot.endTime}
                                </span>
                                <div className="flex flex-col items-center mt-1">
                                  <span className="text-[10px] font-bold">{formatPrice(currentPrice)}</span>
                                  <span className="text-[10px] font-bold">Chờ cọc</span>
                                </div>
                              </div>
                            );
                          }

                          if (isPast) {
                            return (
                              <div
                                key={slot.id}
                                className="time-pill maintenance"
                                aria-label={`Sân ${pitch.name}, ${slot.startTime} - ${slot.endTime}, Đã qua giờ`}
                              >
                                <span className="time-text">
                                  {slot.startTime} - {slot.endTime}
                                </span>
                                <span className="text-xs font-medium mt-1">Đã qua</span>
                              </div>
                            );
                          }

                          const mobileBaseBg = isPeak ? 'rgba(245, 158, 11, 0.1)' : 'var(--color-primary-light)';
                          const mobileBorder = isPeak ? '#b45309' : 'var(--color-primary)';
                          const mobileTextColor = isPeak ? '#92400e' : '#047857';

                          return (
                            <button
                              type="button"
                              tabIndex={0}
                              key={slot.id}
                              className={`time-pill ${isPeak ? 'is-peak' : ''}`}
                              onClick={() => navigate(`/checkout/${slot.id}/${pitch.id}?date=${selectedDate}`)}
                              onKeyDown={(e) => {
                                if (e.key === 'Enter' || e.key === ' ') {
                                  e.preventDefault();
                                  navigate(`/checkout/${slot.id}/${pitch.id}?date=${selectedDate}`);
                                }
                              }}
                              style={{ borderColor: mobileBorder, backgroundColor: mobileBaseBg }}
                              aria-label={`Đặt sân ${pitch.name}, khung giờ ${slot.startTime} đến ${slot.endTime}, giá ${formatPrice(currentPrice)}${isPeak ? ', Giờ vàng' : ''}`}
                            >
                              <span className="time-text">
                                {slot.startTime} - {slot.endTime}
                              </span>
                              <span className="price-text" style={{ color: mobileTextColor, fontWeight: 700 }}>
                                {formatPrice(currentPrice)} {isPeak ? '(Giờ vàng)' : ''}
                              </span>
                            </button>
                          );
                        })}
                      </div>
                    </article>
                  ))
                )}
              </div>
            </div>
          </section>
        </div>
        );
};

        export default BookPitch;
