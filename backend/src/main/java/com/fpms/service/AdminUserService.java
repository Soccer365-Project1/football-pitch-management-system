package com.fpms.service;

import com.fpms.common.response.PageResponse;
import com.fpms.dto.request.UpdateUserRoleRequest;
import com.fpms.dto.request.UpdateUserStatusRequest;
import com.fpms.dto.response.UserResponse;
import com.fpms.entity.enums.RoleName;
import com.fpms.entity.enums.UserStatus;
import com.fpms.security.UserPrincipal;

public interface AdminUserService {

    /**
     * Lấy danh sách người dùng có hỗ trợ tìm kiếm, lọc và phân trang
     *
     * @param keyword từ khóa tìm kiếm (họ tên, email, sđt)
     * @param role vai trò người dùng (ROLE_CUSTOMER, ROLE_STAFF, ROLE_ADMIN)
     * @param status trạng thái tài khoản (ACTIVE, LOCKED)
     * @param page số trang (1-indexed)
     * @param size số bản ghi mỗi trang
     * @return danh sách người dùng phân trang
     */
    PageResponse<UserResponse> getUsers(String keyword, RoleName role, UserStatus status, int page, int size);

    /**
     * Lấy thông tin chi tiết một tài khoản người dùng theo ID
     *
     * @param id ID người dùng
     * @return thông tin chi tiết người dùng
     */
    UserResponse getUserById(Long id);

    /**
     * Khóa hoặc mở khóa tài khoản người dùng (ACTIVE <-> LOCKED)
     *
     * @param currentAdmin thông tin tài khoản admin đang đăng nhập
     * @param id ID người dùng cần cập nhật
     * @param request dữ liệu trạng thái mới
     * @return thông tin người dùng sau khi cập nhật
     */
    UserResponse updateUserStatus(UserPrincipal currentAdmin, Long id, UpdateUserStatusRequest request);

    /**
     * Cấp quyền hoặc thu hồi quyền Staff (ROLE_CUSTOMER <-> ROLE_STAFF)
     *
     * @param currentAdmin thông tin tài khoản admin đang đăng nhập
     * @param id ID người dùng cần cập nhật
     * @param request dữ liệu vai trò mới
     * @return thông tin người dùng sau khi cập nhật
     */
    UserResponse updateUserRole(UserPrincipal currentAdmin, Long id, UpdateUserRoleRequest request);
}
