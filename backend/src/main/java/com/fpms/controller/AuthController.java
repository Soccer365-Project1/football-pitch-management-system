package com.fpms.controller;

import com.fpms.common.response.ApiResponse;
import com.fpms.dto.request.ForgotPasswordRequest;
import com.fpms.dto.request.GoogleLoginRequest;
import com.fpms.dto.request.LoginRequest;
import com.fpms.dto.request.LogoutRequest;
import com.fpms.dto.request.RegisterRequest;
import com.fpms.dto.request.ResetPasswordRequest;
import com.fpms.dto.request.VerifyOtpRequest;
import com.fpms.dto.response.AuthResponse;
import com.fpms.dto.response.UserResponse;
import com.fpms.security.UserPrincipal;
import com.fpms.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "1. Authentication", description = "Các API xác thực, đăng ký, đăng nhập, Google OAuth và khôi phục mật khẩu qua OTP")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Đăng ký tài khoản mới", description = "Tạo tài khoản khách hàng (ROLE_CUSTOMER) với mật khẩu được băm BCrypt")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(true)
                .code(HttpStatus.CREATED.value())
                .message("Đăng ký tài khoản thành công")
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Đăng nhập hệ thống", description = "Đăng nhập bằng Email hoặc Số điện thoại, cấp phát cặp JWT Access & Refresh Token")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", authResponse));
    }

    @Operation(summary = "Đăng nhập bằng Google OAuth", description = "Xác thực Google ID Token và đăng nhập hoặc tự động tạo tài khoản")
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse authResponse = authService.loginWithGoogle(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập bằng Google thành công", authResponse));
    }

    @Operation(summary = "Lấy hồ sơ cá nhân hiện tại", description = "Truy xuất thông tin người dùng đang đăng nhập dựa trên JWT Bearer Token", security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        UserResponse userResponse = authService.getCurrentUser(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin tài khoản thành công", userResponse));
    }

    @Operation(summary = "Yêu cầu gửi mã OTP quên mật khẩu", description = "Sinh mã OTP 6 số ngẫu nhiên, lưu CSDL (hạn 10 phút) và gửi email HTML qua Brevo SMTP Relay")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Mã xác thực OTP đã được gửi tới email của bạn", null));
    }

    @Operation(summary = "Xác thực sơ bộ mã OTP", description = "Kiểm tra tính hợp lệ và hạn dùng của mã OTP trước khi cho phép giao diện đổi mật khẩu")
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Mã OTP hợp lệ", null));
    }

    @Operation(summary = "Đặt lại mật khẩu mới", description = "Xác thực OTP lần 2, kiểm tra mật khẩu khớp và băm BCrypt cập nhật vào CSDL")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Đặt lại mật khẩu thành công. Bạn có thể đăng nhập bằng mật khẩu mới", null));
    }

    @Operation(summary = "Đăng xuất tài khoản", description = "Thu hồi JWT token hiện tại và lưu vào Redis blacklist", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }
}
