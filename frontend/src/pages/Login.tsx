import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

// Biến module-level để trỏ callback tới instance hiện tại của component Login
let activeGoogleCallback: ((response: { credential: string }) => void) | null = null;

const Login: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, loginWithGoogle } = useAuth();

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

  // Khởi tạo dịch vụ Google Identity Services
  useEffect(() => {
    const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;
    if (!googleClientId || googleClientId.includes('YOUR_GOOGLE_CLIENT_ID')) return;

    // Cập nhật callback cho instance hiện tại của component
    activeGoogleCallback = async (response: { credential: string }) => {
      if (!response.credential) {
        setServerError('Không nhận được mã xác thực từ Google');
        return;
      }

      setIsLoading(true);
      try {
        const user = await loginWithGoogle(response.credential);

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
        const message = err.response?.data?.message || 'Đăng nhập bằng Google thất bại. Vui lòng thử lại!';
        setServerError(message);
      } finally {
        setIsLoading(false);
      }
    };

    const renderGoogleSignIn = () => {
      if (!window.google?.accounts?.id) return;

      // 1. Chỉ gọi initialize duy nhất 1 lần trong toàn bộ window session
      const win = window as any;
      if (!win.__fpms_google_initialized__) {
        window.google.accounts.id.initialize({
          client_id: googleClientId,
          auto_select: false,
          callback: (response: { credential: string }) => {
            activeGoogleCallback?.(response);
          },
        });
        win.__fpms_google_initialized__ = true;
      }

      // 2. Render nút Đăng nhập chuẩn của Google vào container của trang Login
      const googleBtnContainer = document.getElementById('googleSignInBtnContainer');
      if (googleBtnContainer) {
        googleBtnContainer.innerHTML = '';
        window.google.accounts.id.renderButton(googleBtnContainer, {
          type: 'standard',
          theme: 'outline',
          size: 'large',
          width: 350,
          text: 'signin_with',
          shape: 'rectangular',
          logo_alignment: 'left',
        });
      }
    };

    // Nạp động Google GSI SDK khi người dùng vào trang đăng nhập
    if (window.google?.accounts?.id) {
      renderGoogleSignIn();
    } else {
      let script = document.getElementById('google-gsi-script') as HTMLScriptElement;
      if (!script) {
        script = document.createElement('script');
        script.id = 'google-gsi-script';
        script.src = 'https://accounts.google.com/gsi/client';
        script.async = true;
        script.defer = true;
        script.onload = () => {
          renderGoogleSignIn();
        };
        document.head.appendChild(script);
      } else {
        script.addEventListener('load', () => renderGoogleSignIn());
      }
    }
  }, [loginWithGoogle, navigate, location.state]);

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
          
          {/* Nút Đăng nhập bằng Google chuẩn từ Google SDK */}
          <div className="flex justify-center items-center w-full my-1" style={{ minHeight: '44px' }}>
            <div id="googleSignInBtnContainer" style={{ width: '100%', display: 'flex', justifyContent: 'center' }}></div>
          </div>
          
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
