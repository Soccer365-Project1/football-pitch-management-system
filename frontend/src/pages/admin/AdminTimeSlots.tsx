import React, { useState, useEffect, useCallback } from 'react';
import { Plus, Edit, Trash2, Clock, Loader2, Zap } from 'lucide-react';
import { timeslotService } from '../../services/timeslotService';
import type { TimeSlot, TimeSlotRequest } from '../../types/timeslot';
import { showToast, showAlert } from '../../utils/toast';
import { TimeSlotModal } from '../../components/admin/timeslot/TimeSlotModal';
import { DeleteConfirmModal } from '../../components/admin/timeslot/DeleteConfirmModal';

/**
 * =========================================================================================
 * COMPONENT CHÍNH: AdminTimeSlots (Màn hình Quản trị Khung giờ)
 * =========================================================================================
 * Quản lý danh sách các ca đá cố định trong ngày theo đặc tả SRS Mục 3.4.15 (Hình 3.62 - 3.68):
 * 1. Bảng danh sách khung giờ sắp xếp theo giờ bắt đầu, hiển thị đơn giá thật từ API Backend.
 * 2. Header được thiết kế gọn gàng, nút "+ Thêm khung giờ mới" co dãn hợp lý, không tràn viền.
 * 3. Thao tác Thêm mới và Sửa ca đá được tách gọn gàng vào component con `TimeSlotModal`.
 * 4. Thao tác Xóa được bảo vệ an toàn bởi component con `DeleteConfirmModal` và hỗ trợ Optimistic UI.
 * 5. Bọc comment tiếng Việt chi tiết cho mọi State, Handler và JSX để intern dễ theo dõi.
 */
const AdminTimeSlots: React.FC = () => {
  // ---------------------------------------------------------------------------------------
  // 1. Quản lý trạng thái Dữ liệu (useState cơ bản)
  // ---------------------------------------------------------------------------------------
  /** Danh sách các khung giờ hoạt động hiển thị trong bảng (kèm theo đơn giá thật từ API) */
  const [timeSlots, setTimeSlots] = useState<TimeSlot[]>([]);

  /** Cờ báo trạng thái đang tải dữ liệu danh sách từ server */
  const [loading, setLoading] = useState<boolean>(true);

  /** Cờ báo đang thực hiện thao tác gửi request (Thêm / Sửa / Xóa) để disable các nút bấm */
  const [submitting, setSubmitting] = useState<boolean>(false);

  // ---------------------------------------------------------------------------------------
  // 2. Quản lý trạng thái Mở/Đóng Modal
  // ---------------------------------------------------------------------------------------
  /** Cờ mở Modal "Thêm Khung Giờ Mới" */
  const [isAddModalOpen, setIsAddModalOpen] = useState<boolean>(false);

  /** Đối tượng khung giờ đang được chọn để Chỉnh sửa (null khi không mở modal sửa) */
  const [editSlot, setEditSlot] = useState<TimeSlot | null>(null);

  /** Đối tượng khung giờ đang được chọn để Xóa (null khi không mở modal xóa) */
  const [deleteSlot, setDeleteSlot] = useState<TimeSlot | null>(null);

  // ---------------------------------------------------------------------------------------
  // 3. Logic Tải Dữ Liệu Danh Sách Khung Giờ (API Backend Thật)
  // ---------------------------------------------------------------------------------------
  /**
   * Gọi API: `GET /api/v1/admin/time-slots?activeOnly=true`
   * - Mặc định chỉ lấy các ca đá đang hoạt động (`isActive = true`), loại bỏ các ca đã xóa mềm.
   * - Lọc bổ sung phía client (`slot.isActive !== false`) để đảm bảo an toàn dữ liệu tuyệt đối.
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

  // ---------------------------------------------------------------------------------------
  // 4. Handlers: Xử Lý Thêm Mới & Chỉnh Sửa Khung Giờ
  // ---------------------------------------------------------------------------------------
  /**
   * Xử lý tạo mới khung giờ: Nhận payload từ component `TimeSlotModal`
   */
  const handleAddSubmit = async (payload: TimeSlotRequest) => {
    setSubmitting(true);
    try {
      await timeslotService.createTimeSlot(payload);
      showToast('Thêm khung giờ mới thành công!', 'success');
      setIsAddModalOpen(false);
      await loadTimeSlots(); // Nạp lại danh sách mới
    } finally {
      setSubmitting(false);
    }
  };

  /**
   * Xử lý cập nhật khung giờ: Nhận payload từ component `TimeSlotModal`
   */
  const handleEditSubmit = async (payload: TimeSlotRequest) => {
    if (!editSlot) return;
    setSubmitting(true);
    try {
      await timeslotService.updateTimeSlot(editSlot.id, payload);
      showToast('Cập nhật khung giờ thành công!', 'success');
      setEditSlot(null);
      await loadTimeSlots(); // Nạp lại danh sách mới
    } finally {
      setSubmitting(false);
    }
  };

  // ---------------------------------------------------------------------------------------
  // 5. Handlers: Xử Lý Xóa Khung Giờ An Toàn & Optimistic Update
  // ---------------------------------------------------------------------------------------
  /**
   * Xóa khung giờ:
   * 1. Ứng dụng kỹ thuật Optimistic Update: Xóa ngay khỏi danh sách hiển thị trên UI.
   * 2. Nếu Backend trả về mã lỗi 3014 (đang có khách đặt lịch trong tương lai):
   *    Hiển thị thông báo SweetAlert2 cảnh báo lý do không thể xóa.
   */
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
      const errorMsg =
        err.response?.data?.message ||
        'Không thể xóa khung giờ do đang có khách hàng đặt lịch trong những ngày sắp tới!';
      showAlert('Không thể xóa khung giờ', errorMsg, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  // ---------------------------------------------------------------------------------------
  // 6. Giao Diện Chính (RENDER)
  // ---------------------------------------------------------------------------------------
  return (
    <div className="w-full admin-timeslots-container">
      {/* Khung Card chính chứa Header và Bảng dữ liệu */}
      <div
        className="card p-4 sm:p-6"
        style={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
          transform: 'none',
        }}
      >
        {/* ===============================================================================
            6.1. HEADER QUẢN TRỊ TRÊN CÙNG (.admin-timeslots-header)
            - Tiêu đề bên trái, Nút "+ Thêm khung giờ mới" bên phải gọn gàng
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
            onClick={() => setIsAddModalOpen(true)}
          >
            <Plus size={18} /> Thêm khung giờ mới
          </button>
        </div>

        {/* ===============================================================================
            6.2. BẢNG DỮ LIỆU KHUNG GIỜ (TABLE CÓ CUỘN NGANG & DỌC MƯỢT MÀ)
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
            /* Trạng thái 1: Đang tải dữ liệu */
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
            /* Trạng thái 2: Danh sách rỗng */
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
            /* Trạng thái 3: Bảng danh sách các khung giờ */
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

                      {/* Cột 3: Đơn giá Sân 5 thật từ Backend API (0 đ nếu chưa có ma trận giá) */}
                      <td className="p-4 font-semibold text-muted">
                        {slot.pricePitch5 != null && slot.pricePitch5 > 0
                          ? `${new Intl.NumberFormat('vi-VN').format(slot.pricePitch5)} đ`
                          : '0 đ'}
                      </td>

                      {/* Cột 4: Đơn giá Sân 7 thật từ Backend API (0 đ nếu chưa có ma trận giá) */}
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
          6.3. MODAL THÊM MỚI KHUNG GIỜ (GỌI TIMESLOTMODAL)
      =================================================================================== */}
      <TimeSlotModal
        isOpen={isAddModalOpen}
        isEdit={false}
        onClose={() => setIsAddModalOpen(false)}
        onSubmit={handleAddSubmit}
        submitting={submitting}
      />

      {/* ===================================================================================
          6.4. MODAL CHỈNH SỬA KHUNG GIỜ (GỌI TIMESLOTMODAL)
      =================================================================================== */}
      <TimeSlotModal
        isOpen={!!editSlot}
        isEdit={true}
        initialData={editSlot}
        onClose={() => setEditSlot(null)}
        onSubmit={handleEditSubmit}
        submitting={submitting}
      />

      {/* ===================================================================================
          6.5. MODAL XÁC NHẬN XÓA KHUNG GIỜ (GỌI DELETECONFIRMMODAL)
      =================================================================================== */}
      <DeleteConfirmModal
        slot={deleteSlot}
        onClose={() => setDeleteSlot(null)}
        onConfirm={handleDeleteConfirm}
        submitting={submitting}
      />
    </div>
  );
};

export default AdminTimeSlots;
