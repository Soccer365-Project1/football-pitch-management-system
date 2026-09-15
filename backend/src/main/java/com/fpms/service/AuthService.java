package com.fpms.service;

import com.fpms.dto.request.*;
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
     * Đăng nhập hệ thống bằng Google ID Token, cấp phát cặp JWT Token
     *
     * @param request thông tin GoogleLoginRequest chứa idToken
     * @return AuthResponse chứa cặp token và thông tin user
     */
    AuthResponse loginWithGoogle(GoogleLoginRequest request);

    /**
     * Lấy thông tin tài khoản người dùng hiện tại từ phiên đăng nhập
     *
     * @param userPrincipal thông tin Principal của người dùng đang đăng nhập
     * @return UserResponse chứa thông tin tài khoản (ẩn mật khẩu)
     */
    UserResponse getCurrentUser(UserPrincipal userPrincipal);

    /**
     * Tiếp nhận yêu cầu quên mật khẩu, sinh mã OTP 6 số và gửi qua email
     *
     * @param request thông tin email người dùng
     */
    void forgotPassword(ForgotPasswordRequest request);

    /**
     * Xác thực tính hợp lệ của mã OTP trước khi cho phép thiết lập mật khẩu mới
     *
     * @param request thông tin email và mã OTP
     */
    void verifyOtp(VerifyOtpRequest request);

    /**
     * Đặt lại mật khẩu mới sau khi xác thực OTP thành công
     *
     * @param request thông tin email, mã OTP, mật khẩu mới và xác nhận mật khẩu
     */
    void resetPassword(ResetPasswordRequest request);
}
