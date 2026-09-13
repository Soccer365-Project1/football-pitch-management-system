package com.fpms.service;

import com.fpms.dto.request.LoginRequest;
import com.fpms.dto.request.RegisterRequest;
import com.fpms.dto.response.AuthResponse;
import com.fpms.dto.response.UserResponse;
import com.fpms.security.UserPrincipal;

public interface AuthService {

    /**
     * Đăng ký tài khoản người dùng mới với vai trò mặc định ROLE_CUSTOMER
     *
     * @param request thông tin đăng ký người dùng
     */
    void register(RegisterRequest request);

    /**
     * Đăng nhập hệ thống bằng Email/SĐT và Mật khẩu, cấp phát cặp JWT Token
     *
     * @param request thông tin đăng nhập (loginId, password)
     * @return AuthResponse chứa cặp token và thông tin user
     */
    AuthResponse login(LoginRequest request);

    /**
     * Lấy thông tin tài khoản người dùng hiện tại từ phiên đăng nhập
     *
     * @param userPrincipal thông tin Principal của người dùng đang đăng nhập
     * @return UserResponse chứa thông tin tài khoản (ẩn mật khẩu)
     */
    UserResponse getCurrentUser(UserPrincipal userPrincipal);
}
