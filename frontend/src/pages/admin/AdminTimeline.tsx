import React, { useState, useMemo, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  Calendar,
  Play,
  Square,
  Check,
  Loader2
} from 'lucide-react';
import { useScheduleWebSocket } from '../../hooks/useScheduleWebSocket.ts';
import { usePitches, useTimeSlots, usePitchTypes } from '../../hooks/queries/usePitchQueries.ts';
import { useAdminTimeline, useApproveBooking, useCheckInBooking } from '../../hooks/queries/useBookingQueries.ts';
import type { Pitch } from '../../types/pitch.ts';
import type { AdminTimelineSlotResponse } from '../../types/adminTimeline.ts';
import { TimelineDetailModal, canCheckInSlot } from '../../components/admin/timeline/index.ts';
import { showToast, showAlert, showConfirm } from '../../utils/toast.ts';

const getLocalDateString = (d: Date = new Date()) => {
  return new Date(d.getTime() - (d.getTimezoneOffset() * 60000)).toISOString().split('T')[0];
};

const AdminTimeline: React.FC = () => {
  // 1. Đồng bộ Ngày & Bộ lọc loại sân với URL query params (searchParams)
  const [searchParams, setSearchParams] = useSearchParams();

  const selectedDate = searchParams.get('date') || getLocalDateString();
  const selectedPitchType = searchParams.get('type') || 'all';

  // Tự động đồng bộ ngày mặc định lên URL nếu chưa có
  useEffect(() => {
    if (!searchParams.get('date')) {
      const nextParams: Record<string, string> = { date: selectedDate };
      if (selectedPitchType !== 'all') {
        nextParams.type = selectedPitchType;
      }
      setSearchParams(nextParams, { replace: true });
    }
  }, [searchParams, selectedDate, selectedPitchType, setSearchParams]);

  const handleDateChange = (newDate: string) => {
    const nextParams: Record<string, string> = { date: newDate };
    if (selectedPitchType !== 'all') {
      nextParams.type = selectedPitchType;
    }
    setSearchParams(nextParams, { replace: true });
  };

  const handlePitchTypeChange = (newType: string) => {
    const nextParams: Record<string, string> = { date: selectedDate };
    if (newType !== 'all') {
      nextParams.type = newType;
    }
    setSearchParams(nextParams, { replace: true });
  };

  // 2. React Query: Tải dữ liệu ma trận ca đá Real-time
  const { data: pitchTypes = [] } = usePitchTypes();
  const { data: rawSlots = [] } = useTimeSlots();
  const timeSlots = useMemo(() => {
    return [...rawSlots].sort((a, b) => a.startTime.localeCompare(b.startTime));
  }, [rawSlots]);

  const { data: pitchRes, isLoading: isLoadingPitches } = usePitches(selectedPitchType);
  const pitches: Pitch[] = useMemo(() => {
    return Array.isArray(pitchRes) ? pitchRes : (pitchRes as any)?.items || [];
  }, [pitchRes]);

  const { data: timelineData, isLoading: isLoadingTimeline } = useAdminTimeline(selectedDate, selectedPitchType);
  const timelineSlots = timelineData?.bookings || [];

  const loading = isLoadingPitches || isLoadingTimeline;

  // 3. Mutations: Duyệt đơn & Nhận sân (Check-in)
  const approveMutation = useApproveBooking();
  const checkInMutation = useCheckInBooking();

  const handleApprove = async (bookingId: number) => {
    try {
      await approveMutation.mutateAsync(bookingId);
      showToast('Duyệt đơn đặt sân thành công!', 'success');
      setDetailSlot(null);
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Không thể duyệt đơn đặt sân';
      showToast(msg, 'error');
    }
  };

  const handleCheckIn = async (slot: AdminTimelineSlotResponse) => {
    if (!slot.bookingId) return;

    // 1. Kiểm tra điều kiện thời gian (>= startTime và đúng ngày)
    const check = canCheckInSlot(slot);
    if (!check.canCheckIn) {
      showAlert('Không thể nhận sân', check.reason || 'Chưa đủ điều kiện nhận sân', 'warning');
      return;
    }

    // 2. Xác nhận nhận sân
    const confirmed = await showConfirm(
      'Xác nhận nhận sân?',
      `Khách hàng sẽ nhận sân ${slot.pitchName} (ca ${slot.startTime?.substring(0, 5)} - ${slot.endTime?.substring(0, 5)}). Trạng thái chuyển thành Đang sử dụng.`,
      'Xác nhận nhận sân'
    );
    if (!confirmed) return;

    try {
      await checkInMutation.mutateAsync(slot.bookingId);
      showToast('Nhận sân thành công! Trận đấu đang diễn ra.', 'success');
      setDetailSlot(null);
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Không thể nhận sân';
      showToast(msg, 'error');
    }
  };

  // 4. Modal xem chi tiết ca đá
  const [detailSlot, setDetailSlot] = useState<AdminTimelineSlotResponse | null>(null);

  // Lắng nghe WebSocket để realtime update
  useScheduleWebSocket(selectedDate);

  // Tìm thông tin ca đá cho một Sân và Khung giờ cụ thể
  const getSlotData = (pitchId: number, timeSlotId: number): AdminTimelineSlotResponse | undefined => {
    return timelineSlots.find((s) => s.pitchId === pitchId && s.timeSlotId === timeSlotId);
  };

  return (
    <div className="timeline-page-container">
      {/* Khung Card chính của trang Quản lý Ca Đá */}
      <div className="timeline-card">
        {/* Thanh công cụ: Chú thích trạng thái (trái) & Bộ lọc ngày / sân (phải) */}
        <div className="timeline-header-toolbar">
          {/* Trái: Chú giải trạng thái (Status Legends) */}
          <div className="timeline-legend-list">
            <div className="timeline-legend-item legend-available">
              <span className="timeline-legend-box available" />
              <span>Trống</span>
            </div>
            <div className="timeline-legend-item legend-confirmed">
              <span className="timeline-legend-box confirmed" />
              <span>Đã xác nhận</span>
            </div>
            <div className="timeline-legend-item legend-in-progress">
              <span className="timeline-legend-box in-progress" />
              <span>Đang đá</span>
            </div>
            <div className="timeline-legend-item legend-waiting">
              <span className="timeline-legend-box waiting" />
              <span>Chờ duyệt</span>
            </div>
            <div className="timeline-legend-item legend-cancel">
              <span className="timeline-legend-box cancel-requested" />
              <span>Yêu cầu hủy</span>
            </div>
          </div>

          {/* Phải: Bộ lọc loại sân & chọn ngày */}
          <div className="timeline-actions-bar">
            {/* Dropdown lọc theo loại sân */}
            <select
              value={selectedPitchType}
              onChange={(e) => handlePitchTypeChange(e.target.value)}
              className="timeline-select-pitch"
              aria-label="Lọc theo loại sân"
            >
              <option value="all">Tất cả sân</option>
              {pitchTypes.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </select>

            {/* Ô chọn ngày đá */}
            <div className="timeline-date-picker-wrapper">
              <Calendar size={18} className="timeline-date-icon" />
              <input
                type="date"
                value={selectedDate}
                onChange={(e) => handleDateChange(e.target.value)}
                className="timeline-date-input"
                aria-label="Chọn ngày quản lý ca đá"
              />
            </div>
          </div>
        </div>

        {/* Bảng Ma Trận Ca Đá (Timeline Grid Table) */}
        <div className="timeline-grid-wrapper">
          {loading ? (
            <div className="timeline-loading-container">
              <Loader2 size={32} className="animate-spin text-emerald-500" />
              <p>Đang tải dữ liệu ca đá...</p>
            </div>
          ) : (
            <table className="timeline-grid-table">
              <thead>
                <tr>
                  {/* Cột 1 cố định bên trái (Sticky): Tên Sân */}
                  <th className="timeline-th-sticky-pitch">
                    Sân / Giờ
                  </th>
                  {/* Các cột Khung giờ */}
                  {timeSlots.map((slot) => (
                    <th key={slot.id} className="timeline-th-timeslot">
                      <div className="timeline-slot-header-box">
                        <span className="timeline-slot-start">{slot.startTime?.substring(0, 5)}</span>
                        <span className="timeline-slot-end">{slot.endTime?.substring(0, 5)}</span>
                      </div>
                    </th>
                  ))}
                </tr>
              </thead>

              <tbody>
                {pitches.length === 0 ? (
                  <tr>
                    <td colSpan={timeSlots.length + 1} className="timeline-empty-notice">
                      Không tìm thấy sân bóng nào phù hợp với bộ lọc
                    </td>
                  </tr>
                ) : (
                  pitches.map((pitch) => {
                    const isPitchMaintenance = pitch.status === 'MAINTENANCE' || pitch.status === 'INACTIVE';

                    return (
                      <tr key={pitch.id} className="timeline-row">
                        {/* Cột Tên Sân cố định bên trái (Sticky) */}
                        <td className="timeline-td-sticky-pitch">
                          <div className="timeline-pitch-title" title={pitch.name}>
                            {pitch.name}
                          </div>
                          <span className="timeline-pitch-badge">
                            {pitch.pitchType?.name || 'Sân 5'}
                          </span>
                        </td>

                        {/* Các ô ca đá theo từng Khung giờ */}
                        {timeSlots.map((slot) => {
                          const slotData = getSlotData(pitch.id, slot.id);
                          const status = isPitchMaintenance ? 'MAINTENANCE' : (slotData?.status || 'AVAILABLE');

                          return (
                            <td key={slot.id} className={`timeline-td-slot status-${status.toLowerCase()}`}>
                              {/* 1. Sân đang bảo trì */}
                              {status === 'MAINTENANCE' && (
                                <div className="timeline-cell-maintenance">
                                  Bảo trì
                                </div>
                              )}

                              {/* 2. Ô sân trống */}
                              {status === 'AVAILABLE' && (
                                <div className="timeline-cell-available">
                                  Trống
                                </div>
                              )}

                              {/* 3. Đã xác nhận -> HIỆN NÚT CHECK-IN "Nhận sân" */}
                              {status === 'CONFIRMED' && (
                                <button
                                  type="button"
                                  onClick={() => handleCheckIn(slotData!)}
                                  className="timeline-btn-checkin"
                                  title={`Đơn #${slotData?.bookingCode || slotData?.bookingId} - Đã xác nhận. Bấm để nhận sân`}
                                >
                                  <Play size={11} className="fill-current" />
                                  <span>Nhận sân</span>
                                </button>
                              )}

                              {/* 4. Đang sử dụng sân (IN_PROGRESS) -> HIỆN NÚT CHECK-OUT "Trả sân" */}
                              {status === 'IN_PROGRESS' && (
                                <button
                                  type="button"
                                  onClick={() => setDetailSlot(slotData || null)}
                                  className="timeline-btn-checkout"
                                  title={`Đơn #${slotData?.bookingCode || slotData?.bookingId} - Đang sử dụng. Bấm xem chi tiết`}
                                >
                                  <Square size={10} className="fill-current" />
                                  <span>Trả sân</span>
                                </button>
                              )}

                              {/* 5. Khách gửi Yêu cầu hủy (WAITING_CANCELLATION) -> BẤM VÀO HIỆN XÁC NHẬN */}
                              {status === 'WAITING_CANCELLATION' && (
                                <button
                                  type="button"
                                  onClick={() => setDetailSlot(slotData || null)}
                                  className="timeline-btn-cancel-req"
                                  title={`Đơn #${slotData?.bookingCode || slotData?.bookingId} - Khách xin hủy. Bấm xem chi tiết`}
                                >
                                  <span>Yêu cầu hủy</span>
                                </button>
                              )}

                              {/* 6. Chờ duyệt (WAITING_APPROVAL) */}
                              {status === 'WAITING_APPROVAL' && (
                                <div
                                  className="timeline-badge-waiting"
                                  onClick={() => setDetailSlot(slotData || null)}
                                  title="Đơn đang chờ duyệt. Bấm để duyệt đơn"
                                >
                                  Chờ duyệt
                                </div>
                              )}

                              {/* 7. Ca đá đã hoàn tất (COMPLETED) */}
                              {status === 'COMPLETED' && (
                                <div
                                  className="timeline-cell-completed"
                                  onClick={() => setDetailSlot(slotData || null)}
                                  title="Ca đá đã hoàn tất. Bấm để xem chi tiết"
                                >
                                  <Check size={13} />
                                  <span>Xong</span>
                                </div>
                              )}

                              {/* 8. Đang giữ chỗ thanh toán cọc (PENDING_HOLD) */}
                              {status === 'PENDING_HOLD' && (
                                <div className="timeline-badge-pending-hold">
                                  Giữ chỗ
                                </div>
                              )}
                            </td>
                          );
                        })}
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Modal Chi tiết ca đá & Thao tác Duyệt / Nhận sân */}
      {detailSlot && (
        <TimelineDetailModal
          slot={detailSlot}
          onClose={() => setDetailSlot(null)}
          onApprove={handleApprove}
          onCheckIn={handleCheckIn}
          isApproving={approveMutation.isPending}
          isCheckingIn={checkInMutation.isPending}
        />
      )}
    </div>
  );
};

export default AdminTimeline;
