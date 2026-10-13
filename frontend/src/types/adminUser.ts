import type { PageResponse } from './common';

export type RoleName = 'ROLE_CUSTOMER' | 'ROLE_STAFF' | 'ROLE_ADMIN';
export type UserStatus = 'ACTIVE' | 'LOCKED';

/**
 * Thông tin bản ghi người dùng trong danh sách quản trị
 */
export interface AdminUserItem {
  id: number;
  fullName: string;
  email: string;
  phoneNumber?: string | null;
  avatarUrl?: string | null;
  role: RoleName;
  status: UserStatus;
  createdAt?: string;
}

/**
 * Các tham số truy vấn tìm kiếm, lọc và phân trang
 */
export interface AdminUserFilterParams {
  keyword?: string;
  role?: RoleName | 'ALL';
  status?: UserStatus | 'ALL';
  page: number;
  size: number;
}

/**
 * Yêu cầu thay đổi trạng thái người dùng (Khóa / Mở khóa)
 */
export interface UpdateUserStatusRequest {
  status: UserStatus;
}

/**
 * Yêu cầu cập nhật thông tin người dùng (Họ và tên, Số điện thoại)
 */
export interface UpdateUserRequest {
  fullName: string;
  phoneNumber: string;
}

/**
 * Yêu cầu thay đổi vai trò người dùng (Cấp / Thu hồi quyền Staff)
 */
export interface UpdateUserRoleRequest {
  role: RoleName;
}

/**
 * Yêu cầu tạo tài khoản nhân viên mới từ Quản trị viên
 */
export interface CreateStaffRequest {
  fullName: string;
  phoneNumber: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export type { PageResponse };
