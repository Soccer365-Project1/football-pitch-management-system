package com.fpms.service;

public interface EmailService {

    /**
     * Gửi email chứa mã xác thực OTP phục vụ việc đặt lại mật khẩu.
     *
     * @param toEmail địa chỉ email nhận thư
     * @param otpCode mã OTP gồm 6 chữ số
     * @param expirationMinutes thời gian hiệu lực của mã (tính bằng phút)
     */
    void sendOtpEmail(String toEmail, String otpCode, int expirationMinutes);
}
