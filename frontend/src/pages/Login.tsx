import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const Login: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  // 1. Quản lý trạng thái Form
  const [formData, setFormData] = useState({
    loginId: '',
    password: '',
    rememberMe: false,
  });

  // 2. Trạng thái lỗi và Loading
  const [errors, setErrors] = useState<{ loginId?: string; password?: string }>({});
  const [serverError, setServerError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  // Xử lý thay đổi input
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, type, checked } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));

    if (errors[name as 'loginId' | 'password']) {
      setErrors((prev) => ({ ...prev, [name]: undefined }));
    }
    if (serverError) setServerError(null);
  };

  // Kiểm tra tính hợp lệ phía Client
  const validateForm = (): boolean => {
    const newErrors: { loginId?: string; password?: string } = {};

    if (!formData.loginId.trim()) {
      newErrors.loginId = 'Vui lòng nhập Email hoặc Số điện thoại';
    }

    if (!formData.password) {
      newErrors.password = 'Vui lòng nhập mật khẩu';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Xử lý khi Submit Form Đăng nhập
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setServerError(null);

    if (!validateForm()) return;

    setIsLoading(true);
    try {
      const user = await login({
        loginId: formData.loginId.trim(),
        password: formData.password,
      }, formData.rememberMe);

      // Điều hướng thông minh sau khi đăng nhập thành công
      const fromPath = (location.state as any)?.from?.pathname;
      if (fromPath) {
        navigate(fromPath, { replace: true });
      } else if (user.role === 'ROLE_ADMIN') {
        navigate('/admin/dashboard', { replace: true });
      } else if (user.role === 'ROLE_STAFF') {
        navigate('/staff/orders', { replace: true });
      } else {
        navigate('/', { replace: true });
      }
    } catch (err: any) {
      const message =
        err.response?.data?.message ||
        'Đăng nhập thất bại. Vui lòng kiểm tra lại tài khoản hoặc mật khẩu!';
      setServerError(message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleGoogleLogin = () => {
    alert('Tính năng Đăng nhập với Google đang được phát triển và sẽ sớm ra mắt!');
  };

  return (
    <div className="flex justify-center items-start pt-4">
      <div className="card" style={{ width: '100%', maxWidth: '400px' }}>
        <h2 className="text-2xl font-bold text-center mb-6">Đăng nhập Soccer365</h2>

        {/* Thông báo lỗi từ Backend */}
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
        
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          {/* Ô Email hoặc Số điện thoại */}
          <div>
            <label className="font-semibold text-sm">Email hoặc Số điện thoại</label>
            <input 
              type="text" 
              name="loginId"
              value={formData.loginId}
              onChange={handleChange}
              className="mt-2 w-full outline-none" 
              style={{ 
                padding: '0.75rem', 
                borderRadius: 'var(--radius-md)', 
                border: errors.loginId ? '1px solid var(--color-danger)' : '1px solid var(--color-border)', 
                backgroundColor: 'var(--color-bg-base)', 
                color: 'var(--color-text-base)' 
              }}
              placeholder="Nhập email hoặc số điện thoại..."
            />
            {errors.loginId && (
              <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{errors.loginId}</p>
            )}
          </div>
          
          {/* Ô Mật khẩu */}
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
          
          {/* Ghi nhớ & Quên mật khẩu */}
          <div className="flex justify-between items-center text-sm">
            <label className="flex items-center gap-2 cursor-pointer select-none">
              <input 
                type="checkbox" 
                name="rememberMe"
                checked={formData.rememberMe}
                onChange={handleChange}
                className="cursor-pointer"
              /> Ghi nhớ
            </label>
            <Link to="/forgot-password" style={{ color: 'var(--color-primary)' }}>Quên mật khẩu?</Link>
          </div>
          
          {/* Nút Đăng nhập */}
          <button 
            type="submit" 
            disabled={isLoading}
            className="btn btn-primary mt-2 flex items-center justify-center gap-2 cursor-pointer disabled:opacity-70 disabled:cursor-not-allowed" 
            style={{ padding: '0.75rem', width: '100%' }}
          >
            {isLoading ? 'Đang đăng nhập...' : 'Đăng nhập'}
          </button>
          
          {/* Đường ngăn cách Hoặc */}
          <div className="flex items-center justify-center gap-4 my-2">
            <div style={{ flex: 1, height: '1px', backgroundColor: 'var(--color-border)' }}></div>
            <span className="text-muted text-sm font-medium">Hoặc</span>
            <div style={{ flex: 1, height: '1px', backgroundColor: 'var(--color-border)' }}></div>
          </div>
          
          {/* Nút Đăng nhập bằng Google */}
          <button 
            type="button" 
            onClick={handleGoogleLogin}
            className="btn btn-secondary flex items-center justify-center gap-2 cursor-pointer" 
            style={{ padding: '0.75rem', width: '100%', backgroundColor: 'var(--color-bg-surface)' }}
          >
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="20" height="20">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
            </svg>
            Đăng nhập bằng Google
          </button>
          
          {/* Chuyển sang Đăng ký */}
          <div className="text-center text-sm mt-4">
            Chưa có tài khoản? <Link to="/register" className="font-semibold" style={{ color: 'var(--color-primary)' }}>Đăng ký ngay</Link>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Login;
