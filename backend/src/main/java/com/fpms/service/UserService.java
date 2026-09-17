package com.fpms.service;

import com.fpms.dto.request.ChangePasswordRequest;
import com.fpms.dto.request.UpdateProfileRequest;
import com.fpms.dto.response.UserResponse;
import com.fpms.security.UserPrincipal;

public interface UserService {

    /**
     * Lấy thông tin hồ sơ cá nhân của người dùng hiện tại
     *
     * @param userPrincipal đối tượng xác thực từ JWT
     * @return UserResponse chứa thông tin hồ sơ
     */
    UserResponse getMyProfile(UserPrincipal userPrincipal);

    /**
     * Cập nhật Họ tên và Số điện thoại cho người dùng hiện tại
     *
     * @param userPrincipal đối tượng xác thực từ JWT
     * @param request thông tin cập nhật (fullName, phoneNumber)
     * @return UserResponse chứa thông tin mới sau khi cập nhật
     */
    UserResponse updateMyProfile(UserPrincipal userPrincipal, UpdateProfileRequest request);

    /**
     * Đổi mật khẩu tài khoản người dùng
     *
     * @param userPrincipal đối tượng xác thực từ JWT
     * @param request thông tin đổi mật khẩu (currentPassword, newPassword, confirmPassword)
     */
    void changePassword(UserPrincipal userPrincipal, ChangePasswordRequest request);
}
