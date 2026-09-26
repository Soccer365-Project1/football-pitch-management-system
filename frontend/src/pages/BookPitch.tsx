import React, { useState } from 'react';
import { Calendar } from 'lucide-react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { usePitches, useTimeSlots, useScheduleGrid, usePitchTypes } from '../hooks/queries/usePitchQueries.ts';

const BookPitch: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const [selectedDate, setSelectedDate] = useState<string>(
    searchParams.get('date') || new Date().toISOString().split('T')[0]
  );
  const [selectedPitchType, setSelectedPitchType] = useState<string>(
    searchParams.get('type') || 'all'
  );

  const { data: pitches = [], isLoading: isLoadingPitches } = usePitches(selectedPitchType);
  const { data: timeSlots = [], isLoading: isLoadingSlots } = useTimeSlots();
  const { data = { bookings: [], prices: [] }, isFetching: isFetchingGrid } = useScheduleGrid(selectedDate, selectedPitchType);
  const { data: pitchTypes = [], isLoading: isLoadingPitchTypes } = usePitchTypes();

  const isLoading = isLoadingPitches || isLoadingSlots || isFetchingGrid || isLoadingPitchTypes;

  const formatPrice = (price: number) => {
    // Rút gọn giá (VD: 250.000đ -> 250k) để giao diện không bị vỡ trên các ô nhỏ
    if (price >= 1000) {
      return (price / 1000) + 'k';
    }
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const isSlotInPast = (dateStr: string, endTime: string) => {
    const now = new Date();
    // Chuyển local time sang string YYYY-MM-DD an toàn hơn (tránh lệch múi giờ)
    const todayStr = new Date(now.getTime() - (now.getTimezoneOffset() * 60000)).toISOString().split('T')[0];
    
    if (dateStr < todayStr) return true;
    
    if (dateStr === todayStr) {
      let [hours, minutes] = endTime.split(':').map(Number);
      // Sửa lỗi: Nếu giờ kết thúc là 00:00, ta hiểu nó là 24:00 của ngày hôm nay
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
    
    const bookings = Array.isArray(data) ? data : (data.bookings || []);
    const gridItem = bookings.find(g => g.pitchId === pitch.id && g.timeSlotId === timeSlotId);
    
    if (!gridItem) return 'available';
    if (gridItem.status === 'PENDING_HOLD') return 'pending_hold';
    if (gridItem.status === 'MAINTENANCE') return 'maintenance';
    
    return 'booked';
  };

  const getPrice = (pitchTypeId: number, isPeakHour: boolean) => {
    const prices = Array.isArray(data) ? [] : (data.prices || []);
    const priceItem = prices.find(p => p.pitchTypeId === pitchTypeId && p.isPeakHour === isPeakHour);
    return priceItem ? priceItem.price : 0;
  };

  return (
    <div className="animate-fade-in pt-8 px-4 md:px-8 mx-auto" style={{ maxWidth: '1600px' }}>
      <div>
        <div
          className="mb-8 shadow-lg"
          style={{
            borderRadius: '1rem',
            background: 'linear-gradient(135deg, rgba(5,150,105,0.9) 0%, rgba(16,185,129,0.85) 50%, rgba(6,182,212,0.9) 100%), url("https://images.unsplash.com/photo-1518605368461-1ee7e53f0b2f?q=80&w=2070&auto=format&fit=crop") center/cover no-repeat',
            color: 'white',
            padding: '3rem 2rem',
            textAlign: 'center',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center'
          }}
        >
          <h1 className="text-2xl md:text-4xl font-bold mb-3 flex items-center justify-center gap-2 md:gap-3 text-white">
            <Calendar size={32} />
            <span>Đặt Sân</span>
          </h1>
          <p className="text-base md:text-lg" style={{ color: 'rgba(255, 255, 255, 0.9)' }}>Chọn ngày và sân bóng phù hợp với bạn.</p>
        </div>

        <div className="card" style={{ minWidth: 0 }}>
          <div className="flex flex-col md:flex-row md:items-center justify-between mb-6 gap-4" style={{ minWidth: 0 }}>
            {/* Legends */}
            <div className="flex items-center gap-5 md:gap-6 text-sm font-medium overflow-x-auto pb-2 no-scrollbar w-full md:w-auto" style={{ WebkitOverflowScrolling: 'touch' }}>
              <div className="flex items-center gap-2 whitespace-nowrap">
                <div style={{ width: 16, height: 16, flexShrink: 0, borderRadius: 4, border: '1px solid var(--color-primary)', backgroundColor: 'var(--color-primary-light)' }}></div>
                <span>Trống</span>
              </div>
              <div className="flex items-center gap-2 whitespace-nowrap">
                <div style={{ width: 16, height: 16, flexShrink: 0, borderRadius: 4, border: '1px solid #f59e0b', backgroundColor: 'rgba(245, 158, 11, 0.15)' }}></div>
                <span>Giờ vàng</span>
              </div>
              <div className="flex items-center gap-2 whitespace-nowrap">
                <div style={{ width: 16, height: 16, flexShrink: 0, borderRadius: 4, backgroundColor: 'rgba(239, 68, 68, 0.15)', border: '1px solid var(--color-danger)' }}></div>
                <span>Đã đặt</span>
              </div>
              <div className="flex items-center gap-2 whitespace-nowrap">
                <div style={{ width: 16, height: 16, flexShrink: 0, borderRadius: 4, backgroundColor: 'var(--color-bg-base)', border: '1px dashed var(--color-border)' }}></div>
                <span>Bảo trì</span>
              </div>
            </div>

            {/* Filters */}
            <div className="flex flex-col md:flex-row items-stretch md:items-center gap-4 w-full md:w-auto ml-auto">
              {/* Pitch Type Filter */}
              <label
                className="flex items-center justify-between gap-2 shadow-sm transition-all"
                style={{ backgroundColor: 'var(--color-bg-surface)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)', cursor: 'pointer', padding: '0.8rem 1.2rem' }}
              >
                <span className="text-muted text-sm font-medium">Loại sân:</span>
                <select
                  value={selectedPitchType}
                  onChange={(e) => {
                    setSelectedPitchType(e.target.value);
                    setSearchParams({ type: e.target.value, date: selectedDate }, { replace: true });
                  }}
                  style={{ background: 'transparent', border: 'none', outline: 'none', color: 'var(--color-text-base)', fontFamily: 'inherit', fontWeight: 'bold', cursor: 'pointer', paddingRight: '0.5rem' }}
                >
                  <option value="all">Tất cả</option>
                  {pitchTypes.map(type => (
                    <option key={type.id} value={type.id.toString()}>{type.name}</option>
                  ))}
                </select>
              </label>

              {/* Date Filter */}
              <div
                className="flex items-center justify-between gap-2 shadow-sm transition-all"
                style={{ backgroundColor: 'var(--color-bg-surface)', borderRadius: 'var(--radius-md)', border: '1px solid var(--color-border)', cursor: 'pointer', padding: '0.8rem 1.2rem' }}
                onClick={() => {
                  const input = document.getElementById('date-picker-input') as HTMLInputElement;
                  if (input && 'showPicker' in HTMLInputElement.prototype) {
                    try { input.showPicker(); } catch (e) { }
                  }
                }}
              >
                <span className="text-muted text-sm font-medium">Ngày đá:</span>
                <input
                  id="date-picker-input"
                  type="date"
                  min={new Date(new Date().getTime() - (new Date().getTimezoneOffset() * 60000)).toISOString().split('T')[0]}
                  value={selectedDate}
                  onChange={(e) => {
                    setSelectedDate(e.target.value);
                    setSearchParams({ type: selectedPitchType, date: e.target.value }, { replace: true });
                  }}
                  style={{ background: 'transparent', border: 'none', outline: 'none', color: 'var(--color-text-base)', fontFamily: 'inherit', fontWeight: 'bold', cursor: 'pointer' }}
                  onClick={(e) => {
                    if ('showPicker' in HTMLInputElement.prototype) {
                      try { (e.target as HTMLInputElement).showPicker(); } catch (err) { }
                    }
                  }}
                />
              </div>
            </div>
          </div>

          {/* Matrix Gantt for Desktop */}
          <div className="hidden md:block matrix-container" style={{ overflowX: 'auto', WebkitOverflowScrolling: 'touch' }}>
            <div className="matrix-header">
              <div className="matrix-cell matrix-pitch-name">Sân / Khung giờ</div>
              {timeSlots.map(slot => (
                <div key={slot.id} className="matrix-cell font-semibold">
                  <div>{slot.startTime} - {slot.endTime}</div>
                </div>
              ))}
            </div>

            {isLoading ? (
              <div className="flex justify-center items-center py-12">
                <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
              </div>
            ) : (
              pitches.map(pitch => (
                <div key={pitch.id} className="matrix-row">
                  <div className="matrix-cell matrix-pitch-name">
                    <div>
                      <div>{pitch.name}</div>
                      <div className="text-sm text-muted font-normal mt-1">{pitch.pitchType?.name || 'Sân bóng'}</div>
                    </div>
                  </div>

                  {timeSlots.map(slot => {
                    const status = getSlotStatus(pitch, slot.id);

                    if (status === 'maintenance') {
                      return (
                        <div key={slot.id} className="matrix-cell p-1">
                          <div className="flex flex-col items-center justify-center font-medium" style={{ height: '100%', borderRadius: '6px', backgroundColor: 'var(--color-bg-base)', border: '1px dashed var(--color-border)', color: 'var(--color-text-muted)', cursor: 'not-allowed', padding: '0.25rem' }}>
                            Bảo trì
                          </div>
                        </div>
                      );
                    }

                    const isPeak = slot.isPeakHour;
                    const currentPrice = getPrice(pitch.pitchType.id, slot.isPeakHour) || (slot as any).basePrice || (isPeak ? 300000 : 250000);
                    const isPast = isSlotInPast(selectedDate, slot.endTime);

                    if (status === 'booked') {
                      return (
                        <div key={slot.id} className="matrix-cell p-1">
                          <div className="flex flex-col items-center justify-center" style={{ height: '100%', borderRadius: '6px', backgroundColor: 'rgba(239, 68, 68, 0.08)', border: '1px solid rgba(239, 68, 68, 0.2)', color: 'var(--color-danger)', cursor: 'not-allowed', padding: '0.25rem' }}>
                            <span className="font-bold text-xs whitespace-nowrap">{formatPrice(currentPrice)}</span>
                            <span className="text-[10px] sm:text-xs mt-0.5 opacity-90 font-medium">Đã đặt</span>
                          </div>
                        </div>
                      );
                    }
                    
                    if (status === 'pending_hold') {
                      return (
                        <div key={slot.id} className="matrix-cell p-1">
                          <div className="flex flex-col items-center justify-center" style={{ height: '100%', borderRadius: '6px', backgroundColor: 'rgba(245, 158, 11, 0.08)', border: '1px solid rgba(245, 158, 11, 0.2)', color: '#f59e0b', cursor: 'not-allowed', padding: '0.25rem' }}>
                            <span className="font-bold text-xs whitespace-nowrap">{formatPrice(currentPrice)}</span>
                            <span className="text-[10px] sm:text-xs mt-0.5 opacity-90 font-medium">Chờ cọc</span>
                          </div>
                        </div>
                      );
                    }

                    // Nếu ô trống nhưng đã quá giờ
                    if (isPast) {
                      return (
                        <div key={slot.id} className="matrix-cell p-1">
                          <div className="flex flex-col items-center justify-center" style={{ height: '100%', borderRadius: '6px', backgroundColor: 'var(--color-bg-base)', border: '1px dashed var(--color-border)', color: 'var(--color-text-muted)', cursor: 'not-allowed', padding: '0.25rem', opacity: 0.7 }}>
                            <span className="font-bold text-xs whitespace-nowrap">{formatPrice(currentPrice)}</span>
                            <span className="text-[10px] sm:text-xs mt-0.5 opacity-90">Đã qua</span>
                          </div>
                        </div>
                      );
                    }

                    // Available
                    const baseBg = isPeak ? 'rgba(245, 158, 11, 0.1)' : 'var(--color-primary-light)';
                    const baseBorder = isPeak ? '#f59e0b' : 'var(--color-primary)';
                    const hoverBg = isPeak ? '#f59e0b' : 'var(--color-primary)';

                    return (
                      <div key={slot.id} className="matrix-cell p-1">
                        <div
                          className="flex flex-col items-center justify-center transition-all matrix-slot-inner"
                          onClick={() => navigate(`/checkout/${slot.id}/${pitch.id}`)}
                          style={{ height: '100%', borderRadius: '6px', backgroundColor: baseBg, border: `1px solid ${baseBorder}`, cursor: 'pointer', padding: '0.25rem' }}
                          title="Nhấn để đặt sân"
                          onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = hoverBg; e.currentTarget.style.color = 'white'; }}
                          onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = baseBg; e.currentTarget.style.color = 'inherit'; }}
                        >
                          <span className="font-bold text-xs whitespace-nowrap">{formatPrice(currentPrice)}</span>
                          <span className="text-[10px] sm:text-xs mt-0.5 opacity-90">{isPeak ? 'Giờ vàng' : 'Trống'}</span>
                        </div>
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
              Hiển thị {pitches.length} sân
            </div>

            {isLoading ? (
              <div className="flex justify-center py-8">
                <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
              </div>
            ) : (
              pitches.map(pitch => (
                <div key={`mobile-${pitch.id}`} className="pitch-card">
                  <div className="flex justify-between items-center mb-4 border-b pb-3" style={{ borderBottomColor: 'var(--color-border)' }}>
                    <h3 className="font-bold text-lg" style={{ color: 'var(--color-primary)' }}>{pitch.name}</h3>
                    <span className="badge badge-success text-sm">{pitch.pitchType?.name || 'Sân bóng'}</span>
                  </div>

                  <div className="time-pills-grid">
                    {timeSlots.map(slot => {
                      const status = getSlotStatus(pitch, slot.id);
                      const isPeak = slot.isPeakHour;

                      const currentPrice = getPrice(pitch.pitchType.id, slot.isPeakHour) || (slot as any).basePrice || (isPeak ? 300000 : 250000);
                      const isPast = isSlotInPast(selectedDate, slot.endTime);

                      if (status === 'maintenance') {
                        return (
                          <div key={slot.id} className="time-pill maintenance">
                            <span className="time-text">{slot.startTime}</span>
                            <span className="text-xs font-medium mt-1">Bảo trì</span>
                          </div>
                        );
                      }

                      if (status === 'booked') {
                        return (
                          <div key={slot.id} className="time-pill booked">
                            <span className="time-text">{slot.startTime}</span>
                            <div className="flex flex-col items-center mt-1">
                               <span className="text-[10px] font-bold">{formatPrice(currentPrice)}</span>
                               <span className="text-[10px] font-medium opacity-80">Đã đặt</span>
                            </div>
                          </div>
                        );
                      }

                      if (status === 'pending_hold') {
                        return (
                          <div key={slot.id} className="time-pill booked" style={{ borderColor: '#f59e0b', backgroundColor: 'rgba(245, 158, 11, 0.1)', color: '#f59e0b' }}>
                            <span className="time-text">{slot.startTime}</span>
                            <div className="flex flex-col items-center mt-1">
                               <span className="text-[10px] font-bold">{formatPrice(currentPrice)}</span>
                               <span className="text-[10px] font-medium opacity-80">Chờ cọc</span>
                            </div>
                          </div>
                        );
                      }

                      if (isPast) {
                        return (
                          <div key={slot.id} className="time-pill maintenance" style={{ opacity: 0.6 }}>
                            <span className="time-text">{slot.startTime}</span>
                            <span className="text-xs font-medium mt-1">Đã qua</span>
                          </div>
                        );
                      }

                      return (
                        <div
                          key={slot.id}
                          className={`time-pill ${isPeak ? 'is-peak' : ''}`}
                          onClick={() => navigate(`/checkout/${slot.id}/${pitch.id}`)}
                          style={isPeak ? { borderColor: '#f59e0b', backgroundColor: 'rgba(245, 158, 11, 0.05)' } : {}}
                        >
                          <span className="time-text">{slot.startTime}</span>
                          <span className="price-text" style={isPeak ? { color: '#f59e0b' } : {}}>{formatPrice(currentPrice)}</span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default BookPitch;
