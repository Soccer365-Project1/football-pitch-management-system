import api from './api';
import type { ApiResponse, UserResponse } from '../types/auth';
import type { UpdateProfileRequest, ChangePasswordRequest } from '../types/user';

export const userService = {
  /**
   * Gọi API Lấy thông tin hồ sơ cá nhân
   * Endpoint: GET /api/v1/users/me
   */
  getMyProfileApi: async (): Promise<ApiResponse<UserResponse>> => {
    const response = await api.get<ApiResponse<UserResponse>>('/users/me');
    return response.data;
  },

  /**
   * Gọi API Cập nhật thông tin cá nhân (Họ và tên, Số điện thoại)
   * Endpoint: PUT /api/v1/users/me
   */
  updateMyProfileApi: async (data: UpdateProfileRequest): Promise<ApiResponse<UserResponse>> => {
    const response = await api.put<ApiResponse<UserResponse>>('/users/me', data);
    return response.data;
  },

  /**
   * Gọi API Đổi mật khẩu tài khoản
   * Endpoint: POST /api/v1/users/change-password
   */
  changePasswordApi: async (data: ChangePasswordRequest): Promise<ApiResponse<void>> => {
    const response = await api.post<ApiResponse<void>>('/users/change-password', data);
    return response.data;
  },
};
