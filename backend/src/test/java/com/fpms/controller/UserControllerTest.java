package com.fpms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpms.dto.request.ChangePasswordRequest;
import com.fpms.dto.request.UpdateProfileRequest;
import com.fpms.dto.response.UserResponse;
import com.fpms.entity.enums.RoleName;
import com.fpms.entity.enums.UserStatus;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.exception.GlobalExceptionHandler;
import com.fpms.security.UserPrincipal;
import com.fpms.service.UserService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserPrincipal userPrincipal;
    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("khachhang@gmail.com")
                .fullName("Nguyễn Văn A")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .build();

        auth = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
    }

    // ================= Test Cases Cho GET /api/v1/users/me =================

    @Test
    @DisplayName("TC-01: GET /api/v1/users/me - Lấy hồ sơ cá nhân thành công trả về 200 OK")
    void getMyProfile_Success() throws Exception {
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("khachhang@gmail.com")
                .fullName("Nguyễn Văn A")
                .phoneNumber("0912345678")
                .role(RoleName.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        try {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            when(userService.getMyProfile(any(UserPrincipal.class))).thenReturn(userResponse);

            mockMvc.perform(get("/api/v1/users/me")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.email").value("khachhang@gmail.com"))
                    .andExpect(jsonPath("$.data.fullName").value("Nguyễn Văn A"));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    // ================= Test Cases Cho PUT /api/v1/users/me =================

    @Test
    @DisplayName("TC-03: PUT /api/v1/users/me - Cập nhật profile thành công trả về 200 OK")
    void updateMyProfile_Success() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyễn Văn A (Đã sửa)")
                .phoneNumber("0988776655")
                .build();

        UserResponse updatedResponse = UserResponse.builder()
                .id(1L)
                .email("khachhang@gmail.com")
                .fullName("Nguyễn Văn A (Đã sửa)")
                .phoneNumber("0988776655")
                .role(RoleName.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        try {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            when(userService.updateMyProfile(any(UserPrincipal.class), any(UpdateProfileRequest.class)))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put("/api/v1/users/me")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Cập nhật thông tin cá nhân thành công"))
                    .andExpect(jsonPath("$.data.fullName").value("Nguyễn Văn A (Đã sửa)"))
                    .andExpect(jsonPath("$.data.phoneNumber").value("0988776655"));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("TC-06: PUT /api/v1/users/me - Số điện thoại sai định dạng trả về 400 Bad Request")
    void updateMyProfile_InvalidPhoneNumber_ReturnsBadRequest() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyễn Văn A")
                .phoneNumber("12345") // Không đúng 10 số bắt đầu bằng 0
                .build();

        mockMvc.perform(put("/api/v1/users/me")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400));

        verify(userService, never()).updateMyProfile(any(), any());
    }

    @Test
    @DisplayName("TC-05: PUT /api/v1/users/me - SĐT đã tồn tại trả về 400 Bad Request và code 2002")
    void updateMyProfile_PhoneAlreadyExists_ReturnsBadRequest() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyễn Văn A")
                .phoneNumber("0999999999")
                .build();

        try {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            doThrow(new AppException(ErrorCode.PHONE_ALREADY_EXISTS))
                    .when(userService).updateMyProfile(any(UserPrincipal.class), any(UpdateProfileRequest.class));

            mockMvc.perform(put("/api/v1/users/me")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(2002))
                    .andExpect(jsonPath("$.message").value(ErrorCode.PHONE_ALREADY_EXISTS.getMessage()));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    // ================= Test Cases Cho POST /api/v1/users/change-password =================

    @Test
    @DisplayName("TC-07: POST /api/v1/users/change-password - Đổi mật khẩu thành công trả về 200 OK")
    void changePassword_Success() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword123")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build();

        try {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            doNothing().when(userService).changePassword(any(UserPrincipal.class), any(ChangePasswordRequest.class));

            mockMvc.perform(post("/api/v1/users/change-password")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công. Vui lòng sử dụng mật khẩu mới cho lần đăng nhập sau"));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("POST /api/v1/users/change-password - Mật khẩu mới dưới 6 ký tự trả về 400 Bad Request")
    void changePassword_ShortNewPassword_ReturnsBadRequest() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword123")
                .newPassword("123") // < 6 ký tự
                .confirmPassword("123")
                .build();

        mockMvc.perform(post("/api/v1/users/change-password")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400));

        verify(userService, never()).changePassword(any(), any());
    }

    @Test
    @DisplayName("TC-08: POST /api/v1/users/change-password - Nhập sai pass cũ trả về 400 và code 2006")
    void changePassword_WrongOldPassword_ReturnsBadRequest() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrongOldPassword")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build();

        try {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            doThrow(new AppException(ErrorCode.OLD_PASSWORD_INCORRECT))
                    .when(userService).changePassword(any(UserPrincipal.class), any(ChangePasswordRequest.class));

            mockMvc.perform(post("/api/v1/users/change-password")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(2016))
                    .andExpect(jsonPath("$.message").value(ErrorCode.OLD_PASSWORD_INCORRECT.getMessage()));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("TC-09: POST /api/v1/users/change-password - Mật khẩu xác nhận không khớp trả về 400 và code 2007")
    void changePassword_PasswordConfirmNotMatch_ReturnsBadRequest() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword123")
                .newPassword("newPassword123")
                .confirmPassword("differentPassword")
                .build();

        try {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
            doThrow(new AppException(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH))
                    .when(userService).changePassword(any(UserPrincipal.class), any(ChangePasswordRequest.class));

            mockMvc.perform(post("/api/v1/users/change-password")
                            .principal(auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(2007))
                    .andExpect(jsonPath("$.message").value(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH.getMessage()));
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}
