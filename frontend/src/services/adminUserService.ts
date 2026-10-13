import api from './api';
import type { ApiResponse, PageResponse } from '../types/common';
import type {
  AdminUserItem,
  AdminUserFilterParams,
  UpdateUserRequest,
  UpdateUserStatusRequest,
  UpdateUserRoleRequest,
  CreateStaffRequest,
} from '../types/adminUser';

export const adminUserService = {
  /**
   * Gọi API Lấy danh sách người dùng có hỗ trợ tìm kiếm, lọc và phân trang
   * Endpoint: GET /api/v1/admin/users
   */
  getUsers: async (params: AdminUserFilterParams): Promise<PageResponse<AdminUserItem>> => {
    const queryParams: Record<string, any> = {
      page: params.page,
      size: params.size,
    };

    if (params.keyword && params.keyword.trim() !== '') {
      queryParams.keyword = params.keyword.trim();
    }
    if (params.role && params.role !== 'ALL') {
      queryParams.role = params.role;
    }
    if (params.status && params.status !== 'ALL') {
      queryParams.status = params.status;
    }

    const response = await api.get<ApiResponse<PageResponse<AdminUserItem>>>('/admin/users', {
      params: queryParams,
    });
    return response.data.data!;
  },

  /**
   * Gọi API Lấy chi tiết hồ sơ người dùng theo ID
   * Endpoint: GET /api/v1/admin/users/{id}
   */
  getUserById: async (id: number): Promise<AdminUserItem> => {
    const response = await api.get<ApiResponse<AdminUserItem>>(`/admin/users/${id}`);
    return response.data.data!;
  },

  /**
   * Gọi API Cập nhật thông tin người dùng (Họ và tên, Số điện thoại)
   * Endpoint: PUT /api/v1/admin/users/{id}
   */
  updateUser: async (id: number, data: UpdateUserRequest): Promise<AdminUserItem> => {
    const response = await api.put<ApiResponse<AdminUserItem>>(`/admin/users/${id}`, data);
    return response.data.data!;
  },

  /**
   * Gọi API Khóa hoặc Mở khóa tài khoản người dùng
   * Endpoint: PATCH /api/v1/admin/users/{id}/status
   */
  updateUserStatus: async (id: number, status: 'ACTIVE' | 'LOCKED'): Promise<AdminUserItem> => {
    const payload: UpdateUserStatusRequest = { status };
    const response = await api.patch<ApiResponse<AdminUserItem>>(`/admin/users/${id}/status`, payload);
    return response.data.data!;
  },

  /**
   * Gọi API Cập nhật vai trò người dùng (Cấp / Thu hồi quyền Staff)
   * Endpoint: PATCH /api/v1/admin/users/{id}/role
   */
  updateUserRole: async (id: number, role: 'ROLE_CUSTOMER' | 'ROLE_STAFF'): Promise<AdminUserItem> => {
    const payload: UpdateUserRoleRequest = { role };
    const response = await api.patch<ApiResponse<AdminUserItem>>(`/admin/users/${id}/role`, payload);
    return response.data.data!;
  },

  /**
   * Gọi API Tạo tài khoản nhân viên mới từ Quản trị viên
   * Endpoint: POST /api/v1/admin/users/staff
   */
  createStaff: async (data: CreateStaffRequest): Promise<AdminUserItem> => {
    const response = await api.post<ApiResponse<AdminUserItem>>('/admin/users/staff', data);
    return response.data.data!;
  },
};
