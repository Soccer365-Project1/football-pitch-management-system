package com.fpms.service;

import com.fpms.dto.request.RegisterRequest;

public interface AuthService {

    /**
     * Đăng ký tài khoản người dùng mới với vai trò mặc định ROLE_CUSTOMER
     *
     * @param request thông tin đăng ký người dùng
     */
    void register(RegisterRequest request);
}
