import React, { useState, useEffect } from 'react';
import { Save, User, Loader2, Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { userService } from '../services/userService';
import { showToast, showConfirm } from '../utils/toast';

interface ProfileErrors {
  fullName?: string;
  phoneNumber?: string;
  general?: string;
}

interface PasswordErrors {
  currentPassword?: string;
  newPassword?: string;
  confirmPassword?: string;
  general?: string;
}

const Profile: React.FC = () => {
  const { user, updateUser } = useAuth();

  // Contact Info state
  const [fullName, setFullName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [profileLoading, setProfileLoading] = useState(false);
  const [profileErrors, setProfileErrors] = useState<ProfileErrors>({});

  // Change Password state
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordLoading, setPasswordLoading] = useState(false);
  const [passwordErrors, setPasswordErrors] = useState<PasswordErrors>({});

  // Quản lý trạng thái ẩn / hiện mật khẩu cho form Đổi mật khẩu (icon con mắt)
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  useEffect(() => {
    if (user) {
      setFullName(user.fullName || '');
      setPhoneNumber(user.phoneNumber || '');
    }
  }, [user]);

  // Xử lý Cập nhật thông tin cá nhân
  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    const errors: ProfileErrors = {};
    const phoneRegex = /^(0[35789])[0-9]{8}$/;

    if (!fullName.trim()) {
      errors.fullName = 'Vui lòng nhập họ và tên';
    } else if (fullName.trim().length < 2) {
      errors.fullName = 'Họ và tên phải có ít nhất 2 ký tự';
    }

    if (!phoneNumber.trim()) {
      errors.phoneNumber = 'Vui lòng nhập số điện thoại';
    } else if (!phoneRegex.test(phoneNumber.trim())) {
      errors.phoneNumber = 'Số điện thoại không hợp lệ (phải gồm 10 chữ số và bắt đầu bằng 03, 05, 07, 08 hoặc 09)';
    }

    if (Object.keys(errors).length > 0) {
      setProfileErrors(errors);
      return;
    }

    setProfileErrors({});

    // Hiển thị hộp thoại xác nhận trước khi cập nhật
    const isConfirmed = await showConfirm(
      'Xác nhận cập nhật',
      'Bạn có chắc chắn muốn thay đổi thông tin cá nhân không?',
      'Lưu thông tin'
    );
    if (!isConfirmed) return;

    setProfileLoading(true);
    try {
      const res = await userService.updateMyProfileApi({
        fullName: fullName.trim(),
        phoneNumber: phoneNumber.trim(),
      });

      if (res.data) {
        updateUser(res.data);
        showToast('Cập nhật thông tin thành công!', 'success');
      }
    } catch (err: any) {
      const code = err.response?.data?.code;
      const message = err.response?.data?.message || 'Cập nhật thông tin thất bại. Vui lòng thử lại!';
      if (code === 2002 || message.toLowerCase().includes('số điện thoại') || message.toLowerCase().includes('phone')) {
        setProfileErrors({ phoneNumber: 'Số điện thoại đã được sử dụng bởi tài khoản khác' });
      } else {
        setProfileErrors({ general: message });
      }
    } finally {
      setProfileLoading(false);
    }
  };

  // Biểu thức chính quy kiểm tra độ mạnh của mật khẩu mới theo chuẩn bảo mật:
  // - 6 đến 64 ký tự: (?=.{6,64}$)
  // - Ít nhất 1 chữ cái: (?=.*[A-Za-z])
  // - Ít nhất 1 chữ số: (?=.*\d)
  // - Ít nhất 1 ký tự đặc biệt: (?=.*[^A-Za-z0-9\s])
  // - Chặn hoàn toàn khoảng trắng: \S+$
  const PASSWORD_REGEX = /^(?=.{6,64}$)(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S+$/;

  // Xử lý sự kiện gửi form Đổi mật khẩu
  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    const errors: PasswordErrors = {};

    // 1. Kiểm tra trường Mật khẩu hiện tại
    if (!currentPassword) {
      errors.currentPassword = 'Vui lòng nhập mật khẩu hiện tại';
    }

    // 2. Kiểm tra tính hợp lệ và độ bảo mật của Mật khẩu mới
    if (!newPassword) {
      errors.newPassword = 'Vui lòng nhập mật khẩu mới';
    } else if (/\s/.test(newPassword)) {
      // Bắt ca biên: Mật khẩu mới chứa khoảng trắng
      errors.newPassword = 'Mật khẩu mới không được chứa khoảng trắng';
    } else if (newPassword.length < 6 || newPassword.length > 64) {
      // Giới hạn độ dài an toàn 6 đến 64 ký tự
      errors.newPassword = 'Mật khẩu mới phải từ 6 đến 64 ký tự';
    } else if (!PASSWORD_REGEX.test(newPassword)) {
      // Bắt buộc có chữ, số, ký tự đặc biệt
      errors.newPassword = 'Mật khẩu mới phải bao gồm ít nhất 1 chữ cái, 1 chữ số và 1 ký tự đặc biệt';
    } else if (currentPassword && newPassword === currentPassword) {
      // Ca biên bảo mật: Chặn người dùng đặt mật khẩu mới trùng với mật khẩu hiện tại
      errors.newPassword = 'Mật khẩu mới không được trùng với mật khẩu hiện tại';
    }

    // 3. Kiểm tra Xác nhận mật khẩu mới (phải khớp chính xác với mật khẩu mới)
    if (!confirmPassword) {
      errors.confirmPassword = 'Vui lòng xác nhận mật khẩu mới';
    } else if (newPassword && confirmPassword && newPassword !== confirmPassword) {
      errors.confirmPassword = 'Mật khẩu xác nhận không khớp với mật khẩu mới';
    }

    // Nếu có lỗi validation client-side thì dừng lại và hiển thị lỗi
    if (Object.keys(errors).length > 0) {
      setPasswordErrors(errors);
      return;
    }

    setPasswordErrors({});

    // Hiển thị hộp thoại xác nhận trước khi gọi API đổi mật khẩu
    const isConfirmed = await showConfirm(
      'Xác nhận đổi mật khẩu',
      'Bạn có chắc chắn muốn cập nhật mật khẩu mới không?',
      'Đổi mật khẩu'
    );
    if (!isConfirmed) return;

    setPasswordLoading(true);
    try {
      // Gọi API đổi mật khẩu lên server
      await userService.changePasswordApi({
        currentPassword,
        newPassword,
        confirmPassword,
      });

      showToast('Đổi mật khẩu thành công!', 'success');
      // Reset form sau khi đổi mật khẩu thành công
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err: any) {
      const code = err.response?.data?.code;
      const message = err.response?.data?.message || 'Đổi mật khẩu thất bại. Vui lòng kiểm tra lại!';
      
      // Xử lý các mã lỗi nghiệp vụ từ backend trả về
      if (code === 2016 || message.toLowerCase().includes('hiện tại') || message.toLowerCase().includes('current password')) {
        setPasswordErrors({ currentPassword: 'Mật khẩu hiện tại không chính xác' });
      } else if (code === 2017 || message.toLowerCase().includes('trùng')) {
        // Mã lỗi 2017: Mật khẩu mới không được trùng với mật khẩu hiện tại
        setPasswordErrors({ newPassword: 'Mật khẩu mới không được trùng với mật khẩu hiện tại' });
      } else if (code === 2007 || message.toLowerCase().includes('khớp') || message.toLowerCase().includes('confirm')) {
        setPasswordErrors({ confirmPassword: 'Mật khẩu xác nhận không khớp' });
      } else {
        setPasswordErrors({ general: message });
      }
    } finally {
      setPasswordLoading(false);
    }
  };

  return (
    <div className="max-w-[1400px] mx-auto px-4 flex flex-col gap-6 pb-12">
      {/* Header Card với Background Gradient và Unsplash Image */}
      <div 
        className="shadow-lg relative overflow-hidden"
        style={{ 
          borderRadius: '1rem',
          background: "linear-gradient(135deg, rgba(5,150,105,0.9) 0%, rgba(16,185,129,0.85) 50%, rgba(6,182,212,0.9) 100%), url('https://images.unsplash.com/photo-1518605368461-1ee7e53f0b2f?q=80&w=2070&auto=format&fit=crop') center/cover no-repeat",
          color: 'white',
          padding: '3rem 2rem',
          textAlign: 'center',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center'
        }}
      >
        <h1 className="text-4xl font-bold mb-3 flex items-center justify-center gap-3 text-white">
          <User size={36} />
          <span>Hồ sơ cá nhân</span>
        </h1>
        <p className="text-lg" style={{ color: 'rgba(255, 255, 255, 0.9)' }}>
          Quản lý thông tin và cài đặt bảo mật của bạn.
        </p>
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Contact Info Card */}
        <div className="card h-full flex flex-col">
          <h2 className="text-xl font-semibold mb-6 pb-2" style={{ borderBottom: '1px solid var(--color-border)' }}>
            Thông tin liên hệ
          </h2>

          <form onSubmit={handleUpdateProfile} className="flex flex-col gap-4 h-full">
            <div className="flex flex-col gap-4">
              <div>
                <label className="font-semibold text-sm">Họ và tên</label>
                <input 
                  type="text" 
                  value={fullName}
                  onChange={(e) => {
                    setFullName(e.target.value);
                    if (profileErrors.fullName) setProfileErrors(prev => ({ ...prev, fullName: undefined }));
                  }}
                  className="mt-2 w-full outline-none" 
                  style={{ 
                    padding: '0.75rem', 
                    borderRadius: 'var(--radius-md)', 
                    border: profileErrors.fullName ? '1px solid var(--color-danger)' : '1px solid var(--color-border)', 
                    backgroundColor: 'var(--color-bg-base)', 
                    color: 'var(--color-text-base)' 
                  }} 
                />
                {profileErrors.fullName && (
                  <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{profileErrors.fullName}</p>
                )}
              </div>

              <div>
                <label className="font-semibold text-sm">Số điện thoại</label>
                <input 
                  type="text" 
                  value={phoneNumber}
                  onChange={(e) => {
                    setPhoneNumber(e.target.value);
                    if (profileErrors.phoneNumber) setProfileErrors(prev => ({ ...prev, phoneNumber: undefined }));
                  }}
                  className="mt-2 w-full outline-none" 
                  style={{ 
                    padding: '0.75rem', 
                    borderRadius: 'var(--radius-md)', 
                    border: profileErrors.phoneNumber ? '1px solid var(--color-danger)' : '1px solid var(--color-border)', 
                    backgroundColor: 'var(--color-bg-base)', 
                    color: 'var(--color-text-base)' 
                  }} 
                />
                {profileErrors.phoneNumber && (
                  <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{profileErrors.phoneNumber}</p>
                )}
              </div>
            </div>
            
            <div>
              <div className="flex items-center gap-2">
                <label className="font-semibold text-sm">Email</label>
                <span className="text-xs text-muted font-normal">(Không thể thay đổi sau khi đăng ký)</span>
              </div>
              <input 
                type="email" 
                value={user?.email || ''} 
                className="mt-2 w-full" 
                style={{ 
                  padding: '0.75rem', 
                  borderRadius: 'var(--radius-md)', 
                  border: '1px solid var(--color-border)', 
                  backgroundColor: 'var(--color-bg-base)', 
                  color: 'var(--color-text-base)',
                  opacity: 0.7,
                  cursor: 'not-allowed'
                }} 
                disabled 
              />
            </div>

            {profileErrors.general && (
              <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{profileErrors.general}</p>
            )}
            
            <div className="mt-auto pt-4 flex justify-end">
              <button 
                type="submit" 
                disabled={profileLoading}
                className="btn btn-primary cursor-pointer disabled:opacity-70 disabled:cursor-not-allowed flex items-center justify-center gap-2" 
                style={{ padding: '0.6rem 1.5rem' }}
              >
                {profileLoading ? (
                  <>
                    <Loader2 size={18} className="animate-spin" />
                    <span>Đang lưu...</span>
                  </>
                ) : (
                  <>
                    <Save size={18} />
                    <span>Lưu thông tin</span>
                  </>
                )}
              </button>
            </div>
          </form>
        </div>
        
        {/* Change Password Card */}
        <div className="card h-full flex flex-col">
          <h2 className="text-xl font-semibold mb-6 pb-2" style={{ borderBottom: '1px solid var(--color-border)' }}>
            Đổi mật khẩu
          </h2>

          <form onSubmit={handleChangePassword} className="flex flex-col gap-4 h-full">
            {/* 1. Ô Mật khẩu hiện tại */}
            <div>
              <label className="font-semibold text-sm">Mật khẩu hiện tại</label>
              <div className="relative mt-2">
                <input 
                  type={showCurrentPassword ? 'text' : 'password'} 
                  value={currentPassword}
                  onChange={(e) => {
                    setCurrentPassword(e.target.value);
                    if (passwordErrors.currentPassword) setPasswordErrors(prev => ({ ...prev, currentPassword: undefined }));
                  }}
                  placeholder="••••••••"
                  className="w-full block outline-none pr-10" 
                  style={{ 
                    padding: '0.75rem', 
                    paddingRight: '2.5rem',
                    borderRadius: 'var(--radius-md)', 
                    border: passwordErrors.currentPassword ? '1px solid var(--color-danger)' : '1px solid var(--color-border)', 
                    backgroundColor: 'var(--color-bg-base)', 
                    color: 'var(--color-text-base)' 
                  }} 
                />
                {/* Nút bấm chuyển đổi ẩn/hiện mật khẩu hiện tại */}
                <button
                  type="button"
                  onClick={() => setShowCurrentPassword((prev) => !prev)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted hover:text-base cursor-pointer bg-transparent border-none p-0 flex items-center justify-center"
                  tabIndex={-1}
                  aria-label={showCurrentPassword ? "Ẩn mật khẩu hiện tại" : "Hiện mật khẩu hiện tại"}
                >
                  {showCurrentPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {passwordErrors.currentPassword && (
                <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{passwordErrors.currentPassword}</p>
              )}
            </div>

            {/* 2. Ô Mật khẩu mới */}
            <div>
              <label className="font-semibold text-sm">Mật khẩu mới</label>
              <div className="relative mt-2">
                <input 
                  type={showNewPassword ? 'text' : 'password'} 
                  value={newPassword}
                  onChange={(e) => {
                    setNewPassword(e.target.value);
                    if (passwordErrors.newPassword) setPasswordErrors(prev => ({ ...prev, newPassword: undefined }));
                  }}
                  placeholder="••••••••"
                  className="w-full block outline-none pr-10" 
                  style={{ 
                    padding: '0.75rem', 
                    paddingRight: '2.5rem',
                    borderRadius: 'var(--radius-md)', 
                    border: passwordErrors.newPassword ? '1px solid var(--color-danger)' : '1px solid var(--color-border)', 
                    backgroundColor: 'var(--color-bg-base)', 
                    color: 'var(--color-text-base)' 
                  }} 
                />
                {/* Nút bấm chuyển đổi ẩn/hiện mật khẩu mới */}
                <button
                  type="button"
                  onClick={() => setShowNewPassword((prev) => !prev)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted hover:text-base cursor-pointer bg-transparent border-none p-0 flex items-center justify-center"
                  tabIndex={-1}
                  aria-label={showNewPassword ? "Ẩn mật khẩu mới" : "Hiện mật khẩu mới"}
                >
                  {showNewPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {passwordErrors.newPassword ? (
                <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{passwordErrors.newPassword}</p>
              ) : (
                <p className="mt-1 text-xs text-muted">Từ 6-64 ký tự, gồm chữ cái, chữ số, ký tự đặc biệt và không dấu cách.</p>
              )}
            </div>

            {/* 3. Ô Xác nhận mật khẩu mới */}
            <div>
              <label className="font-semibold text-sm">Xác nhận mật khẩu mới</label>
              <div className="relative mt-2">
                <input 
                  type={showConfirmPassword ? 'text' : 'password'} 
                  value={confirmPassword}
                  onChange={(e) => {
                    setConfirmPassword(e.target.value);
                    if (passwordErrors.confirmPassword) setPasswordErrors(prev => ({ ...prev, confirmPassword: undefined }));
                  }}
                  placeholder="••••••••"
                  className="w-full block outline-none pr-10" 
                  style={{ 
                    padding: '0.75rem', 
                    paddingRight: '2.5rem',
                    borderRadius: 'var(--radius-md)', 
                    border: passwordErrors.confirmPassword ? '1px solid var(--color-danger)' : '1px solid var(--color-border)', 
                    backgroundColor: 'var(--color-bg-base)', 
                    color: 'var(--color-text-base)' 
                  }} 
                />
                {/* Nút bấm chuyển đổi ẩn/hiện mật khẩu xác nhận */}
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword((prev) => !prev)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted hover:text-base cursor-pointer bg-transparent border-none p-0 flex items-center justify-center"
                  tabIndex={-1}
                  aria-label={showConfirmPassword ? "Ẩn mật khẩu xác nhận" : "Hiện mật khẩu xác nhận"}
                >
                  {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {passwordErrors.confirmPassword && (
                <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{passwordErrors.confirmPassword}</p>
              )}
            </div>

            {passwordErrors.general && (
              <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{passwordErrors.general}</p>
            )}
            
            <div className="mt-auto pt-4 flex justify-end">
              <button 
                type="submit" 
                disabled={passwordLoading}
                className="btn btn-secondary cursor-pointer disabled:opacity-70 disabled:cursor-not-allowed flex items-center justify-center gap-2" 
                style={{ padding: '0.6rem 1.5rem' }}
              >
                {passwordLoading ? (
                  <>
                    <Loader2 size={18} className="animate-spin" />
                    <span>Đang cập nhật...</span>
                  </>
                ) : (
                  <span>Cập nhật mật khẩu</span>
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default Profile;
