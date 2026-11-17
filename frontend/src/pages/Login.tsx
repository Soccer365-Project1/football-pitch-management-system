import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { useDocumentTitle } from '../hooks/useDocumentTitle';
import { showToast } from '../utils/toast';

// Biến module-level để trỏ callback tới instance hiện tại của component Login
let activeGoogleCallback: ((response: { credential: string }) => void) | null = null;

const Login: React.FC = () => {
  useDocumentTitle('Đăng nhập');
  const navigate = useNavigate();
  const location = useLocation();
  const { login, loginWithGoogle } = useAuth();

  // 1. Quản lý trạng thái Form
  const [formData, setFormData] = useState({
    loginId: '',
    password: '',
    rememberMe: false,
  });

  // Trạng thái hiển thị mật khẩu (Ẩn / Hiện với icon con mắt)
  const [showPassword, setShowPassword] = useState(false);

  // Trạng thái tải Google Sign-In động (Tránh cookie bên thứ 3 và lỗi contrast khi audit)
  const [googleLoading, setGoogleLoading] = useState(false);
  const [showGoogleIframe, setShowGoogleIframe] = useState(false);

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

      showToast('Đăng nhập thành công!', 'success');

      // Điều hướng thông minh sau khi đăng nhập thành công
      const fromObj = (location.state as any)?.from;
      if (fromObj) {
        navigate(fromObj.pathname + (fromObj.search || ''), { replace: true });
      } else if (user.role === 'ROLE_ADMIN') {
        navigate('/admin', { replace: true });
      } else if (user.role === 'ROLE_STAFF') {
        navigate('/admin/timeline', { replace: true });
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

  // Đăng ký callback xử lý kết quả đăng nhập Google
  useEffect(() => {
    activeGoogleCallback = async (response: { credential: string }) => {
      if (!response.credential) {
        setServerError('Không nhận được mã xác thực từ Google');
        return;
      }

      setIsLoading(true);
      try {
        const user = await loginWithGoogle(response.credential);

        showToast('Đăng nhập thành công!', 'success');

        // Điều hướng thông minh sau khi đăng nhập thành công
        const fromObj = (location.state as any)?.from;
        if (fromObj) {
          navigate(fromObj.pathname + (fromObj.search || ''), { replace: true });
        } else if (user.role === 'ROLE_ADMIN') {
          navigate('/admin', { replace: true });
        } else if (user.role === 'ROLE_STAFF') {
          navigate('/admin/timeline', { replace: true });
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
  }, [loginWithGoogle, navigate, location.state]);

  // Preload script Google GSI khi di chuột vào nút (không chạy khi trang mới load để đạt Best Practices 100)
  const preloadGoogleScript = () => {
    if (window.google?.accounts?.id || document.getElementById('google-gsi-script')) return;
    const script = document.createElement('script');
    script.id = 'google-gsi-script';
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    document.head.appendChild(script);
  };

  // Kích hoạt luồng xác thực Google khi người dùng bấm nút
  const handleGoogleSignInClick = () => {
    const googleClientId =
      import.meta.env.VITE_GOOGLE_CLIENT_ID ||
      '1032558823852-is8rsjbfg1ckpntdajo7emphtpnstha0.apps.googleusercontent.com';

    if (!googleClientId || googleClientId.includes('YOUR_GOOGLE_CLIENT_ID')) return;

    setGoogleLoading(true);

    const executeGoogleAuth = () => {
      if (!window.google?.accounts?.id) {
        setGoogleLoading(false);
        showToast('Đang kết nối tới dịch vụ Google, vui lòng thử lại sau giây lát...', 'info');
        return;
      }

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

      window.google.accounts.id.prompt((notification: any) => {
        setGoogleLoading(false);
        if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
          setShowGoogleIframe(true);
          setTimeout(() => {
            const googleBtnContainer = document.getElementById('googleSignInBtnContainer');
            if (googleBtnContainer && window.google?.accounts?.id) {
              googleBtnContainer.innerHTML = '';
              window.google.accounts.id.renderButton(googleBtnContainer, {
                type: 'standard',
                theme: 'outline',
                size: 'large',
                width: 380,
                text: 'signin_with',
                shape: 'rectangular',
                logo_alignment: 'left',
              });
            }
          }, 50);
        }
      });
    };

    if (window.google?.accounts?.id) {
      executeGoogleAuth();
    } else {
      let script = document.getElementById('google-gsi-script') as HTMLScriptElement;
      if (!script) {
        script = document.createElement('script');
        script.id = 'google-gsi-script';
        script.src = 'https://accounts.google.com/gsi/client';
        script.async = true;
        script.defer = true;
        script.onload = () => executeGoogleAuth();
        script.onerror = () => {
          setGoogleLoading(false);
          showToast('Không thể tải dịch vụ Google. Vui lòng thử lại.', 'error');
        };
        document.head.appendChild(script);
      } else {
        script.addEventListener('load', () => executeGoogleAuth());
      }
    }
  };

  return (
    <div className="flex justify-center items-start pt-4">
      <div className="card" style={{ width: '100%', maxWidth: '400px' }}>
        <h1 className="text-2xl font-bold text-center mb-6">Đăng nhập Soccer365</h1>

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
            <label htmlFor="loginId" className="font-semibold text-sm">Email hoặc Số điện thoại</label>
            <input 
              id="loginId"
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
            <label htmlFor="loginPassword" className="font-semibold text-sm">Mật khẩu</label>
            <div className="relative mt-2">
              <input 
                id="loginPassword"
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
              {/* Nút bấm chuyển đổi ẩn/hiện mật khẩu (icon con mắt) */}
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
            {errors.password && (
              <p className="mt-1 text-xs" style={{ color: 'var(--color-danger)' }}>{errors.password}</p>
            )}
          </div>
          
          {/* Ghi nhớ & Quên mật khẩu */}
          <div className="flex justify-between items-center text-sm">
            <label htmlFor="rememberMe" className="flex items-center gap-2 cursor-pointer select-none">
              <input 
                id="rememberMe"
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
          
          {/* Nút Đăng nhập bằng Google: Tối ưu 100% Accessibility & Best Practices */}
          <div className="w-full flex justify-center items-center my-1" style={{ minHeight: '44px' }}>
            {!showGoogleIframe ? (
              <button
                type="button"
                onClick={handleGoogleSignInClick}
                onMouseEnter={preloadGoogleScript}
                onFocus={preloadGoogleScript}
                disabled={googleLoading || isLoading}
                className="w-full flex items-center justify-center gap-3 py-2.5 px-4 border rounded-lg font-semibold text-sm transition-all shadow-sm cursor-pointer hover:bg-gray-50 disabled:opacity-70"
                style={{
                  backgroundColor: 'var(--color-bg-surface, #ffffff)',
                  borderColor: 'var(--color-border, #e5e7eb)',
                  color: 'var(--color-text-base, #1f2937)',
                  height: '44px',
                }}
                aria-label="Đăng nhập bằng tài khoản Google"
              >
                {/* Google SVG Logo đa sắc */}
                <svg className="w-5 h-5 flex-shrink-0" viewBox="0 0 24 24">
                  <path fill="#4285F4" d="M23.745 12.27c0-.7-.06-1.4-.19-2.07H12v4.51h6.6c-.29 1.52-1.14 2.82-2.4 3.68v3.05h3.88c2.27-2.09 3.665-5.17 3.665-9.17z"/>
                  <path fill="#34A853" d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.88-3.05c-1.08.72-2.45 1.16-4.05 1.16-3.12 0-5.77-2.1-6.72-4.93H1.25v3.15C3.26 21.36 7.36 24 12 24z"/>
                  <path fill="#FBBC05" d="M5.28 14.27a7.22 7.22 0 0 1 0-4.54V6.58H1.25a11.98 11.98 0 0 0 0 10.84l4.03-3.15z"/>
                  <path fill="#EA4335" d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.36 0 3.26 2.64 1.25 6.58l4.03 3.15c.95-2.83 3.6-4.98 6.72-4.98z"/>
                </svg>
                <span>{googleLoading ? 'Đang kết nối Google...' : 'Đăng nhập bằng Google'}</span>
              </button>
            ) : (
              <div
                id="googleSignInBtnContainer"
                className="w-full flex justify-center items-center"
                style={{ minHeight: '44px' }}
              ></div>
            )}
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
