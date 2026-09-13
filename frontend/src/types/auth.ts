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

export type { ApiResponse };
