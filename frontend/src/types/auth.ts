import type { ApiResponse } from './common';

// ==========================================
// 1. TÍNH NĂNG ĐĂNG KÝ 
// ==========================================
export interface RegisterRequest {
  fullName: string;
  phoneNumber: string;
  email: string;
  password: string;
  confirmPassword: string;
}

// ==========================================
// 2. TÍNH NĂNG ĐĂNG NHẬP
// ==========================================

/**
 * Dữ liệu người dùng gửi lên khi Đăng nhập
 * Tương ứng với LoginRequest.java
 * loginId: Chấp nhận cả Email hoặc Số điện thoại
 */
export interface LoginRequest {
  loginId: string;
  password: string;
}

/**
 * Thông tin chi tiết của người dùng trả về từ Server
 * Tương ứng với UserResponse.java
 */
export interface UserResponse {
  id: number;
  fullName: string;
  email: string;
  phoneNumber?: string | null;
  avatarUrl?: string;
  role: 'ROLE_CUSTOMER' | 'ROLE_STAFF' | 'ROLE_ADMIN';
  status: 'ACTIVE' | 'LOCKED';
  createdAt?: string;
}

/**
 * Dữ liệu trả về khi Đăng nhập thành công
 * Tương ứng với AuthResponse.java
 */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  user: UserResponse;
}

// ==========================================
// 3. TÍNH NĂNG QUÊN & ĐẶT LẠI MẬT KHẨU (ST-02)
// ==========================================

/**
 * Dữ liệu gửi lên khi yêu cầu cấp OTP quên mật khẩu
 * Tương ứng với ForgotPasswordRequest.java
 */
export interface ForgotPasswordRequest {
  email: string;
}

/**
 * Dữ liệu gửi lên khi xác thực mã OTP
 * Tương ứng với VerifyOtpRequest.java
 */
export interface VerifyOtpRequest {
  email: string;
  otpCode: string;
}

/**
 * Dữ liệu gửi lên khi đặt lại mật khẩu mới
 * Tương ứng với ResetPasswordRequest.java
 */
export interface ResetPasswordRequest {
  email: string;
  otpCode: string;
  newPassword: string;
  confirmPassword: string;
}

/**
 * Dữ liệu gửi lên khi Đăng xuất để thu hồi token vào Redis blacklist
 * Tương ứng với LogoutRequest.java
 */
export interface LogoutRequest {
  token: string;
}

export type { ApiResponse };

