import React, { useState, useEffect, useCallback } from 'react';
import {
  Search,
  MoreVertical,
  Shield,
  ShieldAlert,
  UserX,
  UserCheck,
  X,
  Loader2,
  ChevronLeft,
  ChevronRight,
  Eye,
  EyeOff,
} from 'lucide-react';
import { adminUserService } from '../../services/adminUserService';
import type { AdminUserItem, RoleName, UserStatus } from '../../types/adminUser';
import { ModalOverlay } from '../../components/common/ModalOverlay';
import { useAuth } from '../../contexts/AuthContext';
import { showToast } from '../../utils/toast';

interface FormErrors {
  fullName?: string;
  phoneNumber?: string;
  email?: string;
  password?: string;
  confirmPassword?: string;
}

// Biểu thức chính quy kiểm tra độ mạnh của mật khẩu theo chuẩn bảo mật hệ thống:
// - Độ dài từ 6 đến 64 ký tự: (?=.{6,64}$)
// - Chứa ít nhất 1 chữ cái: (?=.*[A-Za-z])
// - Chứa ít nhất 1 chữ số: (?=.*\d)
// - Chứa ít nhất 1 ký tự đặc biệt: (?=.*[^A-Za-z0-9\s])
// - Chặn triệt để khoảng trắng ở mọi vị trí: \S+$
const PASSWORD_REGEX = /^(?=.{6,64}$)(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S+$/;

/**
 * =========================================================================================
 * COMPONENT CHÍNH: AdminUsers (Màn hình Quản trị Người dùng & Thu hồi Tài khoản)
 * =========================================================================================
 * Cung cấp đầy đủ các tính năng:
 * 1. Thanh tìm kiếm từ khóa (Họ tên, Email, SĐT) và bộ lọc Vai trò, Trạng thái.
 * 2. Bảng danh sách Sticky Table hiển thị Badge và nút Khóa/Thu hồi tài khoản.
 * 3. Modal Tạo Nhân Viên Mới (Thêm Staff) với bộ validation nghiêm ngặt chuẩn hệ thống.
 * 4. Modal Thông Tin Người Dùng (Xem hồ sơ).
 * 5. Modal Xác Nhận Khóa / Thu Hồi Tài Khoản (Lock/Revoke Confirmation).
 * 6. Modal Xác Nhận Mở Khóa / Kích Hoạt Lại (Unlock/Reactivate Confirmation).
 * 7. Phân trang chuẩn Backend đặt ở góc phải chân card.
 */
const AdminUsers: React.FC = () => {
  const { user: currentAdmin } = useAuth();
  const isCurrentUserAdmin = currentAdmin?.role === 'ROLE_ADMIN';
  const isCurrentUserStaff = currentAdmin?.role === 'ROLE_STAFF';

  // ---------------------------------------------------------------------------------------
  // 1. Quản lý trạng thái Bộ lọc & Phân trang (useState)
  // ---------------------------------------------------------------------------------------
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [roleFilter, setRoleFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [page, setPage] = useState<number>(1);
  const pageSize = 10;

  // ---------------------------------------------------------------------------------------
  // 2. Dữ liệu từ Backend & Trạng thái Loading
  // ---------------------------------------------------------------------------------------
  const [users, setUsers] = useState<AdminUserItem[]>([]);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(false);

  // ---------------------------------------------------------------------------------------
  // 3. Trạng thái các Modals
  // ---------------------------------------------------------------------------------------
  const [isAddModalOpen, setIsAddModalOpen] = useState<boolean>(false);
  const [viewUser, setViewUser] = useState<AdminUserItem | null>(null);
  const [lockUser, setLockUser] = useState<AdminUserItem | null>(null);
  const [unlockUser, setUnlockUser] = useState<AdminUserItem | null>(null);
  const [modalSubmitting, setModalSubmitting] = useState<boolean>(false);

  // Form State cho Modal Chỉnh Sửa Thông Tin Người Dùng (viewUser)
  const [editFullName, setEditFullName] = useState<string>('');
  const [editPhone, setEditPhone] = useState<string>('');
  const [editErrors, setEditErrors] = useState<{ fullName?: string; phoneNumber?: string }>({});

  // Form State cho Modal Thêm Nhân Viên Mới
  const [addFullName, setAddFullName] = useState<string>('');
  const [addPhone, setAddPhone] = useState<string>('');
  const [addEmail, setAddEmail] = useState<string>('');
  const [addPassword, setAddPassword] = useState<string>('');
  const [addConfirmPassword, setAddConfirmPassword] = useState<string>('');
  const [showPassword, setShowPassword] = useState<boolean>(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState<boolean>(false);

  // Lỗi form theo từng trường và lỗi chung từ server
  const [fieldErrors, setFieldErrors] = useState<FormErrors>({});
  const [serverError, setServerError] = useState<string | null>(null);

  // ---------------------------------------------------------------------------------------
  // 4. Hook: Tải danh sách người dùng từ API Backend
  // ---------------------------------------------------------------------------------------
  const loadUsers = useCallback(async () => {
    setLoading(true);
    try {
      let roleParam: RoleName | 'ALL' = 'ALL';
      if (roleFilter === 'ADMIN') roleParam = 'ROLE_ADMIN';
      else if (roleFilter === 'STAFF') roleParam = 'ROLE_STAFF';
      else if (roleFilter === 'CUSTOMER') roleParam = 'ROLE_CUSTOMER';

      const data = await adminUserService.getUsers({
        keyword: searchTerm,
        role: roleParam,
        status: statusFilter as UserStatus | 'ALL',
        page,
        size: pageSize,
      });

      const list = data?.items || (data as any)?.content || [];
      setUsers(list);
      setTotalPages(data?.totalPages || 1);
      setTotalElements(data?.totalElements || 0);
    } catch (err: any) {
      setUsers([]);
      setTotalElements(0);
      showToast(err.response?.data?.message || 'Không thể tải danh sách người dùng', 'error');
    } finally {
      setLoading(false);
    }
  }, [searchTerm, roleFilter, statusFilter, page]);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  // ---------------------------------------------------------------------------------------
  // 5. Handlers: Xử lý Khóa / Thu hồi & Mở khóa tài khoản
  // ---------------------------------------------------------------------------------------
  const handleConfirmLock = async () => {
    if (!lockUser) return;
    setModalSubmitting(true);
    try {
      await adminUserService.updateUserStatus(lockUser.id, 'LOCKED');
      showToast(`Đã thu hồi tài khoản của ${lockUser.fullName}`, 'success');
      setLockUser(null);
      await loadUsers();
    } catch (err: any) {
      showToast(err.response?.data?.message || 'Không thể thu hồi tài khoản', 'error');
    } finally {
      setModalSubmitting(false);
    }
  };

  const handleConfirmUnlock = async () => {
    if (!unlockUser) return;
    setModalSubmitting(true);
    try {
      await adminUserService.updateUserStatus(unlockUser.id, 'ACTIVE');
      showToast(`Đã kích hoạt lại tài khoản của ${unlockUser.fullName}`, 'success');
      setUnlockUser(null);
      await loadUsers();
    } catch (err: any) {
      showToast(err.response?.data?.message || 'Không thể mở khóa tài khoản', 'error');
    } finally {
      setModalSubmitting(false);
    }
  };

  // ---------------------------------------------------------------------------------------
  // 5.1. Handlers: Mở và Lưu Chỉnh Sửa Thông Tin Người Dùng
  // ---------------------------------------------------------------------------------------
  const handleOpenViewModal = (user: AdminUserItem) => {
    setViewUser(user);
    setEditFullName(user.fullName || '');
    setEditPhone(user.phoneNumber || '');
    setEditErrors({});
  };

  const handleSaveEditUser = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!viewUser) return;

    const canEditTargetUser = isCurrentUserAdmin || (isCurrentUserStaff && viewUser.role === 'ROLE_CUSTOMER');
    if (!canEditTargetUser) {
      showToast('Bạn không có quyền chỉnh sửa thông tin tài khoản này', 'error');
      return;
    }

    const errors: { fullName?: string; phoneNumber?: string } = {};

    if (!editFullName.trim()) {
      errors.fullName = 'Họ và tên không được để trống';
    } else if (editFullName.trim().length < 2) {
      errors.fullName = 'Họ và tên phải có ít nhất 2 ký tự';
    } else if (editFullName.trim().length > 100) {
      errors.fullName = 'Họ và tên không được vượt quá 100 ký tự';
    }

    if (!editPhone.trim()) {
      errors.phoneNumber = 'Số điện thoại không được để trống';
    } else if (!/^(0[35789])[0-9]{8}$/.test(editPhone.trim())) {
      errors.phoneNumber = 'Số điện thoại không hợp lệ (phải gồm 10 chữ số và bắt đầu bằng 03, 05, 07, 08 hoặc 09)';
    }

    if (Object.keys(errors).length > 0) {
      setEditErrors(errors);
      return;
    }

    setModalSubmitting(true);
    try {
      await adminUserService.updateUser(viewUser.id, {
        fullName: editFullName.trim(),
        phoneNumber: editPhone.trim(),
      });

      showToast('Cập nhật thông tin người dùng thành công!', 'success');
      setViewUser(null);
      await loadUsers();
    } catch (err: any) {
      const msg = err.response?.data?.message || 'Không thể cập nhật thông tin người dùng';
      showToast(msg, 'error');
    } finally {
      setModalSubmitting(false);
    }
  };

  // ---------------------------------------------------------------------------------------
  // 6. Handlers: Xử lý Tạo Nhân Viên Mới (kèm Validate Chuẩn như Register)
  // ---------------------------------------------------------------------------------------
  const handleOpenAddModal = () => {
    setAddFullName('');
    setAddPhone('');
    setAddEmail('');
    setAddPassword('');
    setAddConfirmPassword('');
    setShowPassword(false);
    setShowConfirmPassword(false);
    setFieldErrors({});
    setServerError(null);
    setIsAddModalOpen(true);
  };

  const validateStaffForm = (): boolean => {
    const errors: FormErrors = {};

    // 1. Kiểm tra Họ và tên (không được để trống, từ 2 đến 100 ký tự)
    if (!addFullName.trim()) {
      errors.fullName = 'Họ và tên không được để trống';
    } else if (addFullName.trim().length < 2) {
      errors.fullName = 'Họ và tên phải có ít nhất 2 ký tự';
    } else if (addFullName.trim().length > 100) {
      errors.fullName = 'Họ và tên không được vượt quá 100 ký tự';
    }

    // 2. Kiểm tra Số điện thoại (đúng 10 chữ số, đầu số mạng 03, 05, 07, 08, 09)
    if (!addPhone.trim()) {
      errors.phoneNumber = 'Số điện thoại không được để trống';
    } else if (!/^(0[35789])[0-9]{8}$/.test(addPhone.trim())) {
      errors.phoneNumber = 'Số điện thoại không hợp lệ (phải gồm 10 chữ số và bắt đầu bằng 03, 05, 07, 08 hoặc 09)';
    }

    // 3. Kiểm tra định dạng Email hợp lệ theo chuẩn RFC
    if (!addEmail.trim()) {
      errors.email = 'Email không được để trống';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(addEmail.trim())) {
      errors.email = 'Email không đúng định dạng';
    } else if (addEmail.trim().length > 150) {
      errors.email = 'Email không được vượt quá 150 ký tự';
    }

    // 4. Kiểm tra độ mạnh Mật khẩu
    if (!addPassword) {
      errors.password = 'Mật khẩu không được để trống';
    } else if (/\s/.test(addPassword)) {
      errors.password = 'Mật khẩu không được chứa khoảng trắng';
    } else if (addPassword.length < 6 || addPassword.length > 64) {
      errors.password = 'Mật khẩu phải từ 6 đến 64 ký tự';
    } else if (!PASSWORD_REGEX.test(addPassword)) {
      errors.password = 'Mật khẩu phải bao gồm ít nhất 1 chữ cái, 1 chữ số và 1 ký tự đặc biệt';
    }

    // 5. Kiểm tra Xác nhận mật khẩu
    if (!addConfirmPassword) {
      errors.confirmPassword = 'Vui lòng xác nhận lại mật khẩu';
    } else if (addPassword !== addConfirmPassword) {
      errors.confirmPassword = 'Mật khẩu xác nhận không trùng khớp';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleCreateStaff = async (e: React.FormEvent) => {
    e.preventDefault();
    setServerError(null);

    if (!validateStaffForm()) return;

    setModalSubmitting(true);
    try {
      await adminUserService.createStaff({
        fullName: addFullName.trim(),
        phoneNumber: addPhone.trim(),
        email: addEmail.trim().toLowerCase(),
        password: addPassword,
        confirmPassword: addConfirmPassword,
      });

      showToast('Tạo tài khoản nhân viên thành công!', 'success');
      setIsAddModalOpen(false);
      await loadUsers();
    } catch (err: any) {
      setServerError(err.response?.data?.message || 'Không thể tạo tài khoản nhân viên');
    } finally {
      setModalSubmitting(false);
    }
  };

  // ---------------------------------------------------------------------------------------
  // 7. GIAO DIỆN CHÍNH (RENDER)
  // ---------------------------------------------------------------------------------------
  return (
    <div style={{ height: 'calc(100vh - 140px)', display: 'flex', flexDirection: 'column' }}>
      {/* Khung Card chính */}
      <div className="card" style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        {/* ===============================================================================
            7.1. THANH CÔNG CỤ TRÊN CÙNG (TOOLBAR)
        =============================================================================== */}
        <div className="flex justify-between items-center mb-6" style={{ flexShrink: 0, gap: '1rem', flexWrap: 'wrap' }}>
          {/* Ô nhập tìm kiếm từ khóa */}
          <div
            className="flex items-center gap-2"
            style={{
              height: '42px',
              border: '1px solid var(--color-border)',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'var(--color-bg-base)',
              width: '300px',
              padding: '0 1rem',
              flexShrink: 0,
            }}
          >
            <Search size={16} className="text-muted shrink-0" />
            <input
              type="text"
              placeholder="Tìm theo tên, email, sđt..."
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setPage(1);
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

          {/* Cụm bộ lọc Vai trò, Trạng thái & Nút Tạo nhân viên */}
          <div className="flex gap-4 items-center flex-wrap">
            {/* Dropdown lọc Vai trò */}
            <select
              value={roleFilter}
              onChange={(e) => {
                setRoleFilter(e.target.value);
                setPage(1);
              }}
              style={{
                height: '42px',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-md)',
                backgroundColor: 'var(--color-bg-base)',
                color: 'var(--color-text-base)',
                outline: 'none',
                padding: '0 1rem',
              }}
            >
              <option value="ALL">Tất cả vai trò</option>
              <option value="ADMIN">Quản trị viên</option>
              <option value="STAFF">Nhân viên</option>
              <option value="CUSTOMER">Khách hàng</option>
            </select>

            {/* Dropdown lọc Trạng thái */}
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(1);
              }}
              style={{
                height: '42px',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-md)',
                backgroundColor: 'var(--color-bg-base)',
                color: 'var(--color-text-base)',
                outline: 'none',
                padding: '0 1rem',
              }}
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang hoạt động</option>
              <option value="LOCKED">Đã khóa / Thu hồi</option>
            </select>

            {/* Nút Tạo nhân viên mới: Chỉ Quản trị viên mới có quyền tạo nhân viên */}
            {isCurrentUserAdmin && (
              <button
                type="button"
                className="btn btn-primary"
                style={{ height: '42px', padding: '0 1.25rem', whiteSpace: 'nowrap' }}
                onClick={handleOpenAddModal}
              >
                + Tạo nhân viên
              </button>
            )}
          </div>
        </div>

        {/* ===============================================================================
            7.2. BẢNG DANH SÁCH NGƯỜI DÙNG (STICKY HEADER TABLE)
        =============================================================================== */}
        <div
          style={{
            flex: 1,
            overflowY: 'auto',
            overflowX: 'auto',
            borderTop: '1px solid var(--color-border)',
            display: 'flex',
            flexDirection: 'column',
          }}
        >
          <table style={{ width: '100%', minWidth: '700px', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead style={{ position: 'sticky', top: 0, backgroundColor: 'var(--color-bg-surface)', zIndex: 10 }}>
              <tr style={{ borderBottom: '1px solid var(--color-border)' }}>
                <th className="p-4 font-semibold text-muted text-sm">HỌ VÀ TÊN</th>
                <th className="p-4 font-semibold text-muted text-sm">LIÊN HỆ</th>
                <th className="p-4 font-semibold text-muted text-sm">VAI TRÒ</th>
                <th className="p-4 font-semibold text-muted text-sm">TRẠNG THÁI</th>
                <th className="p-4 font-semibold text-muted text-sm text-left">THAO TÁC</th>
              </tr>
            </thead>
            <tbody>
              {/* Trạng thái 1: Đang tải */}
              {loading ? (
                <tr>
                  <td colSpan={5} className="p-8 text-center text-muted" style={{ verticalAlign: 'middle' }}>
                    <div style={{ minHeight: '220px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '0.75rem' }}>
                      <Loader2 size={24} className="animate-spin text-primary" />
                      <span>Đang tải danh sách người dùng...</span>
                    </div>
                  </td>
                </tr>
              ) : users.length === 0 ? (
                /* Trạng thái 2: Không có dữ liệu */
                <tr>
                  <td colSpan={5} className="p-8 text-center text-muted" style={{ verticalAlign: 'middle' }}>
                    <div style={{ minHeight: '180px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}>
                      <div className="text-base font-medium">Không tìm thấy người dùng nào phù hợp</div>
                      {searchTerm && (
                        <div className="text-xs text-muted">Thử tìm kiếm với từ khóa khác hoặc điều chỉnh lại bộ lọc</div>
                      )}
                    </div>
                  </td>
                </tr>
              ) : (
                /* Trạng thái 3: Hiển thị danh sách */
                users.map((user) => {
                  const isSelf = currentAdmin?.id === user.id;
                  const isAdmin = user.role === 'ROLE_ADMIN';
                  const isStaff = user.role === 'ROLE_STAFF';
                  const isCustomer = user.role === 'ROLE_CUSTOMER';
                  const canLockOrUnlock = !isSelf && !isAdmin && (isCustomer || (isCurrentUserAdmin && isStaff));

                  return (
                    <tr
                      key={user.id}
                      style={{ borderBottom: '1px solid var(--color-border)' }}
                      className="hover:bg-[var(--color-bg-base)]/50 transition-colors"
                    >
                      {/* Cột Họ và tên */}
                      <td className="p-4">
                        <div className="font-semibold flex items-center gap-2">
                          <span>{user.fullName}</span>
                          {isSelf && (
                            <span
                              style={{
                                fontSize: '0.725rem',
                                padding: '0.1rem 0.4rem',
                                borderRadius: '9999px',
                                backgroundColor: 'var(--color-primary-light)',
                                color: 'var(--color-primary)',
                                fontWeight: 600,
                              }}
                            >
                              Bạn
                            </span>
                          )}
                        </div>
                        <div className="text-xs text-muted mt-0.5">ID: {user.id}</div>
                      </td>

                      {/* Cột Liên hệ */}
                      <td className="p-4">
                        <div style={{ wordBreak: 'break-all' }}>{user.email}</div>
                        <div className="text-sm text-muted mt-0.5">{user.phoneNumber || 'Chưa cập nhật SĐT'}</div>
                      </td>

                      {/* Cột Vai trò */}
                      <td className="p-4" style={{ whiteSpace: 'nowrap' }}>
                        {isAdmin && (
                          <span
                            className="badge flex items-center gap-1 inline-flex"
                            style={{ backgroundColor: 'rgba(239, 68, 68, 0.15)', color: 'var(--color-danger)' }}
                          >
                            <ShieldAlert size={14} /> Quản trị viên
                          </span>
                        )}
                        {isStaff && (
                          <span
                            className="badge flex items-center gap-1 inline-flex"
                            style={{ backgroundColor: 'rgba(59, 130, 246, 0.15)', color: 'var(--color-secondary)' }}
                          >
                            <Shield size={14} /> Nhân viên
                          </span>
                        )}
                        {isCustomer && (
                          <span
                            className="badge inline-flex"
                            style={{ backgroundColor: 'var(--color-bg-base)', border: '1px solid var(--color-border)' }}
                          >
                            Khách hàng
                          </span>
                        )}
                      </td>

                      {/* Cột Trạng thái */}
                      <td className="p-4" style={{ whiteSpace: 'nowrap' }}>
                        {user.status === 'ACTIVE' ? (
                          <span className="badge badge-success">Đang hoạt động</span>
                        ) : (
                          <span className="badge badge-danger">Đã khóa / Thu hồi</span>
                        )}
                      </td>

                      {/* Cột Thao tác */}
                      <td className="p-4 text-left" style={{ whiteSpace: 'nowrap' }}>
                        <div className="flex gap-2 justify-start items-center">
                          {/* Nút Khóa / Thu hồi: Nhân viên chỉ khóa được khách hàng, Quản trị viên khóa được khách hàng & nhân viên */}
                          {user.status === 'ACTIVE' && canLockOrUnlock && (
                            <button
                              type="button"
                              className="btn btn-secondary text-danger"
                              style={{ padding: '0.5rem' }}
                              title="Khóa / Thu hồi quyền truy cập"
                              onClick={() => setLockUser(user)}
                            >
                              <UserX size={16} />
                            </button>
                          )}

                          {/* Nút Mở khóa / Kích hoạt lại: Tương tự điều kiện của Khóa / Thu hồi */}
                          {user.status === 'LOCKED' && canLockOrUnlock && (
                            <button
                              type="button"
                              className="btn btn-secondary text-success"
                              style={{ padding: '0.5rem' }}
                              title="Mở khóa / Kích hoạt lại tài khoản"
                              onClick={() => setUnlockUser(user)}
                            >
                              <UserCheck size={16} />
                            </button>
                          )}

                          {/* Nút Xem chi tiết và chỉnh sửa hồ sơ người dùng */}
                          <button
                            type="button"
                            className="btn btn-secondary"
                            style={{ padding: '0.5rem' }}
                            title={
                              isCurrentUserAdmin || (isCurrentUserStaff && isCustomer)
                                ? 'Xem và chỉnh sửa người dùng'
                                : 'Xem thông tin người dùng'
                            }
                            onClick={() => handleOpenViewModal(user)}
                          >
                            <MoreVertical size={16} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* ===============================================================================
            7.3. PHÂN TRANG GÓC PHẢI (RIGHT-ALIGNED PAGINATION)
        =============================================================================== */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center',
            gap: '1.25rem',
            flexShrink: 0,
            borderTop: '1px solid var(--color-border)',
            padding: '0.75rem 1.25rem',
          }}
        >
          <div className="text-sm text-muted">
            {totalElements > 0 ? (
              <>
                Hiển thị{' '}
                <span className="font-semibold text-[var(--color-text-base)]">
                  {(page - 1) * pageSize + 1}
                </span>{' '}
                -{' '}
                <span className="font-semibold text-[var(--color-text-base)]">
                  {Math.min(page * pageSize, totalElements)}
                </span>{' '}
                trong tổng số{' '}
                <span className="font-semibold text-[var(--color-text-base)]">{totalElements}</span> người dùng
              </>
            ) : (
              '0 người dùng'
            )}
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              className="btn btn-secondary"
              style={{ padding: '0.4rem 0.75rem', height: '36px' }}
              disabled={page <= 1 || loading}
              onClick={() => setPage((prev) => Math.max(1, prev - 1))}
            >
              <ChevronLeft size={16} /> Trước
            </button>

            <span className="text-sm font-semibold px-2" style={{ color: 'var(--color-text-base)' }}>
              {page} / {totalPages || 1}
            </span>

            <button
              type="button"
              className="btn btn-secondary"
              style={{ padding: '0.4rem 0.75rem', height: '36px' }}
              disabled={page >= totalPages || loading}
              onClick={() => setPage((prev) => Math.min(totalPages, prev + 1))}
            >
              Sau <ChevronRight size={16} />
            </button>
          </div>
        </div>
      </div>

      {/* ===============================================================================
          7.4. HỆ THỐNG CÁC MODALS (REACT PORTAL)
      =============================================================================== */}

      {/* MODAL 1: Thêm Nhân Viên Mới */}
      {isAddModalOpen && (
        <ModalOverlay onClose={() => !modalSubmitting && setIsAddModalOpen(false)}>
          <div className="flex justify-between items-center mb-6" style={{ flexShrink: 0 }}>
            <h2 className="text-xl font-bold">Thêm Nhân Viên Mới</h2>
            <button
              type="button"
              onClick={() => setIsAddModalOpen(false)}
              disabled={modalSubmitting}
              className="text-muted hover:text-[var(--color-text-base)]"
            >
              <X size={24} />
            </button>
          </div>

          {serverError && (
            <div
              className="mb-4 p-3 rounded text-sm text-center"
              style={{ backgroundColor: 'rgba(239, 68, 68, 0.1)', color: 'var(--color-danger)', border: '1px solid rgba(239, 68, 68, 0.2)' }}
            >
              {serverError}
            </div>
          )}

          <form onSubmit={handleCreateStaff} style={{ display: 'flex', flexDirection: 'column', flex: 1, overflow: 'hidden' }}>
            <div className="grid gap-4 mb-6" style={{ overflowY: 'auto', paddingRight: '0.5rem' }}>
              {/* Họ và tên */}
              <div>
                <label className="block text-sm font-semibold mb-2">Họ và Tên *</label>
                <input
                  type="text"
                  className="w-full"
                  placeholder="Nhập họ và tên nhân viên..."
                  value={addFullName}
                  onChange={(e) => {
                    setAddFullName(e.target.value);
                    if (fieldErrors.fullName) setFieldErrors((prev) => ({ ...prev, fullName: undefined }));
                  }}
                  style={{
                    padding: '0.6rem 1rem',
                    border: fieldErrors.fullName ? '1px solid var(--color-danger)' : '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--color-bg-base)',
                    color: 'var(--color-text-base)',
                  }}
                />
                {fieldErrors.fullName && (
                  <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{fieldErrors.fullName}</p>
                )}
              </div>

              {/* Số điện thoại */}
              <div>
                <label className="block text-sm font-semibold mb-2">Số điện thoại *</label>
                <input
                  type="text"
                  className="w-full"
                  placeholder="VD: 0909123456 (10 chữ số)..."
                  value={addPhone}
                  onChange={(e) => {
                    setAddPhone(e.target.value);
                    if (fieldErrors.phoneNumber) setFieldErrors((prev) => ({ ...prev, phoneNumber: undefined }));
                  }}
                  style={{
                    padding: '0.6rem 1rem',
                    border: fieldErrors.phoneNumber ? '1px solid var(--color-danger)' : '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--color-bg-base)',
                    color: 'var(--color-text-base)',
                  }}
                />
                {fieldErrors.phoneNumber && (
                  <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{fieldErrors.phoneNumber}</p>
                )}
              </div>

              {/* Email */}
              <div>
                <label className="block text-sm font-semibold mb-2">Email *</label>
                <input
                  type="email"
                  className="w-full"
                  placeholder="VD: staff@fpms.com..."
                  value={addEmail}
                  onChange={(e) => {
                    setAddEmail(e.target.value);
                    if (fieldErrors.email) setFieldErrors((prev) => ({ ...prev, email: undefined }));
                  }}
                  style={{
                    padding: '0.6rem 1rem',
                    border: fieldErrors.email ? '1px solid var(--color-danger)' : '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--color-bg-base)',
                    color: 'var(--color-text-base)',
                  }}
                />
                {fieldErrors.email && (
                  <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{fieldErrors.email}</p>
                )}
              </div>

              {/* Vai trò */}
              <div>
                <label className="block text-sm font-semibold mb-2">Vai trò</label>
                <input
                  type="text"
                  className="w-full"
                  value="Nhân viên"
                  readOnly
                  style={{
                    padding: '0.6rem 1rem',
                    border: '1px solid var(--color-border)',
                    borderRadius: 'var(--radius-md)',
                    background: 'var(--color-bg-surface)',
                    color: 'var(--color-text-muted)',
                    cursor: 'not-allowed',
                  }}
                />
              </div>

              {/* Mật khẩu */}
              <div>
                <label className="block text-sm font-semibold mb-2">Mật khẩu *</label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    className="w-full"
                    placeholder="Mật khẩu (tối thiểu 6 ký tự, gồm chữ, số, ký tự đặc biệt)..."
                    value={addPassword}
                    onChange={(e) => {
                      setAddPassword(e.target.value);
                      if (fieldErrors.password) setFieldErrors((prev) => ({ ...prev, password: undefined }));
                    }}
                    style={{
                      padding: '0.6rem 2.5rem 0.6rem 1rem',
                      border: fieldErrors.password ? '1px solid var(--color-danger)' : '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--color-bg-base)',
                      color: 'var(--color-text-base)',
                    }}
                  />
                  <button
                    type="button"
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted hover:text-[var(--color-text-base)]"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
                {fieldErrors.password && (
                  <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{fieldErrors.password}</p>
                )}
              </div>

              {/* Xác nhận mật khẩu */}
              <div>
                <label className="block text-sm font-semibold mb-2">Xác nhận mật khẩu *</label>
                <div className="relative">
                  <input
                    type={showConfirmPassword ? 'text' : 'password'}
                    className="w-full"
                    placeholder="Nhập lại mật khẩu..."
                    value={addConfirmPassword}
                    onChange={(e) => {
                      setAddConfirmPassword(e.target.value);
                      if (fieldErrors.confirmPassword) setFieldErrors((prev) => ({ ...prev, confirmPassword: undefined }));
                    }}
                    style={{
                      padding: '0.6rem 2.5rem 0.6rem 1rem',
                      border: fieldErrors.confirmPassword ? '1px solid var(--color-danger)' : '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--color-bg-base)',
                      color: 'var(--color-text-base)',
                    }}
                  />
                  <button
                    type="button"
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted hover:text-[var(--color-text-base)]"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  >
                    {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
                {fieldErrors.confirmPassword && (
                  <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{fieldErrors.confirmPassword}</p>
                )}
              </div>
            </div>

            <div style={{ flexShrink: 0, paddingTop: '1rem', marginTop: 'auto', borderTop: '1px solid var(--color-border)' }}>
              <button
                type="submit"
                disabled={modalSubmitting}
                className="btn btn-primary w-full flex items-center justify-center gap-2"
                style={{ height: '44px' }}
              >
                {modalSubmitting ? <Loader2 size={18} className="animate-spin" /> : null}
                <span>Tạo Tài Khoản</span>
              </button>
            </div>
          </form>
        </ModalOverlay>
      )}

      {/* MODAL 2: Thông Tin & Chỉnh Sửa Người Dùng */}
      {viewUser && (() => {
        const canEditTargetUser = isCurrentUserAdmin || (isCurrentUserStaff && viewUser.role === 'ROLE_CUSTOMER');

        return (
          <ModalOverlay onClose={() => !modalSubmitting && setViewUser(null)}>
            <div className="flex justify-between items-center mb-6" style={{ flexShrink: 0 }}>
              <div>
                <h2 className="text-xl font-bold">
                  {canEditTargetUser ? 'Thông Tin & Chỉnh Sửa' : 'Chi Tiết Người Dùng'}
                </h2>
                <div className="flex items-center gap-2 mt-1">
                  <span
                    style={{
                      fontSize: '0.75rem',
                      padding: '0.15rem 0.5rem',
                      borderRadius: 'var(--radius-sm)',
                      backgroundColor: 'var(--color-bg-base)',
                      border: '1px solid var(--color-border)',
                      fontWeight: 600,
                      color: 'var(--color-text-muted)',
                    }}
                  >
                    Mã ID: #{viewUser.id}
                  </span>
                  {currentAdmin?.id === viewUser.id && (
                    <span
                      style={{
                        fontSize: '0.725rem',
                        padding: '0.15rem 0.5rem',
                        borderRadius: 'var(--radius-sm)',
                        backgroundColor: 'var(--color-primary-light)',
                        color: 'var(--color-primary)',
                        fontWeight: 600,
                      }}
                    >
                      Tài khoản của bạn
                    </span>
                  )}
                </div>
              </div>
              <button
                type="button"
                onClick={() => !modalSubmitting && setViewUser(null)}
                className="text-muted hover:text-[var(--color-text-base)]"
              >
                <X size={24} />
              </button>
            </div>

            {/* Banner thông báo nếu tài khoản không thể chỉnh sửa bởi nhân viên */}
            {!canEditTargetUser && (
              <div
                className="mb-4 p-3 rounded text-sm flex items-center gap-2"
                style={{
                  backgroundColor: 'rgba(59, 130, 246, 0.08)',
                  color: 'var(--color-secondary)',
                  border: '1px solid rgba(59, 130, 246, 0.2)',
                }}
              >
                <Shield size={16} className="shrink-0" />
                <span>Nhân viên chỉ có quyền xem thông tin của nhân viên khác và quản trị viên, không thể chỉnh sửa.</span>
              </div>
            )}

            <form onSubmit={handleSaveEditUser} style={{ display: 'flex', flexDirection: 'column', flexGrow: 1, overflow: 'hidden' }}>
              <div className="grid gap-4 mb-6" style={{ overflowY: 'auto', paddingRight: '0.5rem' }}>
                {/* Họ và tên */}
                <div>
                  <label className="block text-sm font-semibold mb-2">
                    Họ và Tên {canEditTargetUser && '*'}
                  </label>
                  <input
                    type="text"
                    className="w-full"
                    value={editFullName}
                    readOnly={!canEditTargetUser}
                    onChange={(e) => {
                      if (!canEditTargetUser) return;
                      setEditFullName(e.target.value);
                      if (editErrors.fullName) setEditErrors((prev) => ({ ...prev, fullName: undefined }));
                    }}
                    style={{
                      padding: '0.6rem 1rem',
                      border: editErrors.fullName ? '1px solid var(--color-danger)' : '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-md)',
                      background: canEditTargetUser ? 'var(--color-bg-base)' : 'var(--color-bg-surface)',
                      color: canEditTargetUser ? 'var(--color-text-base)' : 'var(--color-text-muted)',
                      cursor: canEditTargetUser ? 'text' : 'not-allowed',
                    }}
                  />
                  {editErrors.fullName && (
                    <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{editErrors.fullName}</p>
                  )}
                </div>

                {/* Số điện thoại */}
                <div>
                  <label className="block text-sm font-semibold mb-2">
                    Số điện thoại {canEditTargetUser && '*'}
                  </label>
                  <input
                    type="text"
                    className="w-full"
                    placeholder="VD: 0912345678"
                    value={editPhone}
                    readOnly={!canEditTargetUser}
                    onChange={(e) => {
                      if (!canEditTargetUser) return;
                      setEditPhone(e.target.value);
                      if (editErrors.phoneNumber) setEditErrors((prev) => ({ ...prev, phoneNumber: undefined }));
                    }}
                    style={{
                      padding: '0.6rem 1rem',
                      border: editErrors.phoneNumber ? '1px solid var(--color-danger)' : '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-md)',
                      background: canEditTargetUser ? 'var(--color-bg-base)' : 'var(--color-bg-surface)',
                      color: canEditTargetUser ? 'var(--color-text-base)' : 'var(--color-text-muted)',
                      cursor: canEditTargetUser ? 'text' : 'not-allowed',
                    }}
                  />
                  {editErrors.phoneNumber && (
                    <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{editErrors.phoneNumber}</p>
                  )}
                </div>

                {/* Email (Read-only) */}
                <div>
                  <label className="block text-sm font-semibold mb-2">Email</label>
                  <input
                    type="email"
                    className="w-full"
                    value={viewUser.email}
                    readOnly
                    style={{
                      padding: '0.6rem 1rem',
                      border: '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--color-bg-surface)',
                      color: 'var(--color-text-muted)',
                      cursor: 'not-allowed',
                    }}
                  />
                  <span className="text-xs text-muted block mt-1">Email là tên định danh tài khoản và không thể thay đổi</span>
                </div>

                {/* Vai trò (Read-only, tiếng Việt, không in hoa) */}
                <div>
                  <label className="block text-sm font-semibold mb-2">Vai trò</label>
                  <input
                    type="text"
                    className="w-full"
                    value={
                      viewUser.role === 'ROLE_CUSTOMER'
                        ? 'Khách hàng'
                        : viewUser.role === 'ROLE_STAFF'
                        ? 'Nhân viên'
                        : 'Quản trị viên'
                    }
                    readOnly
                    style={{
                      padding: '0.6rem 1rem',
                      border: '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--color-bg-surface)',
                      color: 'var(--color-text-muted)',
                      cursor: 'not-allowed',
                    }}
                  />
                </div>

                {/* Trạng thái tài khoản (Read-only) */}
                <div>
                  <label className="block text-sm font-semibold mb-2">Trạng thái tài khoản</label>
                  <input
                    type="text"
                    className="w-full"
                    value={viewUser.status === 'ACTIVE' ? 'Đang hoạt động' : 'Đã bị khóa / Thu hồi'}
                    readOnly
                    style={{
                      padding: '0.6rem 1rem',
                      border: '1px solid var(--color-border)',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--color-bg-surface)',
                      color: viewUser.status === 'ACTIVE' ? 'var(--color-success)' : 'var(--color-danger)',
                      cursor: 'not-allowed',
                      fontWeight: 500,
                    }}
                  />
                </div>
              </div>

              {/* Footer Buttons: 2 nút khi chỉnh sửa được, 1 nút Đóng khi chỉ xem */}
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
                {canEditTargetUser ? (
                  <>
                    <button
                      type="button"
                      className="btn btn-secondary flex-1"
                      style={{ height: '42px' }}
                      disabled={modalSubmitting}
                      onClick={() => setViewUser(null)}
                    >
                      Hủy bỏ
                    </button>
                    <button
                      type="submit"
                      className="btn btn-primary flex-1 flex items-center justify-center gap-2"
                      style={{ height: '42px' }}
                      disabled={modalSubmitting}
                    >
                      {modalSubmitting ? <Loader2 size={18} className="animate-spin" /> : null}
                      <span>{modalSubmitting ? 'Đang lưu...' : 'Lưu thay đổi'}</span>
                    </button>
                  </>
                ) : (
                  <button
                    type="button"
                    className="btn btn-secondary w-full"
                    style={{ height: '42px' }}
                    onClick={() => setViewUser(null)}
                  >
                    Đóng
                  </button>
                )}
              </div>
            </form>
          </ModalOverlay>
        );
      })()}

      {/* MODAL 3: Xác Nhận Khóa / Thu Hồi Tài Khoản */}
      {lockUser && (
        <ModalOverlay onClose={() => !modalSubmitting && setLockUser(null)}>
          <div className="flex justify-between items-center mb-6" style={{ flexShrink: 0 }}>
            <h2 className="text-xl font-bold text-danger">Xác Nhận Khóa / Thu Hồi Tài Khoản</h2>
            <button
              type="button"
              onClick={() => setLockUser(null)}
              disabled={modalSubmitting}
              className="text-muted hover:text-[var(--color-text-base)]"
            >
              <X size={24} />
            </button>
          </div>

          <div className="mb-6">
            <p>
              Bạn có chắc chắn muốn khóa / thu hồi quyền truy cập tài khoản của <strong>{lockUser.fullName}</strong> không?
            </p>
            <p className="text-muted text-sm mt-2">
              Người dùng này sẽ bị thu hồi phiên làm việc tức thời, không thể đăng nhập hoặc đặt sân mới cho đến khi được kích hoạt lại.
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
              type="button"
              className="btn btn-secondary w-1/2"
              style={{ height: '42px' }}
              disabled={modalSubmitting}
              onClick={() => setLockUser(null)}
            >
              Hủy bỏ
            </button>
            <button
              type="button"
              className="btn btn-primary w-1/2 flex items-center justify-center gap-2"
              style={{ backgroundColor: 'var(--color-danger)', border: 'none', height: '42px' }}
              disabled={modalSubmitting}
              onClick={handleConfirmLock}
            >
              {modalSubmitting ? <Loader2 size={16} className="animate-spin" /> : null}
              <span>Khóa / Thu Hồi</span>
            </button>
          </div>
        </ModalOverlay>
      )}

      {/* MODAL 4: Xác Nhận Mở Khóa / Kích Hoạt Lại */}
      {unlockUser && (
        <ModalOverlay onClose={() => !modalSubmitting && setUnlockUser(null)}>
          <div className="flex justify-between items-center mb-6" style={{ flexShrink: 0 }}>
            <h2 className="text-xl font-bold text-success">Xác Nhận Kích Hoạt Lại</h2>
            <button
              type="button"
              onClick={() => setUnlockUser(null)}
              disabled={modalSubmitting}
              className="text-muted hover:text-[var(--color-text-base)]"
            >
              <X size={24} />
            </button>
          </div>

          <div className="mb-6">
            <p>
              Bạn có chắc chắn muốn mở khóa / kích hoạt lại tài khoản của <strong>{unlockUser.fullName}</strong> không?
            </p>
            <p className="text-muted text-sm mt-2">
              Người dùng này sẽ có thể đăng nhập và tiếp tục sử dụng hệ thống bình thường.
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
              type="button"
              className="btn btn-secondary w-1/2"
              style={{ height: '42px' }}
              disabled={modalSubmitting}
              onClick={() => setUnlockUser(null)}
            >
              Hủy bỏ
            </button>
            <button
              type="button"
              className="btn btn-primary w-1/2 flex items-center justify-center gap-2"
              style={{ backgroundColor: 'var(--color-success)', border: 'none', height: '42px' }}
              disabled={modalSubmitting}
              onClick={handleConfirmUnlock}
            >
              {modalSubmitting ? <Loader2 size={16} className="animate-spin" /> : null}
              <span>Kích Hoạt Lại</span>
            </button>
          </div>
        </ModalOverlay>
      )}
    </div>
  );
};

export default AdminUsers;
