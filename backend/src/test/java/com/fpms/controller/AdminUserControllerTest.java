package com.fpms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpms.common.response.PageResponse;
import com.fpms.dto.request.UpdateUserRoleRequest;
import com.fpms.dto.request.UpdateUserStatusRequest;
import com.fpms.dto.response.UserResponse;
import com.fpms.entity.enums.RoleName;
import com.fpms.entity.enums.UserStatus;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.exception.GlobalExceptionHandler;
import com.fpms.security.UserPrincipal;
import com.fpms.service.AdminUserService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController adminUserController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserPrincipal adminPrincipal;
    private UsernamePasswordAuthenticationToken auth;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminUserController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        adminPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("admin@soccer365.vn")
                .fullName("Quản trị viên")
                .build();

        auth = new UsernamePasswordAuthenticationToken(
                adminPrincipal,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        userResponse = UserResponse.builder()
                .id(2L)
                .email("customer@gmail.com")
                .fullName("Nguyễn Khách Hàng")
                .role(RoleName.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("TC_CTL_01: GET /api/v1/admin/users - Lấy danh sách thành công")
    void getUsers_Success() throws Exception {
        PageResponse<UserResponse> pageResponse = PageResponse.<UserResponse>builder()
                .pageNo(1)
                .pageSize(10)
                .totalElements(1L)
                .totalPages(1)
                .isLast(true)
                .items(List.of(userResponse))
                .build();

        when(adminUserService.getUsers("nguyen", RoleName.ROLE_CUSTOMER, UserStatus.ACTIVE, 1, 10))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/users")
                        .principal(auth)
                        .param("keyword", "nguyen")
                        .param("role", "ROLE_CUSTOMER")
                        .param("status", "ACTIVE")
                        .param("page", "1")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items[0].email").value("customer@gmail.com"));
    }

    @Test
    @DisplayName("TC_CTL_02: GET /api/v1/admin/users/{id} - Lấy chi tiết thành công")
    void getUserById_Success() throws Exception {
        when(adminUserService.getUserById(2L)).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/admin/users/2")
                        .principal(auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(2L))
                .andExpect(jsonPath("$.data.email").value("customer@gmail.com"));
    }

    @Test
    @DisplayName("TC_CTL_03: GET /api/v1/admin/users/{id} - Không tìm thấy ném lỗi 404")
    void getUserById_NotFound() throws Exception {
        when(adminUserService.getUserById(999L)).thenThrow(new AppException(ErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/admin/users/999")
                        .principal(auth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(2006));
    }

    @Test
    @DisplayName("TC_CTL_04: PATCH /api/v1/admin/users/{id}/status - Khóa tài khoản thành công")
    void updateUserStatus_Success() throws Exception {
        UpdateUserStatusRequest request = UpdateUserStatusRequest.builder()
                .status(UserStatus.LOCKED)
                .build();

        UserResponse lockedResponse = UserResponse.builder()
                .id(2L)
                .email("customer@gmail.com")
                .role(RoleName.ROLE_CUSTOMER)
                .status(UserStatus.LOCKED)
                .build();

        when(adminUserService.updateUserStatus(any(), eq(2L), any())).thenReturn(lockedResponse);

        mockMvc.perform(patch("/api/v1/admin/users/2/status")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("LOCKED"));
    }

    @Test
    @DisplayName("TC_CTL_05: PATCH /api/v1/admin/users/{id}/status - Body rỗng báo lỗi 400")
    void updateUserStatus_ValidationFailure() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/2/status")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC_CTL_06: PATCH /api/v1/admin/users/{id}/status - Tự khóa chính mình ném lỗi 400")
    void updateUserStatus_SelfLock_BadRequest() throws Exception {
        UpdateUserStatusRequest request = UpdateUserStatusRequest.builder()
                .status(UserStatus.LOCKED)
                .build();

        when(adminUserService.updateUserStatus(any(), eq(1L), any()))
                .thenThrow(new AppException(ErrorCode.CANNOT_LOCK_SELF));

        mockMvc.perform(patch("/api/v1/admin/users/1/status")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2018));
    }

    @Test
    @DisplayName("TC_CTL_07: PATCH /api/v1/admin/users/{id}/role - Cấp quyền Staff thành công")
    void updateUserRole_Success() throws Exception {
        UpdateUserRoleRequest request = UpdateUserRoleRequest.builder()
                .role(RoleName.ROLE_STAFF)
                .build();

        UserResponse staffResponse = UserResponse.builder()
                .id(2L)
                .email("customer@gmail.com")
                .role(RoleName.ROLE_STAFF)
                .status(UserStatus.ACTIVE)
                .build();

        when(adminUserService.updateUserRole(any(), eq(2L), any())).thenReturn(staffResponse);

        mockMvc.perform(patch("/api/v1/admin/users/2/role")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.role").value("ROLE_STAFF"));
    }

    @Test
    @DisplayName("TC_CTL_08: PATCH /api/v1/admin/users/{id}/role - Gán ROLE_ADMIN trái phép ném lỗi 400")
    void updateUserRole_AssignAdminRole_BadRequest() throws Exception {
        UpdateUserRoleRequest request = UpdateUserRoleRequest.builder()
                .role(RoleName.ROLE_ADMIN)
                .build();

        when(adminUserService.updateUserRole(any(), eq(2L), any()))
                .thenThrow(new AppException(ErrorCode.INVALID_ROLE_ASSIGNMENT));

        mockMvc.perform(patch("/api/v1/admin/users/2/role")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2022));
    }
}
