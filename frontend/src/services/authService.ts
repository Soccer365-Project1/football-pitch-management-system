import api from './api';
import type { ApiResponse, RegisterRequest } from '../types/auth';

/**
 * Tầng Dịch vụ Xác thực (Auth Service)
 * Đóng gói tất cả các lời gọi API liên quan đến tài khoản
 */
export const authService = {
  /**
   * Gọi API Đăng ký tài khoản khách hàng mới
   * Method: POST
   * Endpoint: /api/v1/auth/register
   * 
   * @param data Dữ liệu đăng ký (Họ tên, SĐT, Email, Mật khẩu, Xác nhận MK)
   * @returns ApiResponse chứa thông điệp thành công từ Server
   */
  registerApi: async (data: RegisterRequest): Promise<ApiResponse<void>> => {
    const response = await api.post<ApiResponse<void>>('/auth/register', data);
    return response.data;
  },
};
