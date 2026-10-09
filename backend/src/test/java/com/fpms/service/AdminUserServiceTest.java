package com.fpms.service;

import com.fpms.common.response.PageResponse;
import com.fpms.dto.request.UpdateUserRoleRequest;
import com.fpms.dto.request.UpdateUserStatusRequest;
import com.fpms.dto.response.UserResponse;
import com.fpms.entity.Role;
import com.fpms.entity.User;
import com.fpms.entity.enums.RoleName;
import com.fpms.entity.enums.UserStatus;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.mapper.UserMapper;
import com.fpms.repository.RoleRepository;
import com.fpms.repository.UserRepository;
import com.fpms.security.UserPrincipal;
import com.fpms.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private UserPrincipal currentAdmin;
    private Role adminRole;
    private Role customerRole;
    private Role staffRole;
    private User adminUser;
    private User customerUser;
    private User staffUser;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        currentAdmin = UserPrincipal.builder()
                .id(1L)
                .email("admin@soccer365.vn")
                .fullName("Quản trị viên")
                .build();

        adminRole = Role.builder().id(1L).roleName(RoleName.ROLE_ADMIN).build();
        customerRole = Role.builder().id(2L).roleName(RoleName.ROLE_CUSTOMER).build();
        staffRole = Role.builder().id(3L).roleName(RoleName.ROLE_STAFF).build();

        adminUser = User.builder()
                .email("admin@soccer365.vn")
                .fullName("Quản trị viên")
                .role(adminRole)
                .status(UserStatus.ACTIVE)
                .build();
        adminUser.setId(1L);
        adminUser.setIsDeleted(false);

        customerUser = User.builder()
                .email("customer@gmail.com")
                .phoneNumber("0987654321")
                .fullName("Nguyễn Khách Hàng")
                .role(customerRole)
                .status(UserStatus.ACTIVE)
                .build();
        customerUser.setId(2L);
        customerUser.setIsDeleted(false);

        staffUser = User.builder()
                .email("staff@soccer365.vn")
                .phoneNumber("0912345678")
                .fullName("Lê Nhân Viên")
                .role(staffRole)
                .status(UserStatus.ACTIVE)
                .build();
        staffUser.setId(3L);
        staffUser.setIsDeleted(false);

        userResponse = UserResponse.builder()
                .id(2L)
                .email("customer@gmail.com")
                .fullName("Nguyễn Khách Hàng")
                .role(RoleName.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("TC_SRV_01: Lấy danh sách người dùng thành công với bộ lọc và phân trang")
    void getUsers_Success() {
        Page<User> pageMock = new PageImpl<>(Collections.singletonList(customerUser));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pageMock);
        when(userMapper.toUserResponse(customerUser)).thenReturn(userResponse);

        PageResponse<UserResponse> result = adminUserService.getUsers("nguyen", RoleName.ROLE_CUSTOMER, UserStatus.ACTIVE, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals("customer@gmail.com", result.getItems().get(0).getEmail());
        verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("TC_SRV_02: Lấy chi tiết người dùng theo ID thành công")
    void getUserById_Success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(customerUser));
        when(userMapper.toUserResponse(customerUser)).thenReturn(userResponse);

        UserResponse result = adminUserService.getUserById(2L);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("customer@gmail.com", result.getEmail());
    }

    @Test
    @DisplayName("TC_SRV_03: Lấy chi tiết thất bại khi không tìm thấy người dùng")
    void getUserById_NotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> adminUserService.getUserById(999L));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("TC_SRV_04: Khóa tài khoản người dùng thành công")
    void updateUserStatus_Success_Lock() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(customerUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(userResponse);

        UpdateUserStatusRequest request = UpdateUserStatusRequest.builder()
                .status(UserStatus.LOCKED)
                .build();

        UserResponse result = adminUserService.updateUserStatus(currentAdmin, 2L, request);

        assertNotNull(result);
        assertEquals(UserStatus.LOCKED, customerUser.getStatus());
        verify(userRepository).save(customerUser);
    }

    @Test
    @DisplayName("TC_SRV_05: Mở khóa tài khoản người dùng thành công")
    void updateUserStatus_Success_Unlock() {
        customerUser.setStatus(UserStatus.LOCKED);
        when(userRepository.findById(2L)).thenReturn(Optional.of(customerUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(userResponse);

        UpdateUserStatusRequest request = UpdateUserStatusRequest.builder()
                .status(UserStatus.ACTIVE)
                .build();

        UserResponse result = adminUserService.updateUserStatus(currentAdmin, 2L, request);

        assertNotNull(result);
        assertEquals(UserStatus.ACTIVE, customerUser.getStatus());
        verify(userRepository).save(customerUser);
    }

    @Test
    @DisplayName("TC_SRV_06: Chặn Admin tự khóa tài khoản của chính mình")
    void updateUserStatus_SelfLock_ThrowsException() {
        UpdateUserStatusRequest request = UpdateUserStatusRequest.builder()
                .status(UserStatus.LOCKED)
                .build();

        AppException exception = assertThrows(AppException.class, () ->
                adminUserService.updateUserStatus(currentAdmin, 1L, request));

        assertEquals(ErrorCode.CANNOT_LOCK_SELF, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC_SRV_07: Chặn khóa tài khoản của Quản trị viên khác")
    void updateUserStatus_TargetIsAdmin_ThrowsException() {
        User otherAdmin = User.builder()
                .email("otheradmin@soccer365.vn")
                .role(adminRole)
                .status(UserStatus.ACTIVE)
                .build();
        otherAdmin.setId(99L);

        when(userRepository.findById(99L)).thenReturn(Optional.of(otherAdmin));

        UpdateUserStatusRequest request = UpdateUserStatusRequest.builder()
                .status(UserStatus.LOCKED)
                .build();

        AppException exception = assertThrows(AppException.class, () ->
                adminUserService.updateUserStatus(currentAdmin, 99L, request));

        assertEquals(ErrorCode.CANNOT_LOCK_ADMIN, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC_SRV_08: Cấp quyền Staff cho khách hàng thành công")
    void updateUserRole_Success_CustomerToStaff() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(customerUser));
        when(roleRepository.findByRoleName(RoleName.ROLE_STAFF)).thenReturn(Optional.of(staffRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(userResponse);

        UpdateUserRoleRequest request = UpdateUserRoleRequest.builder()
                .role(RoleName.ROLE_STAFF)
                .build();

        UserResponse result = adminUserService.updateUserRole(currentAdmin, 2L, request);

        assertNotNull(result);
        assertEquals(staffRole, customerUser.getRole());
        verify(userRepository).save(customerUser);
    }

    @Test
    @DisplayName("TC_SRV_09: Thu hồi quyền Staff về khách hàng thành công")
    void updateUserRole_Success_StaffToCustomer() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(staffUser));
        when(roleRepository.findByRoleName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(userResponse);

        UpdateUserRoleRequest request = UpdateUserRoleRequest.builder()
                .role(RoleName.ROLE_CUSTOMER)
                .build();

        UserResponse result = adminUserService.updateUserRole(currentAdmin, 3L, request);

        assertNotNull(result);
        assertEquals(customerRole, staffUser.getRole());
        verify(userRepository).save(staffUser);
    }

    @Test
    @DisplayName("TC_SRV_10: Chặn Admin tự thay đổi vai trò của chính mình")
    void updateUserRole_SelfChange_ThrowsException() {
        UpdateUserRoleRequest request = UpdateUserRoleRequest.builder()
                .role(RoleName.ROLE_STAFF)
                .build();

        AppException exception = assertThrows(AppException.class, () ->
                adminUserService.updateUserRole(currentAdmin, 1L, request));

        assertEquals(ErrorCode.CANNOT_CHANGE_OWN_ROLE, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC_SRV_11: Chặn thay đổi vai trò của Quản trị viên khác")
    void updateUserRole_TargetIsAdmin_ThrowsException() {
        User otherAdmin = User.builder()
                .email("otheradmin@soccer365.vn")
                .role(adminRole)
                .status(UserStatus.ACTIVE)
                .build();
        otherAdmin.setId(99L);

        when(userRepository.findById(99L)).thenReturn(Optional.of(otherAdmin));

        UpdateUserRoleRequest request = UpdateUserRoleRequest.builder()
                .role(RoleName.ROLE_STAFF)
                .build();

        AppException exception = assertThrows(AppException.class, () ->
                adminUserService.updateUserRole(currentAdmin, 99L, request));

        assertEquals(ErrorCode.CANNOT_MODIFY_ADMIN, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC_SRV_12: Chặn leo thang đặc quyền cố tình gán ROLE_ADMIN")
    void updateUserRole_AssignAdminRole_ThrowsException() {
        UpdateUserRoleRequest request = UpdateUserRoleRequest.builder()
                .role(RoleName.ROLE_ADMIN)
                .build();

        AppException exception = assertThrows(AppException.class, () ->
                adminUserService.updateUserRole(currentAdmin, 2L, request));

        assertEquals(ErrorCode.INVALID_ROLE_ASSIGNMENT, exception.getErrorCode());
        verify(userRepository, never()).findById(any());
    }
}
