package com.fpms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpms.dto.request.ForgotPasswordRequest;
import com.fpms.dto.request.LogoutRequest;
import com.fpms.dto.request.RegisterRequest;
import com.fpms.dto.request.ResetPasswordRequest;
import com.fpms.dto.request.VerifyOtpRequest;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.exception.GlobalExceptionHandler;
import com.fpms.service.AuthService;
import com.fpms.dto.response.UserResponse;
import com.fpms.entity.enums.RoleName;
import com.fpms.entity.enums.UserStatus;
import com.fpms.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("TC-05: POST /api/v1/auth/register - Thành công trả về 201 Created")
    void register_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Nguyễn Văn A")
                .phoneNumber("0912345678")
                .email("nguyenvana@gmail.com")
                .password("Password@123")
                .confirmPassword("Password@123")
                .build();

        doNothing().when(authService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Đăng ký tài khoản thành công"));

        verify(authService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("TC-01: POST /api/v1/auth/register - Thiếu trường bắt buộc trả về 400 Bad Request")
    void register_MissingFields_ReturnsBadRequest() throws Exception {
        RegisterRequest emptyRequest = new RegisterRequest();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors").isArray());

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("TC-02: POST /api/v1/auth/register - Số điện thoại không hợp lệ trả về 400")
    void register_InvalidPhoneNumber_ReturnsBadRequest() throws Exception {
        RegisterRequest invalidPhoneRequest = RegisterRequest.builder()
                .fullName("Nguyễn Văn A")
                .phoneNumber("12345") // không đúng định dạng 10 số bắt đầu bằng 0
                .email("nguyenvana@gmail.com")
                .password("Password@123")
                .confirmPassword("Password@123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPhoneRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("phoneNumber"));

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("TC-02: POST /api/v1/auth/register - Email sai định dạng trả về 400")
    void register_InvalidEmail_ReturnsBadRequest() throws Exception {
        RegisterRequest invalidEmailRequest = RegisterRequest.builder()
                .fullName("Nguyễn Văn A")
                .phoneNumber("0912345678")
                .email("invalid-email-format")
                .password("Password@123")
                .confirmPassword("Password@123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEmailRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("email"));

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("TC-04: POST /api/v1/auth/register - Email đã tồn tại trả về 400 và mã lỗi 2003")
    void register_EmailAlreadyExists_ReturnsBusinessError() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Nguyễn Văn A")
                .phoneNumber("0912345678")
                .email("nguyenvana@gmail.com")
                .password("Password@123")
                .confirmPassword("Password@123")
                .build();

        doThrow(new AppException(ErrorCode.EMAIL_ALREADY_EXISTS))
                .when(authService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(2003))
                .andExpect(jsonPath("$.message").value(ErrorCode.EMAIL_ALREADY_EXISTS.getMessage()));
    }

    @Test
    @DisplayName("TC-02b: POST /api/v1/auth/register - Mật khẩu chứa khoảng trắng trả về 400 Bad Request")
    void register_PasswordContainsWhitespace_ReturnsBadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Nguyễn Văn A")
                .phoneNumber("0912345678")
                .email("nguyenvana@gmail.com")
                .password("Pass@ 123")
                .confirmPassword("Pass@ 123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("password"));

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("TC-02c: POST /api/v1/auth/register - Mật khẩu thiếu ký tự đặc biệt trả về 400 Bad Request")
    void register_PasswordMissingSpecialChar_ReturnsBadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Nguyễn Văn A")
                .phoneNumber("0912345678")
                .email("nguyenvana@gmail.com")
                .password("Password123")
                .confirmPassword("Password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("password"));

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("TC-02d: POST /api/v1/auth/register - Mật khẩu dưới 6 ký tự trả về 400 Bad Request")
    void register_PasswordUnder6Chars_ReturnsBadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Nguyễn Văn A")
                .phoneNumber("0912345678")
                .email("nguyenvana@gmail.com")
                .password("P@1a")
                .confirmPassword("P@1a")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors[0].field").value("password"));

        verify(authService, never()).register(any());
    }

    // ================= Test Cases Cho POST /api/v1/auth/login =================

    @Test
    @DisplayName("TC-08: POST /api/v1/auth/login - Đăng nhập thành công trả về 200 OK và cặp token")
    void login_Success() throws Exception {
        com.fpms.dto.request.LoginRequest request = com.fpms.dto.request.LoginRequest.builder()
                .loginId("nguyenvana@gmail.com")
                .password("password123")
                .build();

        com.fpms.dto.response.AuthResponse authResponse = com.fpms.dto.response.AuthResponse.builder()
                .accessToken("mockAccessToken")
                .refreshToken("mockRefreshToken")
                .tokenType("Bearer")
                .user(com.fpms.dto.response.UserResponse.builder().id(1L).email("nguyenvana@gmail.com").build())
                .build();

        when(authService.login(any(com.fpms.dto.request.LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("mockAccessToken"))
                .andExpect(jsonPath("$.data.refreshToken").value("mockRefreshToken"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value("nguyenvana@gmail.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Bỏ trống loginId/password trả về 400 Bad Request")
    void login_MissingFields_ReturnsBadRequest() throws Exception {
        com.fpms.dto.request.LoginRequest request = new com.fpms.dto.request.LoginRequest();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("TC-06: POST /api/v1/auth/login - Sai mật khẩu trả về 401 Unauthorized")
    void login_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        com.fpms.dto.request.LoginRequest request = com.fpms.dto.request.LoginRequest.builder()
                .loginId("nguyenvana@gmail.com")
                .password("wrongPassword")
                .build();

        when(authService.login(any(com.fpms.dto.request.LoginRequest.class)))
                .thenThrow(new AppException(ErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(2004))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_CREDENTIALS.getMessage()));
    }

    @Test
    @DisplayName("TC-07: POST /api/v1/auth/login - Tài khoản bị khóa trả về 403 Forbidden")
    void login_UserLocked_ReturnsForbidden() throws Exception {
        com.fpms.dto.request.LoginRequest request = com.fpms.dto.request.LoginRequest.builder()
                .loginId("lockeduser@gmail.com")
                .password("password123")
                .build();

        when(authService.login(any(com.fpms.dto.request.LoginRequest.class)))
                .thenThrow(new AppException(ErrorCode.USER_LOCKED));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(2005))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_LOCKED.getMessage()));
    }

    // ================= Test Cases Cho GET /api/v1/auth/me =================

    @Test
    @DisplayName("GET /api/v1/auth/me - Thành công trả về 200 OK và thông tin người dùng")
    void getCurrentUser_Success() throws Exception {
        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("nguyenvana@gmail.com")
                .fullName("Nguyễn Văn A")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .build();

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("nguyenvana@gmail.com")
                .fullName("Nguyễn Văn A")
                .phoneNumber("0912345678")
                .role(RoleName.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());

        try {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            when(authService.getCurrentUser(any(UserPrincipal.class))).thenReturn(userResponse);

            mockMvc.perform(get("/api/v1/auth/me")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Lấy thông tin tài khoản thành công"))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.email").value("nguyenvana@gmail.com"))
                    .andExpect(jsonPath("$.data.fullName").value("Nguyễn Văn A"));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    // ================= Test Cases Cho Subtask ST-02 Endpoints =================

    // --- POST /api/v1/auth/forgot-password ---

    @Test
    @DisplayName("TC-01: POST /api/v1/auth/forgot-password - Yêu cầu gửi OTP thành công trả về 200 OK")
    void forgotPassword_Success() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("nguyenvana@gmail.com")
                .build();

        doNothing().when(authService).forgotPassword(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Mã xác thực OTP đã được gửi tới email của bạn"));

        verify(authService, times(1)).forgotPassword(any(ForgotPasswordRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password - Email sai định dạng trả về 400 Bad Request")
    void forgotPassword_InvalidEmail_ReturnsBadRequest() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("invalid-email-format")
                .build();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400));

        verify(authService, never()).forgotPassword(any());
    }

    @Test
    @DisplayName("POST /api/v1/auth/forgot-password - Bỏ trống email trả về 400 Bad Request")
    void forgotPassword_BlankEmail_ReturnsBadRequest() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400));

        verify(authService, never()).forgotPassword(any());
    }

    @Test
    @DisplayName("TC-02: POST /api/v1/auth/forgot-password - Email không tồn tại trả về 404 Not Found")
    void forgotPassword_UserNotFound_ReturnsNotFound() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("unknown@gmail.com")
                .build();

        doThrow(new AppException(ErrorCode.USER_NOT_FOUND))
                .when(authService).forgotPassword(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(2006))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("TC-04: POST /api/v1/auth/forgot-password - Spam yêu cầu trong 60s trả về 429 Too Many Requests")
    void forgotPassword_CooldownActive_ReturnsTooManyRequests() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("nguyenvana@gmail.com")
                .build();

        doThrow(new AppException(ErrorCode.OTP_COOLDOWN_ACTIVE))
                .when(authService).forgotPassword(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(2014))
                .andExpect(jsonPath("$.message").value(ErrorCode.OTP_COOLDOWN_ACTIVE.getMessage()));
    }

    // --- POST /api/v1/auth/verify-otp ---

    @Test
    @DisplayName("TC-05: POST /api/v1/auth/verify-otp - Xác thực OTP thành công trả về 200 OK")
    void verifyOtp_Success() throws Exception {
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("nguyenvana@gmail.com")
                .otpCode("849201")
                .build();

        doNothing().when(authService).verifyOtp(any(VerifyOtpRequest.class));

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Mã OTP hợp lệ"));

        verify(authService, times(1)).verifyOtp(any(VerifyOtpRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/verify-otp - Mã OTP không đủ 6 số trả về 400 Bad Request")
    void verifyOtp_InvalidOtpPattern_ReturnsBadRequest() throws Exception {
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("nguyenvana@gmail.com")
                .otpCode("123") // Không đúng 6 chữ số
                .build();

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400));

        verify(authService, never()).verifyOtp(any());
    }

    @Test
    @DisplayName("TC-06: POST /api/v1/auth/verify-otp - Sai mã OTP trả về 400 Bad Request và code 2011")
    void verifyOtp_WrongOtp_ReturnsBadRequest() throws Exception {
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("nguyenvana@gmail.com")
                .otpCode("000000")
                .build();

        doThrow(new AppException(ErrorCode.OTP_INVALID))
                .when(authService).verifyOtp(any(VerifyOtpRequest.class));

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(2011))
                .andExpect(jsonPath("$.message").value(ErrorCode.OTP_INVALID.getMessage()));
    }

    @Test
    @DisplayName("POST /api/v1/auth/verify-otp - OTP đã hết hạn trả về 400 Bad Request và code 2012")
    void verifyOtp_ExpiredOtp_ReturnsBadRequest() throws Exception {
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email("nguyenvana@gmail.com")
                .otpCode("849201")
                .build();

        doThrow(new AppException(ErrorCode.OTP_EXPIRED))
                .when(authService).verifyOtp(any(VerifyOtpRequest.class));

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(2012))
                .andExpect(jsonPath("$.message").value(ErrorCode.OTP_EXPIRED.getMessage()));
    }

    // --- POST /api/v1/auth/reset-password ---

    @Test
    @DisplayName("TC-07: POST /api/v1/auth/reset-password - Đổi mật khẩu thành công trả về 200 OK")
    void resetPassword_Success() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("nguyenvana@gmail.com")
                .otpCode("849201")
                .newPassword("NewPassword@123")
                .confirmPassword("NewPassword@123")
                .build();

        doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Đặt lại mật khẩu thành công. Bạn có thể đăng nhập bằng mật khẩu mới"));

        verify(authService, times(1)).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/reset-password - Mật khẩu mới dưới 6 ký tự trả về 400 Bad Request")
    void resetPassword_ShortPassword_ReturnsBadRequest() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("nguyenvana@gmail.com")
                .otpCode("849201")
                .newPassword("12345") // < 6 ký tự
                .confirmPassword("12345")
                .build();

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400));

        verify(authService, never()).resetPassword(any());
    }

    @Test
    @DisplayName("TC-08: POST /api/v1/auth/reset-password - Mật khẩu xác nhận không khớp trả về 400 và code 2007")
    void resetPassword_PasswordConfirmNotMatch_ReturnsBadRequest() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("nguyenvana@gmail.com")
                .otpCode("849201")
                .newPassword("NewPassword@123")
                .confirmPassword("differentPassword@123")
                .build();

        doThrow(new AppException(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH))
                .when(authService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(2007))
                .andExpect(jsonPath("$.message").value(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH.getMessage()));
    }

    @Test
    @DisplayName("TC-09: POST /api/v1/auth/logout - Đăng xuất thành công trả về 200 OK")
    void logout_Success_ReturnsOk() throws Exception {
        LogoutRequest request = LogoutRequest.builder()
                .token("mockJwtToken")
                .build();

        doNothing().when(authService).logout(any(LogoutRequest.class));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng xuất thành công"));

        verify(authService, times(1)).logout(any(LogoutRequest.class));
    }

    @Test
    @DisplayName("TC-10: POST /api/v1/auth/logout - Thất bại do token rỗng trả về 400 Bad Request")
    void logout_BlankToken_ReturnsBadRequest() throws Exception {
        LogoutRequest request = LogoutRequest.builder()
                .token("")
                .build();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(authService, never()).logout(any(LogoutRequest.class));
    }
}

