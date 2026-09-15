import api from './api';
import type { 
  ApiResponse, 
  RegisterRequest, 
  LoginRequest, 
  AuthResponse, 
  UserResponse,
  ForgotPasswordRequest,
  VerifyOtpRequest,
  ResetPasswordRequest 
} from '../types/auth';

/**
 * Tầng Dịch vụ Xác thực (Auth Service)
 * Đóng gói tất cả các lời gọi API liên quan đến tài khoản
 */
export const authService = {
  /**
   * Gọi API Đăng ký tài khoản khách hàng mới
   * Endpoint: POST /api/v1/auth/register
   */
  registerApi: async (data: RegisterRequest): Promise<ApiResponse<void>> => {
    const response = await api.post<ApiResponse<void>>('/auth/register', data);
    return response.data;
  },

  /**
   * Gọi API Đăng nhập hệ thống
   * Endpoint: POST /api/v1/auth/login
   */
  loginApi: async (data: LoginRequest): Promise<ApiResponse<AuthResponse>> => {
    const response = await api.post<ApiResponse<AuthResponse>>('/auth/login', data);
    return response.data;
  },

  /**
   * Gọi API Lấy thông tin tài khoản hiện tại
   * Endpoint: GET /api/v1/auth/me
   * Token tự động được gắn thông qua Axios Request Interceptor trong api.ts
   */
  getMeApi: async (): Promise<ApiResponse<UserResponse>> => {
    const response = await api.get<ApiResponse<UserResponse>>('/auth/me');
    return response.data;
  },

  /**
   * Gọi API Đăng nhập bằng Google ID Token
   * Endpoint: POST /api/v1/auth/google
   */
  loginWithGoogleApi: async (idToken: string): Promise<ApiResponse<AuthResponse>> => {
    const response = await api.post<ApiResponse<AuthResponse>>('/auth/google', { idToken });
    return response.data;
  },

  /**
   * Gọi API yêu cầu cấp mã OTP qua Email
   * Endpoint: POST /api/v1/auth/forgot-password
   */
  forgotPasswordApi: async (data: ForgotPasswordRequest): Promise<ApiResponse<void>> => {
    const response = await api.post<ApiResponse<void>>('/auth/forgot-password', data);
    return response.data;
  },

  /**
   * Gọi API xác thực mã OTP
   * Endpoint: POST /api/v1/auth/verify-otp
   */
  verifyOtpApi: async (data: VerifyOtpRequest): Promise<ApiResponse<void>> => {
    const response = await api.post<ApiResponse<void>>('/auth/verify-otp', data);
    return response.data;
  },

  /**
   * Gọi API đặt lại mật khẩu mới
   * Endpoint: POST /api/v1/auth/reset-password
   */
  resetPasswordApi: async (data: ResetPasswordRequest): Promise<ApiResponse<void>> => {
    const response = await api.post<ApiResponse<void>>('/auth/reset-password', data);
    return response.data;
  },

  /**
   * Xóa sạch token ở Client khi người dùng đăng xuất
   * Xóa ở cả localStorage (nếu có ghi nhớ) và sessionStorage (nếu không ghi nhớ)
   */
  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    sessionStorage.removeItem('accessToken');
    sessionStorage.removeItem('refreshToken');
  },
};

