package com.fpms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpms.dto.request.RegisterRequest;
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
                .password("password123")
                .confirmPassword("password123")
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
                .password("password123")
                .confirmPassword("password123")
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
                .password("password123")
                .confirmPassword("password123")
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
                .password("password123")
                .confirmPassword("password123")
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
}

