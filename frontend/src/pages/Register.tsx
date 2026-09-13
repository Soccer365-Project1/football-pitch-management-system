import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
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

  // Kiểm tra hợp lệ phía Client
  const validateForm = (): boolean => {
    const newErrors: FormErrors = {};

    if (!formData.fullName.trim()) {
      newErrors.fullName = 'Họ và tên không được để trống';
    }

    if (!formData.phoneNumber.trim()) {
      newErrors.phoneNumber = 'Số điện thoại không được để trống';
    } else if (!/^0\d{9}$/.test(formData.phoneNumber.trim())) {
      newErrors.phoneNumber = 'Số điện thoại phải gồm đúng 10 chữ số và bắt đầu bằng số 0';
    }

    if (!formData.email.trim()) {
      newErrors.email = 'Email không được để trống';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email.trim())) {
      newErrors.email = 'Email không đúng định dạng';
    }

    if (!formData.password) {
      newErrors.password = 'Mật khẩu không được để trống';
    } else if (formData.password.length < 6) {
      newErrors.password = 'Mật khẩu phải có tối thiểu 6 ký tự';
    }

    if (!formData.confirmPassword) {
      newErrors.confirmPassword = 'Vui lòng xác nhận lại mật khẩu';
    } else if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Mật khẩu xác nhận không trùng khớp';
    }

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
            <input 
              type="password" 
              name="password"
              value={formData.password}
              onChange={handleChange}
              className="mt-2 w-full outline-none" 
              style={{ 
                padding: '0.75rem', 
                borderRadius: 'var(--radius-md)', 
                border: errors.password ? '1px solid var(--color-danger)' : '1px solid var(--color-border)', 
                backgroundColor: 'var(--color-bg-base)', 
                color: 'var(--color-text-base)' 
              }}
              placeholder="••••••••"
            />
            {errors.password && (
              <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{errors.password}</p>
            )}
          </div>

          {/* Hàng 4: Xác nhận mật khẩu */}
          <div>
            <label className="font-semibold text-sm">Xác nhận mật khẩu</label>
            <input 
              type="password" 
              name="confirmPassword"
              value={formData.confirmPassword}
              onChange={handleChange}
              className="mt-2 w-full outline-none" 
              style={{ 
                padding: '0.75rem', 
                borderRadius: 'var(--radius-md)', 
                border: errors.confirmPassword ? '1px solid var(--color-danger)' : '1px solid var(--color-border)', 
                backgroundColor: 'var(--color-bg-base)', 
                color: 'var(--color-text-base)' 
              }}
              placeholder="••••••••"
            />
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
