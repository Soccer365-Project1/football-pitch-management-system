import React, { useState, useEffect, useCallback } from 'react';
import { Search, Plus, Edit, Settings, Wrench, Trash2, Loader2, ChevronLeft, ChevronRight, PowerOff } from 'lucide-react';
import Swal from 'sweetalert2';
import { pitchService } from '../../services/pitchService';
import type { Pitch, PitchType, PitchStatus, PitchRequest } from '../../types/pitch';
import { showToast } from '../../utils/toast';
import { PitchModal } from '../../components/admin/pitch/PitchModal';
import { ChangeStatusModal } from '../../components/admin/pitch/ChangeStatusModal';

/**
 * =========================================================================================
 * COMPONENT CHÍNH: AdminPitches (Màn hình Quản trị Danh mục Sân bóng)
 * =========================================================================================
 * Quản lý toàn bộ thông tin sân bóng (Sân 5, Sân 7, v.v.):
 * 1. Thanh công cụ tìm kiếm theo tên và lọc theo Loại sân / Trạng thái hoạt động.
 * 2. Bảng danh sách sân bóng với tiêu đề cố định (Sticky Header) và cuộn ngang linh hoạt.
 * 3. Thao tác: Thay đổi trạng thái (Hoạt động / Bảo trì / Ngừng hoạt động), Sửa thông tin, Xóa mềm.
 * 4. Phân trang đặt ở chân card (Right-aligned pagination).
 * 5. Tách toàn bộ Form Thêm / Sửa vào component con `PitchModal` để giữ trang chính gọn gàng.
 */
const AdminPitches: React.FC = () => {
  // ---------------------------------------------------------------------------------------
  // 1. Quản lý trạng thái Bộ lọc & Phân trang (useState)
  // ---------------------------------------------------------------------------------------
  /** Từ khóa tìm kiếm theo tên sân bóng */
  const [searchTerm, setSearchTerm] = useState<string>('');

  /** ID loại sân bóng đang lọc ('ALL' là tất cả hoặc ID số cụ thể) */
  const [typeFilter, setTypeFilter] = useState<number | string>('ALL');

  /** Trạng thái sân bóng đang lọc ('ALL', 'ACTIVE', 'MAINTENANCE', 'INACTIVE') */
  const [statusFilter, setStatusFilter] = useState<PitchStatus | 'ALL'>('ALL');

  /** Trang hiện tại của danh sách (1-indexed theo quy ước Backend) */
  const [page, setPage] = useState<number>(1);

  /** Số lượng sân bóng hiển thị cố định trên mỗi trang */
  const pageSize = 10;

  // ---------------------------------------------------------------------------------------
  // 2. Quản lý Dữ liệu Sân bóng từ Backend
  // ---------------------------------------------------------------------------------------
  /** Danh sách sân bóng hiển thị trong bảng */
  const [pitches, setPitches] = useState<Pitch[]>([]);

  /** Danh mục các loại sân (tải từ API /api/v1/pitches/types) */
  const [pitchTypes, setPitchTypes] = useState<PitchType[]>([]);

  /** Tổng số trang được tính từ Backend */
  const [totalPages, setTotalPages] = useState<number>(1);

  /** Tổng số lượng bản ghi sân bóng thỏa mãn điều kiện lọc */
  const [totalElements, setTotalElements] = useState<number>(0);

  // ---------------------------------------------------------------------------------------
  // 3. Trạng thái Loading, Submit & Modal
  // ---------------------------------------------------------------------------------------
  /** Cờ báo đang tải dữ liệu danh sách từ server */
  const [loading, setLoading] = useState<boolean>(false);

  /** Cờ báo đang thực hiện lưu dữ liệu (Thêm hoặc Sửa) */
  const [submitting, setSubmitting] = useState<boolean>(false);

  /** Thông báo lỗi từ server trả về nếu có (ví dụ: trùng tên sân) */
  const [serverError, setServerError] = useState<string | null>(null);

  /** Cờ mở Modal Thêm sân mới */
  const [isAddModalOpen, setIsAddModalOpen] = useState<boolean>(false);

  /** Đối tượng sân bóng đang được chọn để Chỉnh sửa (null khi không mở modal sửa) */
  const [editPitch, setEditPitch] = useState<Pitch | null>(null);

  /** Đối tượng sân bóng đang được chọn để Đổi trạng thái (null khi không mở modal đổi trạng thái) */
  const [statusPitch, setStatusPitch] = useState<Pitch | null>(null);

  // ---------------------------------------------------------------------------------------
  // 4. Hook: Tải danh mục loại sân bóng khi Component được tải lần đầu
  // ---------------------------------------------------------------------------------------
  useEffect(() => {
    const fetchPitchTypes = async () => {
      try {
        const types = await pitchService.getPitchTypes();
        setPitchTypes(types || []);
      } catch (err: any) {
        showToast(err.response?.data?.message || 'Không thể tải danh mục loại sân', 'error');
      }
    };
    fetchPitchTypes();
  }, []);

  // ---------------------------------------------------------------------------------------
  // 5. Hook: Tải danh sách sân bóng từ Backend dựa trên điều kiện tìm kiếm và phân trang
  // ---------------------------------------------------------------------------------------
  const loadPitches = useCallback(async () => {
    setLoading(true);
    try {
      const data = await pitchService.getPitches({
        keyword: searchTerm,
        pitchTypeId: typeFilter,
        status: statusFilter,
        page,
        size: pageSize,
      });

      // Backend trả về `items` trong PageResponse, fallback sang `content` nếu có
      const list = data?.items || data?.content || [];
      setPitches(list);
      setTotalPages(data?.totalPages || 1);
      setTotalElements(data?.totalElements || 0);
    } catch (err: any) {
      setPitches([]);
      setTotalElements(0);
      showToast(err.response?.data?.message || 'Không thể tải danh sách sân bóng', 'error');
    } finally {
      setLoading(false);
    }
  }, [searchTerm, typeFilter, statusFilter, page]);

  // Tự động gọi lại API khi bất kỳ điều kiện lọc nào thay đổi
  useEffect(() => {
    loadPitches();
  }, [loadPitches]);

  // ---------------------------------------------------------------------------------------
  // 6. Handlers: Xử lý Thêm mới và Chỉnh sửa Sân bóng
  // ---------------------------------------------------------------------------------------
  /** Mở popup Thêm sân mới */
  const handleOpenAddModal = () => {
    setServerError(null);
    setIsAddModalOpen(true);
  };

  /** Mở popup Chỉnh sửa thông tin sân */
  const handleOpenEditModal = (pitch: Pitch) => {
    setServerError(null);
    setEditPitch(pitch);
  };

  /** Đóng toàn bộ Modal đang mở và xóa lỗi server */
  const handleCloseModal = () => {
    setIsAddModalOpen(false);
    setEditPitch(null);
    setServerError(null);
  };

  /**
   * Gọi API Thêm mới sân bóng: `POST /api/v1/pitches`
   * Nhận dữ liệu payload từ component con `PitchModal`
   */
  const handleCreatePitch = async (payload: PitchRequest) => {
    setSubmitting(true);
    setServerError(null);
    try {
      await pitchService.createPitch(payload);
      showToast('Thêm sân bóng mới thành công!', 'success');
      handleCloseModal();
      loadPitches(); // Tải lại danh sách mới
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Đã có lỗi xảy ra khi tạo sân';
      // Xử lý ca biên: Tên sân đã tồn tại (Mã lỗi 3001)
      if (err.response?.data?.code === 3001 || msg.includes('đã tồn tại')) {
        setServerError('* Tên sân bóng đã tồn tại trên hệ thống, vui lòng chọn tên khác.');
      } else {
        setServerError(msg);
      }
      showToast(msg, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  /**
   * Gọi API Cập nhật sân bóng: `PUT /api/v1/pitches/{id}`
   * Nhận dữ liệu payload từ component con `PitchModal`
   */
  const handleUpdatePitch = async (payload: PitchRequest) => {
    if (!editPitch) return;
    setSubmitting(true);
    setServerError(null);
    try {
      await pitchService.updatePitch(editPitch.id, payload);
      showToast('Cập nhật thông tin sân bóng thành công!', 'success');
      handleCloseModal();
      loadPitches(); // Tải lại danh sách mới
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Đã có lỗi xảy ra khi cập nhật';
      if (err.response?.data?.code === 3001 || msg.includes('đã tồn tại')) {
        setServerError('* Tên sân bóng đã tồn tại trên hệ thống, vui lòng chọn tên khác.');
      } else {
        setServerError(msg);
      }
      showToast(msg, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  // ---------------------------------------------------------------------------------------
  // 7. Handlers: Chuyển đổi trạng thái & Xóa sân (SweetAlert2)
  // ---------------------------------------------------------------------------------------
  /**
   * Cập nhật trạng thái hoạt động mới cho sân bóng:
   * Nhận newStatus ('ACTIVE' | 'MAINTENANCE' | 'INACTIVE') từ ChangeStatusModal
   */
  const handleUpdateStatus = async (newStatus: PitchStatus) => {
    if (!statusPitch) return;
    setSubmitting(true);
    try {
      await pitchService.updatePitchStatus(statusPitch.id, newStatus);
      showToast('Cập nhật trạng thái sân bóng thành công!', 'success');
      setStatusPitch(null);
      loadPitches();
    } catch (err: any) {
      showToast(err.response?.data?.message || 'Không thể đổi trạng thái sân', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  /**
   * Xóa mềm sân bóng với thông báo xác nhận an toàn:
   * - Nếu sân đã có dữ liệu đặt sân (Mã lỗi 3005): Gợi ý chuyển sang "Ngừng hoạt động"
   */
  const handleDeletePitch = async (pitch: Pitch) => {
    const result = await Swal.fire({
      title: 'Xác nhận xóa sân bóng?',
      text: `Bạn có chắc chắn muốn xóa sân "${pitch.name}"? Dữ liệu sân sẽ bị ẩn khỏi hệ thống.`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#ef4444',
      cancelButtonColor: '#6b7280',
      confirmButtonText: 'Xóa vĩnh viễn',
      cancelButtonText: 'Hủy bỏ',
    });

    if (result.isConfirmed) {
      try {
        await pitchService.deletePitch(pitch.id);
        showToast('Xóa sân bóng thành công!', 'success');
        // Ca biên: Bản ghi duy nhất bị xóa tại trang > 1 -> lùi về trang trước
        if (pitches.length === 1 && page > 1) {
          setPage((prev) => prev - 1);
        } else {
          loadPitches();
        }
      } catch (err: any) {
        const errorData = err.response?.data;
        const msg = errorData?.message || 'Không thể xóa sân bóng';

        if (errorData?.code === 3005 || msg.includes('dữ liệu đặt sân') || msg.includes('lịch sử')) {
          const switchResult = await Swal.fire({
            title: 'Không thể xóa sân bóng!',
            text: `${msg}. Bạn có muốn chuyển sân "${pitch.name}" sang trạng thái "Ngừng hoạt động" để lưu trữ dữ liệu lịch sử không?`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#10b981',
            cancelButtonColor: '#6b7280',
            confirmButtonText: 'Chuyển sang Ngừng hoạt động',
            cancelButtonText: 'Đóng',
          });

          if (switchResult.isConfirmed) {
            try {
              await pitchService.updatePitchStatus(pitch.id, 'INACTIVE');
              showToast('Đã chuyển sân sang trạng thái Ngừng hoạt động!', 'success');
              loadPitches();
            } catch (switchErr: any) {
              showToast(switchErr.response?.data?.message || 'Không thể đổi trạng thái', 'error');
            }
          }
        } else {
          showToast(msg, 'error');
        }
      }
    }
  };

  // ---------------------------------------------------------------------------------------
  // 8. GIAO DIỆN CHÍNH (RENDER)
  // ---------------------------------------------------------------------------------------
  return (
    <div className="w-full admin-pitches-container">
      {/* Khung Card chính chứa Toolbar, Bảng dữ liệu và Phân trang */}
      <div
        className="card p-3 sm:p-5 md:p-6"
        style={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
          transform: 'none',
        }}
      >
        {/* ===============================================================================
            8.1. THANH CÔNG CỤ TRÊN CÙNG (TOOLBAR)
        =============================================================================== */}
        <div className="admin-pitches-toolbar">
          {/* Ô nhập tìm kiếm từ khóa theo tên sân */}
          <div className="admin-pitches-search">
            <Search size={16} className="text-muted shrink-0" />
            <input
              type="text"
              placeholder="Tìm theo tên sân..."
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setPage(1); // Reset về trang 1 khi đổi từ khóa
              }}
              style={{
                background: 'transparent',
                border: 'none',
                outline: 'none',
                color: 'var(--color-text-base)',
                fontFamily: 'inherit',
                width: '100%',
              }}
            />
          </div>

          {/* Cụm bộ lọc Loại sân, Trạng thái & Nút Thêm mới */}
          <div className="admin-pitches-actions">
            {/* Dropdown lọc theo loại sân */}
            <select
              value={typeFilter}
              onChange={(e) => {
                setTypeFilter(e.target.value === 'ALL' ? 'ALL' : Number(e.target.value));
                setPage(1);
              }}
              className="admin-pitches-select"
            >
              <option value="ALL">Tất cả loại sân</option>
              {pitchTypes.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </select>

            {/* Dropdown lọc theo trạng thái hoạt động */}
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value as PitchStatus | 'ALL');
                setPage(1);
              }}
              className="admin-pitches-select"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang hoạt động</option>
              <option value="MAINTENANCE">Đang bảo trì</option>
              <option value="INACTIVE">Ngừng hoạt động</option>
            </select>

            {/* Nút mở Modal thêm sân bóng mới */}
            <button
              type="button"
              className="btn btn-primary"
              style={{ height: '42px', whiteSpace: 'nowrap' }}
              onClick={handleOpenAddModal}
            >
              <Plus size={18} /> Thêm sân mới
            </button>
          </div>
        </div>

        {/* ===============================================================================
            8.2. BẢNG DANH SÁCH SÂN BÓNG (STICKY HEADER TABLE)
        =============================================================================== */}
        <div
          style={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            overflowY: 'auto',
            overflowX: 'auto',
            borderTop: '1px solid var(--color-border)',
          }}
        >
          <table
            className={`admin-pitches-table ${pitches.length === 0 || loading ? 'is-empty' : ''}`}
            style={{
              height: pitches.length === 0 || loading ? '100%' : 'auto',
              minWidth: pitches.length > 0 ? '650px' : '100%',
            }}
          >
            {/* Header cố định bằng position sticky */}
            <thead style={{ position: 'sticky', top: 0, backgroundColor: 'var(--color-bg-surface)', zIndex: 10 }}>
              <tr style={{ borderBottom: '1px solid var(--color-border)' }}>
                <th className="p-4 font-semibold text-muted text-sm">TÊN SÂN</th>
                <th className="p-4 font-semibold text-muted text-sm">LOẠI SÂN</th>
                <th className="p-4 font-semibold text-muted text-sm">TRẠNG THÁI</th>
                <th className="p-4 font-semibold text-muted text-sm text-left">THAO TÁC</th>
              </tr>
            </thead>
            <tbody style={{ height: pitches.length === 0 || loading ? '100%' : 'auto' }}>
              {/* Trạng thái 1: Đang tải dữ liệu */}
              {loading ? (
                <tr style={{ height: '100%' }}>
                  <td colSpan={4} className="p-8 text-center text-muted" style={{ height: '100%', verticalAlign: 'middle' }}>
                    <div style={{ minHeight: '260px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '0.75rem' }}>
                      <Loader2 size={24} className="animate-spin text-primary" />
                      <span>Đang tải danh sách sân bóng...</span>
                    </div>
                  </td>
                </tr>
              ) : pitches.length === 0 ? (
                /* Trạng thái 2: Không có bản ghi nào */
                <tr style={{ height: '100%' }}>
                  <td colSpan={4} className="text-center text-muted" style={{ height: '100%', verticalAlign: 'middle', padding: '1.5rem 1rem' }}>
                    <div style={{ minHeight: '160px', height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}>
                      <div className="text-base font-medium text-muted">Không tìm thấy sân bóng nào phù hợp</div>
                      {searchTerm && (
                        <div className="text-xs text-muted" style={{ opacity: 0.8 }}>
                          Thử tìm kiếm với từ khóa khác hoặc điều chỉnh lại bộ lọc
                        </div>
                      )}
                    </div>
                  </td>
                </tr>
              ) : (
                /* Trạng thái 3: Hiển thị các dòng sân bóng */
                pitches.map((pitch) => (
                  <tr
                    key={pitch.id}
                    style={{ borderBottom: '1px solid var(--color-border)' }}
                    className="hover:bg-[var(--color-bg-base)]/50 transition-colors"
                  >
                    {/* Cột Tên Sân */}
                    <td className="p-4 font-semibold">
                      <div>{pitch.name}</div>
                      <div className="text-xs text-muted font-normal mt-1">Mã: {pitch.id}</div>
                    </td>

                    {/* Cột Loại Sân */}
                    <td className="p-4">
                      <span
                        className="badge"
                        style={{
                          backgroundColor: 'var(--color-bg-base)',
                          border: '1px solid var(--color-border)',
                          padding: '0.25rem 0.6rem',
                          borderRadius: 'var(--radius-md)',
                          fontSize: '0.85rem',
                        }}
                      >
                        {pitch.pitchType?.name || 'Sân tiêu chuẩn'}
                      </span>
                    </td>

                    {/* Cột Trạng Thái */}
                    <td className="p-4">
                      {pitch.status === 'ACTIVE' ? (
                        <span className="badge badge-success">Đang hoạt động</span>
                      ) : pitch.status === 'MAINTENANCE' ? (
                        <span className="badge badge-warning">Đang bảo trì</span>
                      ) : (
                        <span
                          className="badge"
                          style={{
                            backgroundColor: 'var(--color-bg-base)',
                            border: '1px solid var(--color-border)',
                            color: 'var(--color-text-muted)',
                            padding: '0.25rem 0.6rem',
                            borderRadius: 'var(--radius-md)',
                            fontSize: '0.85rem',
                          }}
                        >
                          Ngừng hoạt động
                        </span>
                      )}
                    </td>

                    {/* Cột Nút bấm thao tác */}
                    <td className="p-4 text-left">
                      <div className="flex gap-2 justify-start items-center">
                        {/* Nút Đổi trạng thái */}
                        <button
                          type="button"
                          className={`btn btn-secondary ${
                            pitch.status === 'ACTIVE'
                              ? 'text-primary'
                              : pitch.status === 'MAINTENANCE'
                              ? 'text-warning'
                              : 'text-muted'
                          }`}
                          style={{ padding: '0.5rem' }}
                          title="Thay đổi trạng thái sân"
                          onClick={() => setStatusPitch(pitch)}
                        >
                          {pitch.status === 'ACTIVE' ? (
                            <Settings size={16} />
                          ) : pitch.status === 'MAINTENANCE' ? (
                            <Wrench size={16} />
                          ) : (
                            <PowerOff size={16} />
                          )}
                        </button>

                        {/* Nút Chỉnh sửa */}
                        <button
                          type="button"
                          className="btn btn-secondary"
                          style={{ padding: '0.5rem' }}
                          title="Chỉnh sửa sân bóng"
                          onClick={() => handleOpenEditModal(pitch)}
                        >
                          <Edit size={16} />
                        </button>

                        {/* Nút Xóa */}
                        <button
                          type="button"
                          className="btn btn-secondary text-danger"
                          style={{ padding: '0.5rem', color: 'var(--color-danger)' }}
                          title="Xóa sân"
                          onClick={() => handleDeletePitch(pitch)}
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* ===============================================================================
            8.3. PHÂN TRANG (PAGINATION)
        =============================================================================== */}
        {totalElements > 0 && (
          <div className="admin-pitches-pagination">
            <div className="text-sm text-muted">
              Hiển thị <strong style={{ color: 'var(--color-text-base)' }}>{(page - 1) * pageSize + 1} - {Math.min(page * pageSize, totalElements)}</strong> trên tổng số{' '}
              <strong style={{ color: 'var(--color-text-base)' }}>{totalElements}</strong> sân bóng
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <button
                type="button"
                onClick={() => setPage((prev) => Math.max(1, prev - 1))}
                disabled={page <= 1}
                className="btn btn-secondary"
                style={{
                  height: '36px',
                  padding: '0 0.75rem',
                  fontSize: '0.875rem',
                  opacity: page <= 1 ? 0.5 : 1,
                  cursor: page <= 1 ? 'not-allowed' : 'pointer',
                }}
              >
                <ChevronLeft size={16} />
                <span>Trước</span>
              </button>

              <span
                style={{
                  padding: '0.35rem 0.75rem',
                  fontSize: '0.875rem',
                  fontWeight: 600,
                  color: 'var(--color-text-base)',
                  backgroundColor: 'var(--color-bg-base)',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--color-border)',
                }}
              >
                {page} / {Math.max(1, totalPages)}
              </span>

              <button
                type="button"
                onClick={() => setPage((prev) => Math.min(totalPages, prev + 1))}
                disabled={page >= totalPages}
                className="btn btn-secondary"
                style={{
                  height: '36px',
                  padding: '0 0.75rem',
                  fontSize: '0.875rem',
                  opacity: page >= totalPages ? 0.5 : 1,
                  cursor: page >= totalPages ? 'not-allowed' : 'pointer',
                }}
              >
                <span>Sau</span>
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* ===============================================================================
          8.4. MODAL THÊM MỚI HOẶC CHỈNH SỬA SÂN BÓNG (TÁCH THÀNH PITCHMODAL)
      =============================================================================== */}
      <PitchModal
        isOpen={isAddModalOpen || !!editPitch}
        isEdit={!!editPitch}
        initialData={editPitch}
        pitchTypes={pitchTypes}
        onClose={handleCloseModal}
        onSubmit={editPitch ? handleUpdatePitch : handleCreatePitch}
        submitting={submitting}
        serverError={serverError}
      />

      {/* ===============================================================================
          8.5. MODAL ĐỔI TRẠNG THÁI SÂN BÓNG (TÁCH THÀNH CHANGESTATUSMODAL)
      =============================================================================== */}
      <ChangeStatusModal
        pitch={statusPitch}
        onClose={() => setStatusPitch(null)}
        onConfirm={handleUpdateStatus}
        submitting={submitting}
      />
    </div>
  );
};

export default AdminPitches;
