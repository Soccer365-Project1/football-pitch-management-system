import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, RefreshCw, ArrowLeft } from 'lucide-react';
import { authService } from '../services/authService';
import { showToast, showAlert } from '../utils/toast';

const ForgotPassword: React.FC = () => {
  const navigate = useNavigate();

  // 1. Quản lý bước hiện tại (1: Nhập Email, 2: Nhập OTP & Mật khẩu mới)
  const [step, setStep] = useState<1 | 2>(1);

  // 2. Trạng thái Form dữ liệu
  const [email, setEmail] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  // 3. Trạng thái ẩn / hiện mật khẩu
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // 4. Quản lý trạng thái lỗi và tải (Loading)
  const [errors, setErrors] = useState<{
    email?: string;
    otpCode?: string;
    newPassword?: string;
    confirmPassword?: string;
  }>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  // 5. Quản lý đếm ngược gửi lại mã OTP (Cooldown 60 giây)
  const [cooldown, setCooldown] = useState(0);

  // Xử lý đếm lùi Cooldown mỗi giây
  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setInterval(() => {
      setCooldown((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [cooldown]);

  // Regex kiểm tra email hợp lệ
  const isValidEmail = (val: string): boolean => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val);
  };

  // ==========================================
  // XỬ LÝ BƯỚC 1: GỬI MÃ XÁC NHẬN OTP
  // ==========================================
  const handleSendOtp = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    setServerError(null);

    const cleanEmail = email.trim();
    if (!cleanEmail) {
      setErrors({ email: 'Vui lòng nhập địa chỉ email của bạn' });
      return;
    }
    if (!isValidEmail(cleanEmail)) {
      setErrors({ email: 'Địa chỉ email không đúng định dạng' });
      return;
    }

    setErrors({});
    setIsLoading(true);

    try {
      await authService.forgotPasswordApi({ email: cleanEmail });
      showToast('Mã xác thực OTP đã được gửi tới email của bạn!', 'success');
      setCooldown(60); // Bắt đầu đếm ngược 60 giây
      setStep(2);
    } catch (err: any) {
      const message =
        err.response?.data?.message ||
        'Không thể gửi mã xác nhận. Vui lòng kiểm tra lại email hoặc thử lại sau!';
      setServerError(message);
    } finally {
      setIsLoading(false);
    }
  };

  // ==========================================
  // XỬ LÝ GỬI LẠI MÃ OTP (RESEND OTP)
  // ==========================================
  const handleResendOtp = async () => {
    if (cooldown > 0 || isLoading) return;
    setServerError(null);
    setIsLoading(true);

    try {
      await authService.forgotPasswordApi({ email: email.trim() });
      showToast('Đã gửi lại mã xác thực mới vào email của bạn!', 'success');
      setCooldown(60);
      setOtpCode(''); // Xóa mã cũ để người dùng nhập mã mới
    } catch (err: any) {
      const message =
        err.response?.data?.message || 'Gửi lại mã thất bại. Vui lòng thử lại!';
      setServerError(message);
    } finally {
      setIsLoading(false);
    }
  };

  // ==========================================
  // XỬ LÝ BƯỚC 2: ĐẶT LẠI MẬT KHẨU
  // ==========================================
  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setServerError(null);

    const newErrors: {
      otpCode?: string;
      newPassword?: string;
      confirmPassword?: string;
    } = {};

    const cleanOtp = otpCode.trim();
    if (!cleanOtp) {
      newErrors.otpCode = 'Vui lòng nhập mã OTP 6 số';
    } else if (!/^\d{6}$/.test(cleanOtp)) {
      newErrors.otpCode = 'Mã OTP phải gồm chính xác 6 chữ số';
    }

    // Biểu thức chính quy kiểm tra độ mạnh của mật khẩu mới theo chuẩn bảo mật:
    // - 6 đến 64 ký tự: (?=.{6,64}$)
    // - Ít nhất 1 chữ cái: (?=.*[A-Za-z])
    // - Ít nhất 1 chữ số: (?=.*\d)
    // - Ít nhất 1 ký tự đặc biệt: (?=.*[^A-Za-z0-9\s])
    // - Chặn hoàn toàn khoảng trắng: \S+$
    const PASSWORD_REGEX = /^(?=.{6,64}$)(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S+$/;

    // Kiểm tra trường Mật khẩu mới
    if (!newPassword) {
      newErrors.newPassword = 'Vui lòng nhập mật khẩu mới';
    } else if (/\s/.test(newPassword)) {
      // Bắt ca biên chứa khoảng trắng
      newErrors.newPassword = 'Mật khẩu mới không được chứa khoảng trắng';
    } else if (newPassword.length < 6 || newPassword.length > 64) {
      newErrors.newPassword = 'Mật khẩu mới phải từ 6 đến 64 ký tự';
    } else if (!PASSWORD_REGEX.test(newPassword)) {
      newErrors.newPassword = 'Mật khẩu mới phải bao gồm ít nhất 1 chữ cái, 1 chữ số và 1 ký tự đặc biệt';
    }

    // Kiểm tra trường Xác nhận mật khẩu mới (phải khớp hoàn toàn với mật khẩu mới)
    if (!confirmPassword) {
      newErrors.confirmPassword = 'Vui lòng xác nhận lại mật khẩu mới';
    } else if (newPassword !== confirmPassword) {
      newErrors.confirmPassword = 'Mật khẩu xác nhận không trùng khớp';
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setErrors({});
    setIsLoading(true);

    try {
      await authService.resetPasswordApi({
        email: email.trim(),
        otpCode: cleanOtp,
        newPassword,
        confirmPassword,
      });

      showAlert(
        'Thành công!',
        'Mật khẩu của bạn đã được cập nhật thành công. Hãy đăng nhập bằng mật khẩu mới!',
        'success'
      );

      // Chuyển hướng về trang Đăng nhập sau khi hoàn tất
      navigate('/login', { replace: true });
    } catch (err: any) {
      const message =
        err.response?.data?.message ||
        'Đặt lại mật khẩu thất bại. Vui lòng kiểm tra lại mã OTP!';
      setServerError(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex justify-center items-start pt-4">
      <div className="card" style={{ width: '100%', maxWidth: '400px' }}>
        {/* Nút quay lại Bước 1 khi đang ở Bước 2 */}
        {step === 2 && (
          <button
            type="button"
            onClick={() => {
              setStep(1);
              setServerError(null);
            }}
            className="flex items-center gap-1 text-xs text-muted hover:text-primary mb-2 transition cursor-pointer"
          >
            <ArrowLeft size={14} /> Đổi email khác
          </button>
        )}

        <h2 className="text-2xl font-bold text-center mb-2">
          {step === 1 ? 'Quên mật khẩu?' : 'Tạo mật khẩu mới'}
        </h2>
        <p className="text-center text-muted text-sm mb-6">
          {step === 1
            ? 'Nhập email của bạn và chúng tôi sẽ gửi mã OTP để đặt lại mật khẩu.'
            : (
              <>
                Mã OTP đã được gửi đến email <strong className="text-primary">{email}</strong>. Vui lòng kiểm tra và thiết lập lại mật khẩu.
              </>
            )}
        </p>

        {/* Thông báo lỗi từ Backend */}
        {serverError && (
          <div
            className="mb-4 p-3 rounded text-sm text-center"
            style={{
              backgroundColor: 'rgba(239, 68, 68, 0.1)',
              color: 'var(--color-danger)',
              border: '1px solid var(--color-danger)',
            }}
          >
            {serverError}
          </div>
        )}

        <form onSubmit={step === 1 ? handleSendOtp : handleResetPassword} className="flex flex-col gap-4">
          {step === 1 ? (
            /* ================= BƯỚC 1: NHẬP EMAIL ================= */
            <div>
              <label className="font-semibold text-sm">Email đăng ký</label>
              <input
                type="email"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  if (errors.email) setErrors((prev) => ({ ...prev, email: undefined }));
                  if (serverError) setServerError(null);
                }}
                className="mt-2 w-full outline-none"
                style={{
                  padding: '0.75rem',
                  borderRadius: 'var(--radius-md)',
                  border: errors.email ? '1px solid var(--color-danger)' : '1px solid var(--color-border)',
                  backgroundColor: 'var(--color-bg-base)',
                  color: 'var(--color-text-base)',
                }}
                placeholder="name@example.com"
                disabled={isLoading}
                autoFocus
              />
              {errors.email && (
                <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>
                  {errors.email}
                </p>
              )}
            </div>
          ) : (
            /* ================= BƯỚC 2: NHẬP OTP & ĐẶT LẠI MẬT KHẨU ================= */
            <>
              {/* Ô Nhập mã OTP 6 số */}
              <div>
                <div className="flex justify-between items-center">
                  <label className="font-semibold text-sm">Mã OTP (6 chữ số)</label>
                  {/* Nút gửi lại mã kèm Countdown 60s */}
                  {cooldown > 0 ? (
                    <span className="text-xs text-muted">
                      Gửi lại sau ({cooldown}s)
                    </span>
                  ) : (
                    <button
                      type="button"
                      onClick={handleResendOtp}
                      disabled={isLoading}
                      className="text-xs flex items-center gap-1 cursor-pointer hover:underline"
                      style={{ color: 'var(--color-primary)' }}
                    >
                      <RefreshCw size={12} className={isLoading ? 'animate-spin' : ''} />
                      Gửi lại mã
                    </button>
                  )}
                </div>

                <input
                  type="text"
                  value={otpCode}
                  onChange={(e) => {
                    // Chỉ cho phép nhập số, tối đa 6 ký tự
                    const val = e.target.value.replace(/\D/g, '').slice(0, 6);
                    setOtpCode(val);
                    if (errors.otpCode) setErrors((prev) => ({ ...prev, otpCode: undefined }));
                    if (serverError) setServerError(null);
                  }}
                  className="mt-2 w-full outline-none"
                  style={{
                    padding: '0.75rem',
                    borderRadius: 'var(--radius-md)',
                    border: errors.otpCode ? '1px solid var(--color-danger)' : '1px solid var(--color-border)',
                    backgroundColor: 'var(--color-bg-base)',
                    color: 'var(--color-text-base)',
                    letterSpacing: '6px',
                    textAlign: 'center',
                    fontSize: '1.25rem',
                    fontWeight: 'bold',
                  }}
                  placeholder="------"
                  maxLength={6}
                  disabled={isLoading}
                  autoFocus
                />
                {errors.otpCode && (
                  <p className="mt-1 text-xs text-center" style={{ color: 'var(--color-danger)' }}>
                    {errors.otpCode}
                  </p>
                )}
              </div>

              {/* Ô Mật khẩu mới */}
              <div>
                <label className="font-semibold text-sm">Mật khẩu mới</label>
                <div className="relative mt-2">
                  <input
                    type={showNewPassword ? 'text' : 'password'}
                    value={newPassword}
                    onChange={(e) => {
                      setNewPassword(e.target.value);
                      if (errors.newPassword) setErrors((prev) => ({ ...prev, newPassword: undefined }));
                      if (serverError) setServerError(null);
                    }}
                    className="w-full outline-none pr-10"
                    style={{
                      padding: '0.75rem',
                      paddingRight: '2.5rem',
                      borderRadius: 'var(--radius-md)',
                      border: errors.newPassword ? '1px solid var(--color-danger)' : '1px solid var(--color-border)',
                      backgroundColor: 'var(--color-bg-base)',
                      color: 'var(--color-text-base)',
                    }}
                    placeholder="Tối thiểu 6 ký tự"
                    disabled={isLoading}
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
                {errors.newPassword ? (
                  <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>
                    {errors.newPassword}
                  </p>
                ) : (
                  <p className="mt-1 text-xs text-muted">Từ 6-64 ký tự, gồm chữ cái, chữ số, ký tự đặc biệt và không dấu cách.</p>
                )}
              </div>

              {/* Ô Xác nhận mật khẩu mới */}
              <div>
                <label className="font-semibold text-sm">Xác nhận mật khẩu mới</label>
                <div className="relative mt-2">
                  <input
                    type={showConfirmPassword ? 'text' : 'password'}
                    value={confirmPassword}
                    onChange={(e) => {
                      setConfirmPassword(e.target.value);
                      if (errors.confirmPassword) setErrors((prev) => ({ ...prev, confirmPassword: undefined }));
                      if (serverError) setServerError(null);
                    }}
                    className="w-full outline-none pr-10"
                    style={{
                      padding: '0.75rem',
                      paddingRight: '2.5rem',
                      borderRadius: 'var(--radius-md)',
                      border: errors.confirmPassword ? '1px solid var(--color-danger)' : '1px solid var(--color-border)',
                      backgroundColor: 'var(--color-bg-base)',
                      color: 'var(--color-text-base)',
                    }}
                    placeholder="Nhập lại mật khẩu mới"
                    disabled={isLoading}
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
                {errors.confirmPassword && (
                  <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>
                    {errors.confirmPassword}
                  </p>
                )}
              </div>
            </>
          )}

          {/* Nút Submit hành động */}
          <button
            type="submit"
            disabled={isLoading}
            className="btn btn-primary mt-2 flex items-center justify-center gap-2 cursor-pointer disabled:opacity-70 disabled:cursor-not-allowed"
            style={{ padding: '0.75rem', width: '100%' }}
          >
            {isLoading ? (
              <span className="flex items-center gap-2">
                <RefreshCw size={16} className="animate-spin" />
                {step === 1 ? 'Đang gửi mã...' : 'Đang đặt lại mật khẩu...'}
              </span>
            ) : (
              step === 1 ? 'Gửi mã xác nhận' : 'Đặt lại mật khẩu'
            )}
          </button>

          {/* Liên kết quay lại trang Đăng nhập */}
          <div className="text-center text-sm mt-2">
            <Link to="/login" className="font-semibold text-muted hover:text-primary transition">
              Quay lại đăng nhập
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ForgotPassword;
