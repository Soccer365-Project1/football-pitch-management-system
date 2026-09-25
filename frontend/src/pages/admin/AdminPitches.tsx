import React, { useState, useEffect, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { Search, Plus, Edit, Settings, X, Wrench, Trash2, Loader2, ChevronLeft, ChevronRight, PowerOff } from 'lucide-react';
import Swal from 'sweetalert2';
import { pitchService } from '../../services/pitchService';
import type { Pitch, PitchType, PitchStatus, PitchRequest } from '../../types/pitch';
import { showToast } from '../../utils/toast';

/**
 * =========================================================================================
 * COMPONENT: ModalOverlay
 * =========================================================================================
 * Component vỏ bọc (wrapper) cho toàn bộ Modal trong trang.
 * Sử dụng `createPortal` để mount trực tiếp vào `document.body`, đảm bảo:
 * - Không bị ảnh hưởng bởi CSS cha (`overflow: hidden` hoặc `z-index`).
 * - Backdrop đen mờ phủ toàn màn hình (`rgba(0,0,0,0.5)`).
 * - Click vào vùng ngoài (backdrop) sẽ kích hoạt hàm `onClose`.
 * - Hộp thoại con ở giữa giữ kích thước tối đa 500px, cuộn dọc nếu nội dung dài.
 */
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
      {/* Vùng bấm ra ngoài để đóng modal */}
      <div
        style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0 }}
        onClick={onClose}
      />
      {/* Khung nội dung modal ở giữa */}
      <div
        className="card"
        style={{
          position: 'relative',
          zIndex: 10000,
          width: '100%',
          maxWidth: '500px',
          maxHeight: '90vh',
          overflowY: 'auto',
          display: 'flex',
          flexDirection: 'column',
          transform: 'none', // Tránh hiệu ứng hover nhấc card của CSS gốc
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
 * COMPONENT CHÍNH: AdminPitches
 * =========================================================================================
 * Quản lý Danh mục Sân bóng (Admin Dashboard)
 * Bao gồm:
 * 1. Thanh công cụ tìm kiếm và bộ lọc (Loại sân, Trạng thái) + Nút thêm sân mới.
 * 2. Bảng danh sách sân bóng với Sticky Header, trạng thái badge màu sắc.
 * 3. Thao tác: Đổi trạng thái bảo trì (SweetAlert2), Chỉnh sửa (Modal), Xóa mềm (SweetAlert2).
 * 4. Phân trang đặt ở góc phải chân card (Right-aligned pagination).
 */
const AdminPitches: React.FC = () => {
  // ---------------------------------------------------------------------------------------
  // 1. Quản lý trạng thái Bộ lọc & Phân trang
  // ---------------------------------------------------------------------------------------
  const [searchTerm, setSearchTerm] = useState('');                      // Từ khóa tìm kiếm theo tên sân
  const [typeFilter, setTypeFilter] = useState<number | string>('ALL');  // Lọc theo ID loại sân ('ALL' hoặc số)
  const [statusFilter, setStatusFilter] = useState<PitchStatus | 'ALL'>('ALL'); // Lọc theo trạng thái ('ALL', 'ACTIVE', 'MAINTENANCE', 'INACTIVE')
  const [page, setPage] = useState(1);                                  // Trang hiện tại (1-indexed theo quy ước Backend)
  const pageSize = 10;                                                  // Số bản ghi cố định trên mỗi trang

  // ---------------------------------------------------------------------------------------
  // 2. Quản lý dữ liệu từ Backend
  // ---------------------------------------------------------------------------------------
  const [pitches, setPitches] = useState<Pitch[]>([]);                  // Danh sách sân bóng hiển thị trong bảng
  const [pitchTypes, setPitchTypes] = useState<PitchType[]>([]);        // Danh mục loại sân (tải từ API /types)
  const [totalPages, setTotalPages] = useState(1);                      // Tổng số trang tính từ Backend
  const [totalElements, setTotalElements] = useState(0);                // Tổng số lượng bản ghi thỏa mãn điều kiện

  // ---------------------------------------------------------------------------------------
  // 3. Trạng thái giao diện (UI Loading, Submit & Error States)
  // ---------------------------------------------------------------------------------------
  const [loading, setLoading] = useState(false);                        // Đang tải dữ liệu danh sách sân
  const [submitting, setSubmitting] = useState(false);                  // Đang gửi request Tạo hoặc Cập nhật sân
  const [serverError, setServerError] = useState<string | null>(null);  // Lỗi từ server trả về (ví dụ: trùng tên sân)

  // ---------------------------------------------------------------------------------------
  // 4. Trạng thái điều khiển Modal
  // ---------------------------------------------------------------------------------------
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);          // Trạng thái mở Modal Thêm mới
  const [editPitch, setEditPitch] = useState<Pitch | null>(null);       // Sân đang được chọn để chỉnh sửa (null = đóng modal)

  // ---------------------------------------------------------------------------------------
  // 5. Quản lý giá trị Form trong Modal
  // ---------------------------------------------------------------------------------------
  // Form Thêm mới
  const [addName, setAddName] = useState('');
  const [addTypeId, setAddTypeId] = useState<number | string>('');
  const [addDesc, setAddDesc] = useState('');
  const [formErrors, setFormErrors] = useState<{ name?: string; typeId?: string }>({});

  // Form Chỉnh sửa
  const [editName, setEditName] = useState('');
  const [editTypeId, setEditTypeId] = useState<number | string>('');
  const [editDesc, setEditDesc] = useState('');

  // ---------------------------------------------------------------------------------------
  // 6. Hook: Tải danh mục loại sân bóng khi Component được mount
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
  // 7. Hook: Tải danh sách sân bóng từ Backend dựa trên các bộ lọc và phân trang
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

      // Backend trả về `items` trong PageResponse, fallback `content` nếu có
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
  // 8. Handlers: Mở Modal Thêm mới và Chỉnh sửa
  // ---------------------------------------------------------------------------------------
  /**
   * Mở modal thêm sân mới: Reset các ô nhập liệu về giá trị mặc định ban đầu
   */
  const handleOpenAddModal = () => {
    setAddName('');
    setAddTypeId(pitchTypes[0]?.id || '');
    setAddDesc('');
    setFormErrors({});
    setServerError(null);
    setIsAddModalOpen(true);
  };

  /**
   * Mở modal chỉnh sửa: Điền trước thông tin của sân hiện tại vào form
   */
  const handleOpenEditModal = (pitch: Pitch) => {
    setEditPitch(pitch);
    setEditName(pitch.name);
    setEditTypeId(pitch.pitchType?.id || pitchTypes[0]?.id || '');
    setEditDesc(pitch.description || '');
    setFormErrors({});
    setServerError(null);
  };

  // ---------------------------------------------------------------------------------------
  // 9. Handlers: Submit dữ liệu Thêm mới và Chỉnh sửa
  // ---------------------------------------------------------------------------------------
  /**
   * Xử lý tạo mới sân bóng: Validate tại client ➔ Gọi API ➔ Thông báo kết quả
   */
  const handleCreatePitch = async (e: React.FormEvent) => {
    e.preventDefault();

    // Client-side validation: Kiểm tra tên sân và loại sân bắt buộc
    const errors: { name?: string; typeId?: string } = {};
    if (!addName.trim()) {
      errors.name = 'Vui lòng nhập tên sân bóng';
    } else if (addName.trim().length > 100) {
      errors.name = 'Tên sân không được vượt quá 100 ký tự';
    }
    if (!addTypeId) {
      errors.typeId = 'Vui lòng chọn loại sân';
    }

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setSubmitting(true);
    setServerError(null);
    try {
      const payload: PitchRequest = {
        name: addName.trim(),
        pitchTypeId: Number(addTypeId),
        description: addDesc.trim() || undefined,
      };
      await pitchService.createPitch(payload);
      showToast('Thêm sân bóng mới thành công!', 'success');
      setIsAddModalOpen(false);
      loadPitches(); // Tải lại danh sách mới
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Đã có lỗi xảy ra khi tạo sân';
      // Xử lý ca biên: Tên sân đã tồn tại (Mã lỗi 3001) ➔ Hiển thị dòng chữ đỏ trên form mà không đóng modal
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
   * Xử lý cập nhật sân bóng: Validate tại client ➔ Gọi API PUT ➔ Tải lại dữ liệu
   */
  const handleUpdatePitch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editPitch) return;

    // Client-side validation
    const errors: { name?: string; typeId?: string } = {};
    if (!editName.trim()) {
      errors.name = 'Vui lòng nhập tên sân bóng';
    } else if (editName.trim().length > 100) {
      errors.name = 'Tên sân không được vượt quá 100 ký tự';
    }
    if (!editTypeId) {
      errors.typeId = 'Vui lòng chọn loại sân';
    }

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    setSubmitting(true);
    setServerError(null);
    try {
      const payload: PitchRequest = {
        name: editName.trim(),
        pitchTypeId: Number(editTypeId),
        description: editDesc.trim() || undefined,
      };
      await pitchService.updatePitch(editPitch.id, payload);
      showToast('Cập nhật thông tin sân bóng thành công!', 'success');
      setEditPitch(null);
      loadPitches(); // Tải lại danh sách
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
  // 10. Handlers: Chuyển đổi trạng thái & Xóa sân (SweetAlert2)
  // ---------------------------------------------------------------------------------------
  /**
   * Thay đổi trạng thái hoạt động của sân (Hoạt động, Bảo trì, Ngừng hoạt động)
   * Sử dụng SweetAlert2 radio options trực quan cho phép Admin lựa chọn linh hoạt
   */
  const handleChangeStatus = async (pitch: Pitch) => {
    const inputOptions: Record<string, string> = {
      ACTIVE: '🟢 Đang hoạt động (Mở đón khách đặt sân)',
      MAINTENANCE: '🟠 Đang bảo trì (Tạm dừng đón khách)',
      INACTIVE: '⚪ Ngừng hoạt động (Không mở lịch đặt, lưu trữ dữ liệu)',
    };

    const { value: selectedStatus } = await Swal.fire({
      title: `Trạng thái: ${pitch.name}`,
      text: 'Chọn trạng thái hoạt động mới cho sân bóng:',
      input: 'radio',
      inputOptions,
      inputValue: pitch.status,
      showCancelButton: true,
      confirmButtonText: 'Cập nhật',
      cancelButtonText: 'Hủy bỏ',
      confirmButtonColor: '#10b981',
      inputValidator: (value) => {
        if (!value) {
          return 'Vui lòng chọn một trạng thái!';
        }
        if (value === pitch.status) {
          return 'Sân bóng hiện đã ở trạng thái này!';
        }
      },
    });

    if (selectedStatus) {
      try {
        await pitchService.updatePitchStatus(pitch.id, selectedStatus as PitchStatus);
        showToast('Cập nhật trạng thái sân bóng thành công!', 'success');
        loadPitches();
      } catch (err: any) {
        showToast(err.response?.data?.message || 'Không thể đổi trạng thái sân', 'error');
      }
    }
  };

  /**
   * Xóa mềm sân bóng:
   * - Xác nhận bằng SweetAlert2 nút đỏ (#ef4444)
   * - Nếu sân đã có dữ liệu đặt sân trong lịch sử (Backend trả về mã lỗi 3005):
   *   Chặn xóa và hiển thị hộp thoại gợi ý chuyển trạng thái sang "Ngừng hoạt động" (INACTIVE)
   * - Xử lý ca biên: Nếu xóa bản ghi duy nhất của trang > 1, tự động lùi về trang trước (page - 1)
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
        // Ca biên: Bản ghi duy nhất bị xóa tại trang 2 trở lên ➔ Lùi trang
        if (pitches.length === 1 && page > 1) {
          setPage((prev) => prev - 1);
        } else {
          loadPitches();
        }
      } catch (err: any) {
        const errorData = err.response?.data;
        const msg = errorData?.message || 'Không thể xóa sân bóng';

        // Xử lý ca biên: Sân đã có đơn đặt sân trong lịch sử (Mã lỗi 3005)
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
  // 11. GIAO DIỆN CHÍNH (RENDER) - RESPONSIVE
  // ---------------------------------------------------------------------------------------
  return (
    // Khung bao ngoài: Áp dụng class admin-pitches-container (hỗ trợ responsive chiều cao)
    <div className="w-full admin-pitches-container">
      {/* Khung Card chính: Bo viền, đổ bóng, padding co giãn (p-3 sm:p-5 md:p-6), chứa toolbar, table và pagination */}
      <div
        className="card p-3 sm:p-5 md:p-6"
        style={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          overflow: 'hidden',
          transform: 'none', // Vô hiệu hóa hiệu ứng hover nhấc card của CSS gốc
        }}
      >
        {/* ===============================================================================
            11.1. THANH CÔNG CỤ TRÊN CÙNG (TOOLBAR) - RESPONSIVE
            - Áp dụng class .admin-pitches-toolbar từ index.css
            - Tự động chuyển dạng cột trên mobile/tablet và dạng ngang trên desktop
        =============================================================================== */}
        <div className="admin-pitches-toolbar">
          {/* Ô nhập tìm kiếm từ khóa theo tên sân (.admin-pitches-search) */}
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

          {/* Cụm bộ lọc Loại sân, Trạng thái & Nút Thêm mới (.admin-pitches-actions) */}
          <div className="admin-pitches-actions">
            {/* Dropdown lọc theo loại sân bóng */}
            <select
              value={typeFilter}
              onChange={(e) => {
                setTypeFilter(e.target.value === 'ALL' ? 'ALL' : Number(e.target.value));
                setPage(1); // Reset về trang 1 khi đổi bộ lọc
              }}
              style={{
                height: '42px',
                minWidth: '130px',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-md)',
                backgroundColor: 'var(--color-bg-base)',
                color: 'var(--color-text-base)',
                outline: 'none',
                padding: '0 0.75rem',
                cursor: 'pointer',
              }}
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
                setPage(1); // Reset về trang 1 khi đổi trạng thái
              }}
              style={{
                height: '42px',
                minWidth: '130px',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-md)',
                backgroundColor: 'var(--color-bg-base)',
                color: 'var(--color-text-base)',
                outline: 'none',
                padding: '0 0.75rem',
                cursor: 'pointer',
              }}
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
            11.2. BẢNG DANH SÁCH SÂN BÓNG (STICKY HEADER TABLE) - RESPONSIVE
            - Container có overflowX: 'auto' cho phép cuộn/vuốt ngang trên màn hình nhỏ
            - Sử dụng class .admin-pitches-table (min-width: 650px khi có dữ liệu) để các cột không bị bẹp
            - Khi không có dữ liệu (rỗng/loading): Áp dụng class .is-empty và flex căn giữa nội dung ra chính giữa màn hình
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
            {/* Header bảng cố định bằng position sticky */}
            <thead style={{ position: 'sticky', top: 0, backgroundColor: 'var(--color-bg-surface)', zIndex: 10 }}>
              <tr style={{ borderBottom: '1px solid var(--color-border)' }}>
                <th className="p-4 font-semibold text-muted text-sm">TÊN SÂN</th>
                <th className="p-4 font-semibold text-muted text-sm">LOẠI SÂN</th>
                <th className="p-4 font-semibold text-muted text-sm">TRẠNG THÁI</th>
                <th className="p-4 font-semibold text-muted text-sm text-left">THAO TÁC</th>
              </tr>
            </thead>
            <tbody style={{ height: pitches.length === 0 || loading ? '100%' : 'auto' }}>
              {/* Trạng thái 1: Đang tải dữ liệu từ Backend */}
              {loading ? (
                <tr style={{ height: '100%' }}>
                  <td
                    colSpan={4}
                    className="p-8 text-center text-muted"
                    style={{
                      height: '100%',
                      verticalAlign: 'middle',
                    }}
                  >
                    <div
                      style={{
                        minHeight: '260px',
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '0.75rem',
                      }}
                    >
                      <Loader2 size={24} className="animate-spin text-primary" />
                      <span>Đang tải danh sách sân bóng...</span>
                    </div>
                  </td>
                </tr>
              ) : pitches.length === 0 ? (
                /* Trạng thái 2: Không tìm thấy bản ghi nào - Căn chính giữa khung màn hình cả chiều ngang lẫn chiều dọc */
                <tr style={{ height: '100%' }}>
                  <td
                    colSpan={4}
                    className="text-center text-muted"
                    style={{
                      height: '100%',
                      verticalAlign: 'middle',
                      padding: '1.5rem 1rem',
                    }}
                  >
                    <div
                      style={{
                        minHeight: '160px',
                        height: '100%',
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        gap: '0.5rem',
                      }}
                    >
                      <div className="text-base font-medium text-muted">
                        Không tìm thấy sân bóng nào phù hợp
                      </div>
                      {searchTerm && (
                        <div className="text-xs text-muted" style={{ opacity: 0.8 }}>
                          Thử tìm kiếm với từ khóa khác hoặc điều chỉnh lại bộ lọc
                        </div>
                      )}
                    </div>
                  </td>
                </tr>
              ) : (
                /* Trạng thái 3: Hiển thị danh sách sân bóng */
                pitches.map((pitch) => (
                  <tr
                    key={pitch.id}
                    style={{ borderBottom: '1px solid var(--color-border)' }}
                    className="hover:bg-[var(--color-bg-base)]/50 transition-colors"
                  >
                    {/* Cột Tên Sân: Tên in đậm kèm mã định danh bên dưới */}
                    <td className="p-4 font-semibold">
                      <div>{pitch.name}</div>
                      <div className="text-xs text-muted font-normal mt-1">Mã: {pitch.id}</div>
                    </td>

                    {/* Cột Loại Sân: Hiển thị badge bo tròn */}
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

                    {/* Cột Trạng Thái: Badge Xanh (Hoạt động), Cam (Bảo trì), Xám (Ngừng hoạt động) */}
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

                    {/* Cột Thao Tác: Đổi trạng thái, Sửa, Xóa */}
                    <td className="p-4 text-left">
                      <div className="flex gap-2 justify-start items-center">
                        {/* Nút Đổi trạng thái hoạt động */}
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
                          onClick={() => handleChangeStatus(pitch)}
                        >
                          {pitch.status === 'ACTIVE' ? (
                            <Settings size={16} />
                          ) : pitch.status === 'MAINTENANCE' ? (
                            <Wrench size={16} />
                          ) : (
                            <PowerOff size={16} />
                          )}
                        </button>

                        {/* Nút Chỉnh sửa thông tin sân */}
                        <button
                          type="button"
                          className="btn btn-secondary"
                          style={{ padding: '0.5rem' }}
                          title="Chỉnh sửa"
                          onClick={() => handleOpenEditModal(pitch)}
                        >
                          <Edit size={16} />
                        </button>

                        {/* Nút Xóa sân bóng */}
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
            11.3. PHÂN TRANG Ở GÓC PHẢI CHÂN CARD (RIGHT-ALIGNED PAGINATION) - RESPONSIVE
            - Áp dụng class .admin-pitches-pagination từ index.css
            - Tự động căn phải trên màn hình lớn và căn giữa trên mobile
        =============================================================================== */}
        {totalElements > 0 && (
          <div className="admin-pitches-pagination">
            {/* Dòng thông báo dải bản ghi hiển thị */}
            <div className="text-sm text-muted">
              Hiển thị <strong style={{ color: 'var(--color-text-base)' }}>{(page - 1) * pageSize + 1} - {Math.min(page * pageSize, totalElements)}</strong> trên tổng số{' '}
              <strong style={{ color: 'var(--color-text-base)' }}>{totalElements}</strong> sân bóng
            </div>

            {/* Cụm nút điều hướng phân trang */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              {/* Nút lùi về trang trước */}
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

              {/* Ô hiển thị số trang hiện tại / tổng trang */}
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

              {/* Nút tiến tới trang sau */}
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
          11.4. MODAL THÊM SÂN BÓNG MỚI
      =============================================================================== */}
      {isAddModalOpen && (
        <ModalOverlay onClose={() => setIsAddModalOpen(false)}>
          {/* Header Modal */}
          <div className="flex justify-between items-center mb-6" style={{ flexShrink: 0 }}>
            <h2 className="text-xl font-bold">Thêm Sân Bóng Mới</h2>
            <button
              type="button"
              onClick={() => setIsAddModalOpen(false)}
              className="text-muted hover:text-[var(--color-text-base)]"
            >
              <X size={24} />
            </button>
          </div>

          {/* Form nhập liệu */}
          <form onSubmit={handleCreatePitch} className="flex flex-col" style={{ flex: 1 }}>
            <div className="grid gap-4 mb-6" style={{ overflowY: 'auto', paddingRight: '0.5rem' }}>
              {/* Nhập Tên Sân */}
              <div>
                <label className="block text-sm font-semibold mb-2">Tên sân</label>
                <input
                  type="text"
                  className="w-full"
                  placeholder="Ví dụ: Sân 5, Sân VIP..."
                  value={addName}
                  maxLength={100}
                  onChange={(e) => {
                    setAddName(e.target.value);
                    if (formErrors.name) setFormErrors((prev) => ({ ...prev, name: undefined }));
                  }}
                  style={{
                    padding: '0.6rem 1rem',
                    border: '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--color-bg-base)',
                    color: 'var(--color-text-base)',
                  }}
                />
                {/* Thông báo lỗi validation hoặc trùng tên từ Server */}
                {(formErrors.name || serverError) && (
                  <p className="text-xs text-red-500 mt-1 font-medium">{formErrors.name || serverError}</p>
                )}
              </div>

              {/* Chọn Loại Sân */}
              <div>
                <label className="block text-sm font-semibold mb-2">Loại sân</label>
                <select
                  className="w-full"
                  value={addTypeId}
                  onChange={(e) => {
                    setAddTypeId(Number(e.target.value));
                    if (formErrors.typeId) setFormErrors((prev) => ({ ...prev, typeId: undefined }));
                  }}
                  style={{
                    padding: '0.6rem 1rem',
                    border: '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--color-bg-base)',
                    color: 'var(--color-text-base)',
                  }}
                >
                  {pitchTypes.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name} (Tối đa {t.playerCapacity} người)
                    </option>
                  ))}
                </select>
                {formErrors.typeId && <p className="text-xs text-red-500 mt-1">{formErrors.typeId}</p>}
              </div>

              {/* Nhập Mô Tả Sân */}
              <div>
                <label className="block text-sm font-semibold mb-2">Mô tả (tùy chọn)</label>
                <textarea
                  rows={3}
                  className="w-full"
                  placeholder="Ghi chú về mặt cỏ, hệ thống chiếu sáng, vị trí..."
                  value={addDesc}
                  onChange={(e) => setAddDesc(e.target.value)}
                  style={{
                    padding: '0.6rem 1rem',
                    border: '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--color-bg-base)',
                    color: 'var(--color-text-base)',
                    resize: 'none',
                  }}
                />
              </div>
            </div>

            {/* Footer Modal: Nút Tạo Sân */}
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
                disabled={submitting}
                className="btn btn-primary w-full"
                style={{ height: '44px' }}
              >
                {submitting ? (
                  <span className="flex items-center justify-center gap-2">
                    <Loader2 size={18} className="animate-spin" /> Đang tạo...
                  </span>
                ) : (
                  'Tạo Sân Mới'
                )}
              </button>
            </div>
          </form>
        </ModalOverlay>
      )}

      {/* ===============================================================================
          11.5. MODAL CHỈNH SỬA SÂN BÓNG
      =============================================================================== */}
      {editPitch && (
        <ModalOverlay onClose={() => setEditPitch(null)}>
          {/* Header Modal */}
          <div className="flex justify-between items-center mb-6" style={{ flexShrink: 0 }}>
            <h2 className="text-xl font-bold">Chỉnh Sửa Sân Bóng</h2>
            <button
              type="button"
              onClick={() => setEditPitch(null)}
              className="text-muted hover:text-[var(--color-text-base)]"
            >
              <X size={24} />
            </button>
          </div>

          {/* Form Chỉnh sửa */}
          <form onSubmit={handleUpdatePitch} className="flex flex-col" style={{ flex: 1 }}>
            <div className="grid gap-4 mb-6" style={{ overflowY: 'auto', paddingRight: '0.5rem' }}>
              {/* Sửa Tên Sân */}
              <div>
                <label className="block text-sm font-semibold mb-2">Tên sân</label>
                <input
                  type="text"
                  className="w-full"
                  value={editName}
                  maxLength={100}
                  onChange={(e) => {
                    setEditName(e.target.value);
                    if (formErrors.name) setFormErrors((prev) => ({ ...prev, name: undefined }));
                  }}
                  style={{
                    padding: '0.6rem 1rem',
                    border: '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--color-bg-base)',
                    color: 'var(--color-text-base)',
                  }}
                />
                {(formErrors.name || serverError) && (
                  <p className="text-xs text-red-500 mt-1 font-medium">{formErrors.name || serverError}</p>
                )}
              </div>

              {/* Sửa Loại Sân */}
              <div>
                <label className="block text-sm font-semibold mb-2">Loại sân</label>
                <select
                  className="w-full"
                  value={editTypeId}
                  onChange={(e) => {
                    setEditTypeId(Number(e.target.value));
                    if (formErrors.typeId) setFormErrors((prev) => ({ ...prev, typeId: undefined }));
                  }}
                  style={{
                    padding: '0.6rem 1rem',
                    border: '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--color-bg-base)',
                    color: 'var(--color-text-base)',
                  }}
                >
                  {pitchTypes.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name} (Tối đa {t.playerCapacity} người)
                    </option>
                  ))}
                </select>
                {formErrors.typeId && <p className="text-xs text-red-500 mt-1">{formErrors.typeId}</p>}
              </div>

              {/* Sửa Mô Tả Sân */}
              <div>
                <label className="block text-sm font-semibold mb-2">Mô tả (tùy chọn)</label>
                <textarea
                  rows={3}
                  className="w-full"
                  value={editDesc}
                  onChange={(e) => setEditDesc(e.target.value)}
                  style={{
                    padding: '0.6rem 1rem',
                    border: '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--color-bg-base)',
                    color: 'var(--color-text-base)',
                    resize: 'none',
                  }}
                />
              </div>
            </div>

            {/* Footer Modal: Hủy và Lưu Thay Đổi */}
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
                style={{ height: '44px' }}
                onClick={() => setEditPitch(null)}
              >
                Hủy bỏ
              </button>
              <button
                type="submit"
                disabled={submitting}
                className="btn btn-primary w-1/2"
                style={{ height: '44px' }}
              >
                {submitting ? (
                  <span className="flex items-center justify-center gap-2">
                    <Loader2 size={18} className="animate-spin" /> Đang lưu...
                  </span>
                ) : (
                  'Lưu Thay Đổi'
                )}
              </button>
            </div>
          </form>
        </ModalOverlay>
      )}
    </div>
  );
};

export default AdminPitches;
