import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import { authService } from '../services/authService';

interface FormErrors {
  fullName?: string;
  phoneNumber?: string;
  email?: string;
  password?: string;
  confirmPassword?: string;
  agreeTerms?: string;
}

const Register: React.FC = () => {
  const navigate = useNavigate();

  // 1. Quản lý trạng thái dữ liệu Form
  const [formData, setFormData] = useState({
    fullName: '',
    phoneNumber: '',
    email: '',
    password: '',
    confirmPassword: '',
    agreeTerms: false,
  });

  // 2. Trạng thái lỗi và thông báo
  const [errors, setErrors] = useState<FormErrors>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  // 3. Quản lý trạng thái ẩn / hiện mật khẩu (Icon con mắt)
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  // Xử lý khi người dùng nhập liệu
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));

    // Tự động xóa lỗi khi người dùng sửa trường đó
    if (errors[name as keyof FormErrors]) {
      setErrors((prev) => ({ ...prev, [name]: undefined }));
    }
    if (serverError) setServerError(null);
  };

  // 3. Biểu thức chính quy kiểm tra độ mạnh của mật khẩu theo chuẩn bảo mật:
  // - Độ dài từ 6 đến 64 ký tự: (?=.{6,64}$)
  // - Chứa ít nhất 1 chữ cái: (?=.*[A-Za-z])
  // - Chứa ít nhất 1 chữ số: (?=.*\d)
  // - Chứa ít nhất 1 ký tự đặc biệt: (?=.*[^A-Za-z0-9\s])
  // - Chặn triệt để 100% khoảng trắng ở mọi vị trí (đầu, giữa, cuối): \S+$
  const PASSWORD_REGEX = /^(?=.{6,64}$)(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S+$/;

  // Kiểm tra hợp lệ phía Client cho toàn bộ các trường của form đăng ký
  const validateForm = (): boolean => {
    const newErrors: FormErrors = {};

    // Kiểm tra trường Họ và tên (không được để trống sau khi cắt khoảng trắng thừa)
    if (!formData.fullName.trim()) {
      newErrors.fullName = 'Họ và tên không được để trống';
    }

    // Kiểm tra trường Số điện thoại (bắt buộc đúng định dạng 10 chữ số nhà mạng Việt Nam)
    if (!formData.phoneNumber.trim()) {
      newErrors.phoneNumber = 'Số điện thoại không được để trống';
    } else if (!/^(0[35789])[0-9]{8}$/.test(formData.phoneNumber.trim())) {
      newErrors.phoneNumber = 'Số điện thoại không hợp lệ (phải gồm 10 chữ số và bắt đầu bằng 03, 05, 07, 08 hoặc 09)';
    }

    // Kiểm tra định dạng Email hợp lệ theo RFC
    if (!formData.email.trim()) {
      newErrors.email = 'Email không được để trống';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())) {
      newErrors.email = 'Email không đúng định dạng';
    }

    // Kiểm tra tính hợp lệ và độ bảo mật của Mật khẩu (Fix TC_REG_20, TC_REG_21)
    if (!formData.password) {
      newErrors.password = 'Mật khẩu không được để trống';
    } else if (/\s/.test(formData.password)) {
      // Bắt ca biên chứa khoảng trắng ở bất kỳ vị trí nào (TC_REG_20, TC_REG_21)
      newErrors.password = 'Mật khẩu không được chứa khoảng trắng';
    } else if (formData.password.length < 6 || formData.password.length > 64) {
      // Giới hạn độ dài an toàn từ 6 đến 64 ký tự
      newErrors.password = 'Mật khẩu phải từ 6 đến 64 ký tự';
    } else if (!PASSWORD_REGEX.test(formData.password)) {
      // Yêu cầu bắt buộc phải có chữ cái, chữ số và ký tự đặc biệt
      newErrors.password = 'Mật khẩu phải bao gồm ít nhất 1 chữ cái, 1 chữ số và 1 ký tự đặc biệt';
    }

    // Kiểm tra trường Xác nhận mật khẩu (phải trùng khớp chính xác với mật khẩu)
    if (!formData.confirmPassword) {
      newErrors.confirmPassword = 'Vui lòng xác nhận lại mật khẩu';
    } else if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Mật khẩu xác nhận không trùng khớp';
    }

    // Kiểm tra người dùng đã tích chọn đồng ý điều khoản hay chưa
    if (!formData.agreeTerms) {
      newErrors.agreeTerms = 'Bạn cần đồng ý với Điều khoản & Chính sách bảo mật';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Xử lý khi submit Form
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setServerError(null);
    setSuccessMessage(null);

    if (!validateForm()) return;

    setIsLoading(true);
    try {
      await authService.registerApi({
        fullName: formData.fullName.trim(),
        phoneNumber: formData.phoneNumber.trim(),
        email: formData.email.trim(),
        password: formData.password,
        confirmPassword: formData.confirmPassword,
      });

      setSuccessMessage('Đăng ký tài khoản thành công! Đang chuyển hướng sang trang đăng nhập...');

      setTimeout(() => {
        navigate('/login');
      }, 1500);
    } catch (err: any) {
      const message =
        err.response?.data?.message ||
        'Đăng ký thất bại. Vui lòng kiểm tra lại thông tin hoặc kết nối máy chủ!';
      setServerError(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex justify-center items-start pt-4">
      <div className="card" style={{ width: '100%', maxWidth: '500px' }}>
        <h2 className="text-2xl font-bold text-center mb-6">Đăng ký tài khoản</h2>

        {/* Thông báo lỗi từ Server */}
        {serverError && (
          <div 
            className="mb-4 p-3 rounded text-sm text-center"
            style={{ 
              backgroundColor: 'rgba(239, 68, 68, 0.1)', 
              color: 'var(--color-danger)',
              border: '1px solid var(--color-danger)'
            }}
          >
            {serverError}
          </div>
        )}

        {/* Thông báo đăng ký thành công */}
        {successMessage && (
          <div 
            className="mb-4 p-3 rounded text-sm text-center"
            style={{ 
              backgroundColor: 'rgba(16, 185, 129, 0.1)', 
              color: 'var(--color-primary)',
              border: '1px solid var(--color-primary)'
            }}
          >
            {successMessage}
          </div>
        )}
        
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {/* Hàng 1: Họ và tên & Số điện thoại (2 cột) */}
          <div className="grid gap-4" style={{ gridTemplateColumns: '1fr 1fr' }}>
            <div>
              <label className="font-semibold text-sm">Họ và tên</label>
              <input 
                type="text" 
                name="fullName"
                value={formData.fullName}
                onChange={handleChange}
                className="mt-2 w-full outline-none" 
                style={{ 
                  padding: '0.75rem', 
                  borderRadius: 'var(--radius-md)', 
                  border: errors.fullName ? '1px solid var(--color-danger)' : '1px solid var(--color-border)', 
                  backgroundColor: 'var(--color-bg-base)', 
                  color: 'var(--color-text-base)' 
                }}
                placeholder="VD: Nguyễn Văn A"
              />
              {errors.fullName && (
                <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{errors.fullName}</p>
              )}
            </div>

            <div>
              <label className="font-semibold text-sm">Số điện thoại</label>
              <input 
                type="tel" 
                name="phoneNumber"
                value={formData.phoneNumber}
                onChange={handleChange}
                maxLength={10}
                className="mt-2 w-full outline-none" 
                style={{ 
                  padding: '0.75rem', 
                  borderRadius: 'var(--radius-md)', 
                  border: errors.phoneNumber ? '1px solid var(--color-danger)' : '1px solid var(--color-border)', 
                  backgroundColor: 'var(--color-bg-base)', 
                  color: 'var(--color-text-base)' 
                }}
                placeholder="VD: 0912345678"
              />
              {errors.phoneNumber && (
                <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{errors.phoneNumber}</p>
              )}
            </div>
          </div>

          {/* Hàng 2: Email */}
          <div>
            <label className="font-semibold text-sm">Email</label>
            <input 
              type="email" 
              name="email"
              value={formData.email}
              onChange={handleChange}
              className="mt-2 w-full outline-none" 
              style={{ 
                padding: '0.75rem', 
                borderRadius: 'var(--radius-md)', 
                border: errors.email ? '1px solid var(--color-danger)' : '1px solid var(--color-border)', 
                backgroundColor: 'var(--color-bg-base)', 
                color: 'var(--color-text-base)' 
              }}
              placeholder="Nhập email..."
            />
            {errors.email && (
              <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{errors.email}</p>
            )}
          </div>
          
          {/* Hàng 3: Mật khẩu */}
          <div>
            <label className="font-semibold text-sm">Mật khẩu</label>
            <div className="relative mt-2">
              <input 
                type={showPassword ? 'text' : 'password'} 
                name="password"
                value={formData.password}
                onChange={handleChange}
                className="w-full outline-none pr-10" 
                style={{ 
                  padding: '0.75rem', 
                  paddingRight: '2.5rem',
                  borderRadius: 'var(--radius-md)', 
                  border: errors.password ? '1px solid var(--color-danger)' : '1px solid var(--color-border)', 
                  backgroundColor: 'var(--color-bg-base)', 
                  color: 'var(--color-text-base)' 
                }}
                placeholder="••••••••"
              />
              {/* Nút bấm chuyển đổi ẩn/hiện mật khẩu */}
              <button
                type="button"
                onClick={() => setShowPassword((prev) => !prev)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted hover:text-base cursor-pointer bg-transparent border-none p-0 flex items-center justify-center"
                tabIndex={-1}
                aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
            {errors.password ? (
              <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{errors.password}</p>
            ) : (
              <p className="mt-1 text-xs text-muted">Từ 6-64 ký tự, gồm chữ cái, chữ số, ký tự đặc biệt và không dấu cách.</p>
            )}
          </div>

          {/* Hàng 4: Xác nhận mật khẩu */}
          <div>
            <label className="font-semibold text-sm">Xác nhận mật khẩu</label>
            <div className="relative mt-2">
              <input 
                type={showConfirmPassword ? 'text' : 'password'} 
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                className="w-full outline-none pr-10" 
                style={{ 
                  padding: '0.75rem', 
                  paddingRight: '2.5rem',
                  borderRadius: 'var(--radius-md)', 
                  border: errors.confirmPassword ? '1px solid var(--color-danger)' : '1px solid var(--color-border)', 
                  backgroundColor: 'var(--color-bg-base)', 
                  color: 'var(--color-text-base)' 
                }}
                placeholder="••••••••"
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
              <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{errors.confirmPassword}</p>
            )}
          </div>
          
          {/* Hàng 5: Checkbox điều khoản */}
          <div className="flex flex-col text-sm mt-2">
            <label className="flex items-center gap-2 text-muted cursor-pointer select-none">
              <input 
                type="checkbox" 
                name="agreeTerms"
                checked={formData.agreeTerms}
                onChange={handleChange}
                className="cursor-pointer"
              /> 
              <span>Tôi đồng ý với các Điều khoản & Chính sách bảo mật của hệ thống.</span>
            </label>
            {errors.agreeTerms && (
              <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{errors.agreeTerms}</p>
            )}
          </div>
          
          {/* Nút Đăng ký ngay */}
          <button 
            type="submit" 
            disabled={isLoading}
            className="btn btn-primary mt-4 w-full flex items-center justify-center gap-2 cursor-pointer disabled:opacity-70 disabled:cursor-not-allowed" 
            style={{ padding: '0.75rem' }}
          >
            {isLoading ? 'Đang xử lý...' : 'Đăng ký ngay'}
          </button>
          
          {/* Chuyển sang Đăng nhập */}
          <div className="text-center text-sm mt-4">
            Đã có tài khoản?{' '}
            <Link to="/login" className="font-semibold hover:underline" style={{ color: 'var(--color-primary)' }}>
              Đăng nhập
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Register;
