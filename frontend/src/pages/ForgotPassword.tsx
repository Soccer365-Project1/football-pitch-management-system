import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff, RefreshCw, ArrowLeft, KeyRound, Mail, Lock } from 'lucide-react';
import { authService } from '../services/authService';
import { showToast, showAlert } from '../utils/toast';

/**
 * Trang Quên Mật Khẩu (ForgotPassword)
 * 
 * Luồng hoạt động gồm 3 bước tách biệt rõ ràng, tuần tự và an toàn:
 * - Bước 1: Người dùng nhập email đăng ký -> Hệ thống gửi mã OTP 6 số qua email.
 * - Bước 2: Người dùng nhập mã OTP 6 số -> Hệ thống kiểm tra trước (verify) tính hợp lệ và thời hạn của mã OTP.
 * - Bước 3: Sau khi OTP hợp lệ -> Cho phép người dùng nhập mật khẩu mới và xác nhận để hoàn tất đặt lại mật khẩu.
 */
const ForgotPassword: React.FC = () => {
  const navigate = useNavigate();

  // =========================================================================
  // 1. QUẢN LÝ CÁC STATE (TRẠNG THÁI DỮ LIỆU)
  // =========================================================================

  /**
   * State quản lý bước hiện tại trong quy trình:
   * - 1: Bước nhập Email và yêu cầu gửi mã OTP
   * - 2: Bước nhập mã xác thực OTP 6 số và kiểm tra trước với máy chủ
   * - 3: Bước nhập mật khẩu mới và xác nhận mật khẩu
   */
  const [step, setStep] = useState<1 | 2 | 3>(1);

  /** State lưu trữ giá trị email người dùng nhập */
  const [email, setEmail] = useState('');

  /** State lưu trữ mã OTP 6 chữ số người dùng nhập */
  const [otpCode, setOtpCode] = useState('');

  /** State lưu trữ mật khẩu mới */
  const [newPassword, setNewPassword] = useState('');

  /** State lưu trữ mật khẩu nhập lại để xác nhận */
  const [confirmPassword, setConfirmPassword] = useState('');

  /** State bật/tắt hiển thị mật khẩu mới (dạng text hoặc password) */
  const [showNewPassword, setShowNewPassword] = useState(false);

  /** State bật/tắt hiển thị mật khẩu xác nhận (dạng text hoặc password) */
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  /** State quản lý thông báo lỗi chi tiết cho từng trường input */
  const [errors, setErrors] = useState<{
    email?: string;
    otpCode?: string;
    newPassword?: string;
    confirmPassword?: string;
  }>({});

  /** State lưu trữ thông báo lỗi trả về từ máy chủ Backend (nếu có) */
  const [serverError, setServerError] = useState<string | null>(null);

  /** State theo dõi trạng thái đang gửi request API lên server (để hiển thị spinner và disable nút bấm) */
  const [isLoading, setIsLoading] = useState(false);

  /** State đếm ngược thời gian chờ gửi lại mã OTP (Cooldown tính bằng giây, mặc định 60s sau khi gửi) */
  const [cooldown, setCooldown] = useState(0);

  // =========================================================================
  // 2. CÁC HOOKS VÀ TIỆN ÍCH HỖ TRỢ
  // =========================================================================

  /**
   * Hook useEffect: Xử lý bộ đếm lùi thời gian Cooldown gửi lại mã OTP.
   * Cứ mỗi 1000ms (1 giây), giá trị cooldown sẽ giảm đi 1 đơn vị cho đến khi về 0.
   */
  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setInterval(() => {
      setCooldown((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [cooldown]);

  /**
   * Hàm kiểm tra định dạng email hợp lệ bằng Regular Expression chuẩn RFC.
   * @param val Chuỗi email cần kiểm tra
   * @returns true nếu hợp lệ, false nếu không đúng định dạng chuẩn
   */
  const isValidEmail = (val: string): boolean => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val);
  };

  // =========================================================================
  // 3. CÁC HÀM XỬ LÝ SỰ KIỆN (HANDLERS)
  // =========================================================================

  /**
   * XỬ LÝ BƯỚC 1: Tiếp nhận email và gửi yêu cầu sinh mã OTP về hộp thư của người dùng.
   * Xử lý các ca biên (Edge cases):
   * - Bỏ trống email: Báo lỗi yêu cầu nhập email.
   * - Sai định dạng email: Báo lỗi định dạng email.
   * - Server lỗi hoặc tài khoản không tồn tại: Bắt lỗi từ Backend và hiển thị cho người dùng.
   */
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
      // Gọi API forgotPasswordApi (POST /api/v1/auth/forgot-password)
      await authService.forgotPasswordApi({ email: cleanEmail });
      showToast('Mã xác thực OTP đã được gửi tới email của bạn!', 'success');
      setCooldown(60); // Bắt đầu đếm ngược 60 giây chờ gửi lại
      setStep(2);      // Chuyển sang Bước 2: Nhập và xác thực mã OTP
    } catch (err: any) {
      const message =
        err.response?.data?.message ||
        'Không thể gửi mã xác nhận. Vui lòng kiểm tra lại email hoặc thử lại sau!';
      setServerError(message);
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * XỬ LÝ GỬI LẠI MÃ OTP (RESEND OTP)
   * Chỉ cho phép thực hiện khi thời gian cooldown đã hết (cooldown === 0) và không đang trong tiến trình tải.
   */
  const handleResendOtp = async () => {
    if (cooldown > 0 || isLoading) return;
    setServerError(null);
    setIsLoading(true);

    try {
      await authService.forgotPasswordApi({ email: email.trim() });
      showToast('Đã gửi lại mã xác thực mới vào email của bạn!', 'success');
      setCooldown(60); // Khởi tạo lại chu kỳ cooldown 60 giây
      setOtpCode('');  // Reset ô nhập mã OTP để người dùng điền mã mới
    } catch (err: any) {
      const message =
        err.response?.data?.message || 'Gửi lại mã thất bại. Vui lòng thử lại sau!';
      setServerError(message);
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * XỬ LÝ BƯỚC 2: Kiểm tra tính hợp lệ của mã OTP trước khi cho phép nhập mật khẩu mới.
   * Xử lý các ca biên (Edge cases):
   * - Bỏ trống mã OTP: Báo lỗi yêu cầu nhập đủ.
   * - Mã OTP không đúng định dạng 6 số: Báo lỗi định dạng.
   * - Mã OTP sai hoặc hết hạn: Gọi API verify-otp để backend kiểm tra và bắt lỗi chính xác.
   */
  const handleVerifyOtp = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    setServerError(null);

    const cleanOtp = otpCode.trim();
    if (!cleanOtp) {
      setErrors({ otpCode: 'Vui lòng nhập mã OTP 6 số' });
      return;
    }
    if (!/^\d{6}$/.test(cleanOtp)) {
      setErrors({ otpCode: 'Mã OTP phải gồm chính xác 6 chữ số' });
      return;
    }

    setErrors({});
    setIsLoading(true);

    try {
      // Gọi API verifyOtpApi (POST /api/v1/auth/verify-otp) để kiểm tra sơ bộ
      await authService.verifyOtpApi({
        email: email.trim(),
        otpCode: cleanOtp,
      });

      showToast('Mã OTP chính xác! Vui lòng thiết lập mật khẩu mới', 'success');
      setStep(3); // Xác thực thành công -> Chuyển sang Bước 3: Đặt mật khẩu mới
    } catch (err: any) {
      const message =
        err.response?.data?.message ||
        'Mã OTP không chính xác hoặc đã hết thời gian hiệu lực!';
      setServerError(message);
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * XỬ LÝ BƯỚC 3: Tiếp nhận mật khẩu mới, kiểm tra tính hợp lệ bảo mật và gọi API đặt lại mật khẩu.
   * Xử lý các ca biên (Edge cases):
   * - Mật khẩu mới dưới 6 ký tự hoặc trên 64 ký tự: Báo lỗi độ dài.
   * - Chứa khoảng trắng: Báo lỗi không được có khoảng trắng.
   * - Không đủ độ phức tạp (chữ cái, chữ số, ký tự đặc biệt): Báo lỗi chuẩn bảo mật.
   * - Mật khẩu xác nhận không trùng khớp: Báo lỗi không khớp.
   * - Lỗi từ phía Server: Hiển thị thông báo phản hồi từ hệ thống.
   */
  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setServerError(null);

    const newErrors: {
      newPassword?: string;
      confirmPassword?: string;
    } = {};

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
      // Gọi API resetPasswordApi (POST /api/v1/auth/reset-password)
      await authService.resetPasswordApi({
        email: email.trim(),
        otpCode: otpCode.trim(),
        newPassword,
        confirmPassword,
      });

      showAlert(
        'Thành công!',
        'Mật khẩu của bạn đã được cập nhật thành công. Hãy đăng nhập bằng mật khẩu mới!',
        'success'
      );

      // Điều hướng về màn hình Đăng nhập sau khi hoàn thành quy trình
      navigate('/login', { replace: true });
    } catch (err: any) {
      const message =
        err.response?.data?.message ||
        'Đặt lại mật khẩu thất bại. Vui lòng kiểm tra lại thông tin!';
      setServerError(message);
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * Bộ điều phối Submit Form theo từng bước hiện tại (1, 2 hoặc 3)
   */
  const handleSubmit = (e: React.FormEvent) => {
    if (step === 1) {
      handleSendOtp(e);
    } else if (step === 2) {
      handleVerifyOtp(e);
    } else {
      handleResetPassword(e);
    }
  };

  // =========================================================================
  // 4. GIAO DIỆN HIỂN THỊ (RENDER UI)
  // =========================================================================
  return (
    <div className="flex justify-center items-start pt-4">
      <div className="card" style={{ width: '100%', maxWidth: '420px' }}>
        
        {/* Nút quay lại bước trước đó */}
        {step === 2 && (
          <button
            type="button"
            onClick={() => {
              setStep(1);
              setServerError(null);
            }}
            className="flex items-center gap-1 text-xs text-muted hover:text-primary mb-3 transition cursor-pointer"
          >
            <ArrowLeft size={14} /> Đổi email khác
          </button>
        )}

        {step === 3 && (
          <button
            type="button"
            onClick={() => {
              setStep(2);
              setServerError(null);
            }}
            className="flex items-center gap-1 text-xs text-muted hover:text-primary mb-3 transition cursor-pointer"
          >
            <ArrowLeft size={14} /> Quay lại nhập mã OTP
          </button>
        )}

        {/* Thanh chỉ báo các bước (Step Progress Indicator) */}
        <div className="flex items-center justify-center gap-2 mb-5">
          {/* Bước 1: Email */}
          <div
            className="flex items-center justify-center w-7 h-7 rounded-full text-xs font-bold transition-all"
            style={{
              backgroundColor: step >= 1 ? 'var(--color-primary)' : 'var(--color-bg-base)',
              color: step >= 1 ? '#ffffff' : 'var(--color-text-muted)',
              border: '1px solid var(--color-border)',
            }}
            title="Bước 1: Nhập Email"
          >
            1
          </div>
          <div
            className="w-10 h-0.5 transition-all"
            style={{ backgroundColor: step >= 2 ? 'var(--color-primary)' : 'var(--color-border)' }}
          />

          {/* Bước 2: OTP */}
          <div
            className="flex items-center justify-center w-7 h-7 rounded-full text-xs font-bold transition-all"
            style={{
              backgroundColor: step >= 2 ? 'var(--color-primary)' : 'var(--color-bg-base)',
              color: step >= 2 ? '#ffffff' : 'var(--color-text-muted)',
              border: '1px solid var(--color-border)',
            }}
            title="Bước 2: Xác thực OTP"
          >
            2
          </div>
          <div
            className="w-10 h-0.5 transition-all"
            style={{ backgroundColor: step >= 3 ? 'var(--color-primary)' : 'var(--color-border)' }}
          />

          {/* Bước 3: Mật khẩu mới */}
          <div
            className="flex items-center justify-center w-7 h-7 rounded-full text-xs font-bold transition-all"
            style={{
              backgroundColor: step >= 3 ? 'var(--color-primary)' : 'var(--color-bg-base)',
              color: step >= 3 ? '#ffffff' : 'var(--color-text-muted)',
              border: '1px solid var(--color-border)',
            }}
            title="Bước 3: Đặt mật khẩu mới"
          >
            3
          </div>
        </div>

        {/* Tiêu đề từng bước */}
        <h2 className="text-2xl font-bold text-center mb-2">
          {step === 1 && 'Quên mật khẩu?'}
          {step === 2 && 'Xác thực mã OTP'}
          {step === 3 && 'Tạo mật khẩu mới'}
        </h2>

        {/* Đoạn mô tả hướng dẫn chi tiết theo ngữ cảnh của từng bước */}
        <p className="text-center text-muted text-sm mb-6">
          {step === 1 && (
            'Nhập email đã đăng ký của bạn và hệ thống sẽ gửi mã xác thực OTP để tiến hành đặt lại mật khẩu.'
          )}
          {step === 2 && (
            <>
              Mã xác thực gồm 6 chữ số đã được gửi đến email{' '}
              <strong className="text-primary">{email}</strong>. Vui lòng kiểm tra hộp thư (bao gồm cả thư rác/spam).
            </>
          )}
          {step === 3 && (
            <>
              Mã OTP đã được xác minh thành công! Vui lòng thiết lập mật khẩu mới an toàn cho tài khoản{' '}
              <strong className="text-primary">{email}</strong>.
            </>
          )}
        </p>

        {/* Hiển thị lỗi từ phía Backend Server */}
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

        {/* Form xử lý thông tin */}
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          
          {/* ============================================================ */}
          {/* GIAO DIỆN BƯỚC 1: NHẬP ĐỊA CHỈ EMAIL                         */}
          {/* ============================================================ */}
          {step === 1 && (
            <div>
              <label className="font-semibold text-sm flex items-center gap-1.5">
                <Mail size={16} /> Email đăng ký
              </label>
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
          )}

          {/* ============================================================ */}
          {/* GIAO DIỆN BƯỚC 2: NHẬP VÀ XÁC THỰC MÃ OTP (CHỈ NHẬP OTP)     */}
          {/* ============================================================ */}
          {step === 2 && (
            <div>
              <div className="flex justify-between items-center">
                <label className="font-semibold text-sm flex items-center gap-1.5">
                  <KeyRound size={16} /> Mã OTP (6 chữ số)
                </label>
                
                {/* Khu vực đếm ngược Cooldown và Nút gửi lại mã */}
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

              {/* Ô input mã OTP 6 số với kiểu dáng nổi bật, căn giữa, dãn ký tự */}
              <input
                type="text"
                value={otpCode}
                onChange={(e) => {
                  // Chỉ lọc ký tự số, tối đa 6 ký tự
                  const val = e.target.value.replace(/\D/g, '').slice(0, 6);
                  setOtpCode(val);
                  if (errors.otpCode) setErrors((prev) => ({ ...prev, otpCode: undefined }));
                  if (serverError) setServerError(null);
                }}
                className="mt-2 w-full outline-none font-mono"
                style={{
                  padding: '0.85rem',
                  borderRadius: 'var(--radius-md)',
                  border: errors.otpCode ? '1px solid var(--color-danger)' : '1px solid var(--color-border)',
                  backgroundColor: 'var(--color-bg-base)',
                  color: 'var(--color-text-base)',
                  letterSpacing: '8px',
                  textAlign: 'center',
                  fontSize: '1.5rem',
                  fontWeight: '700',
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
          )}

          {/* ============================================================ */}
          {/* GIAO DIỆN BƯỚC 3: THIẾT LẬP MẬT KHẨU MỚI                     */}
          {/* ============================================================ */}
          {step === 3 && (
            <>
              {/* Ô nhập mật khẩu mới */}
              <div>
                <label className="font-semibold text-sm flex items-center gap-1.5">
                  <Lock size={16} /> Mật khẩu mới
                </label>
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
                    autoFocus
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

              {/* Ô nhập xác nhận mật khẩu mới */}
              <div>
                <label className="font-semibold text-sm flex items-center gap-1.5">
                  <Lock size={16} /> Xác nhận mật khẩu mới
                </label>
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

          {/* ============================================================ */}
          {/* NÚT SUBMIT HÀNH ĐỘNG CHO TỪNG BƯỚC                          */}
          {/* ============================================================ */}
          <button
            type="submit"
            disabled={isLoading}
            className="btn btn-primary mt-2 flex items-center justify-center gap-2 cursor-pointer disabled:opacity-70 disabled:cursor-not-allowed"
            style={{ padding: '0.75rem', width: '100%' }}
          >
            {isLoading ? (
              <span className="flex items-center gap-2">
                <RefreshCw size={16} className="animate-spin" />
                {step === 1 && 'Đang gửi mã xác nhận...'}
                {step === 2 && 'Đang xác thực mã OTP...'}
                {step === 3 && 'Đang đặt lại mật khẩu...'}
              </span>
            ) : (
              <>
                {step === 1 && 'Gửi mã xác nhận'}
                {step === 2 && 'Xác thực mã OTP'}
                {step === 3 && 'Đặt lại mật khẩu'}
              </>
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
