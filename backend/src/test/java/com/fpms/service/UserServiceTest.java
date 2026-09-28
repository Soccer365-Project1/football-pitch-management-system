package com.fpms.service;

import com.fpms.dto.request.ChangePasswordRequest;
import com.fpms.dto.request.UpdateProfileRequest;
import com.fpms.dto.response.UserResponse;
import com.fpms.entity.Role;
import com.fpms.entity.User;
import com.fpms.entity.enums.RoleName;
import com.fpms.entity.enums.UserStatus;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.mapper.UserMapper;
import com.fpms.repository.UserRepository;
import com.fpms.security.UserPrincipal;
import com.fpms.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserPrincipal validPrincipal;
    private User user;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        validPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("khachhang@gmail.com")
                .fullName("Nguyễn Văn A")
                .build();

        customerRole = Role.builder()
                .id(1L)
                .roleName(RoleName.ROLE_CUSTOMER)
                .build();

        user = User.builder()
                .email("khachhang@gmail.com")
                .fullName("Nguyễn Văn A")
                .phoneNumber("0912345678")
                .passwordHash("$2a$10$oldHashedPassword")
                .status(UserStatus.ACTIVE)
                .role(customerRole)
                .build();
        user.setId(1L);
    }

    // ================= Test Cases Cho getMyProfile =================

    @Test
    @DisplayName("TC-01: Lấy thông tin hồ sơ thành công với Principal hợp lệ")
    void getMyProfile_Success() {
        UserResponse mockResponse = UserResponse.builder()
                .id(1L)
                .email("khachhang@gmail.com")
                .fullName("Nguyễn Văn A")
                .phoneNumber("0912345678")
                .role(RoleName.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(mockResponse);

        UserResponse result = userService.getMyProfile(validPrincipal);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("khachhang@gmail.com", result.getEmail());
        assertEquals("Nguyễn Văn A", result.getFullName());
    }

    @Test
    @DisplayName("Ném INVALID_TOKEN khi UserPrincipal là null hoặc id rỗng")
    void getMyProfile_NullPrincipal_ThrowsException() {
        AppException exception = assertThrows(AppException.class, () -> userService.getMyProfile(null));
        assertEquals(ErrorCode.INVALID_TOKEN, exception.getErrorCode());

        UserPrincipal invalidPrincipal = UserPrincipal.builder().id(null).build();
        AppException exception2 = assertThrows(AppException.class, () -> userService.getMyProfile(invalidPrincipal));
        assertEquals(ErrorCode.INVALID_TOKEN, exception2.getErrorCode());
    }

    @Test
    @DisplayName("Ném USER_NOT_FOUND khi người dùng không tồn tại trong CSDL")
    void getMyProfile_UserNotFound_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> userService.getMyProfile(validPrincipal));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Ném USER_LOCKED khi tài khoản người dùng bị khóa")
    void getMyProfile_UserLocked_ThrowsException() {
        user.setStatus(UserStatus.LOCKED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        AppException exception = assertThrows(AppException.class, () -> userService.getMyProfile(validPrincipal));
        assertEquals(ErrorCode.USER_LOCKED, exception.getErrorCode());
    }

    // ================= Test Cases Cho updateMyProfile =================

    @Test
    @DisplayName("TC-03: Cập nhật Họ tên và Số điện thoại thành công")
    void updateMyProfile_Success() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyễn Văn A (Đã sửa)")
                .phoneNumber("0988776655")
                .build();

        UserResponse updatedResponse = UserResponse.builder()
                .id(1L)
                .email("khachhang@gmail.com")
                .fullName("Nguyễn Văn A (Đã sửa)")
                .phoneNumber("0988776655")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneNumberAndIdNot("0988776655", 1L)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toUserResponse(user)).thenReturn(updatedResponse);

        UserResponse result = userService.updateMyProfile(validPrincipal, request);

        assertNotNull(result);
        assertEquals("Nguyễn Văn A (Đã sửa)", result.getFullName());
        assertEquals("0988776655", result.getPhoneNumber());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("TC-05: Ném PHONE_ALREADY_EXISTS khi SĐT mới đã thuộc về tài khoản khác")
    void updateMyProfile_PhoneAlreadyExists_ThrowsException() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyễn Văn A")
                .phoneNumber("0999999999")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneNumberAndIdNot("0999999999", 1L)).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> userService.updateMyProfile(validPrincipal, request));

        assertEquals(ErrorCode.PHONE_ALREADY_EXISTS, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném INVALID_TOKEN khi cập nhật profile với Principal rỗng")
    void updateMyProfile_NullPrincipal_ThrowsException() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Nguyễn Văn A")
                .phoneNumber("0988776655")
                .build();

        AppException exception = assertThrows(AppException.class, () -> userService.updateMyProfile(null, request));
        assertEquals(ErrorCode.INVALID_TOKEN, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    // ================= Test Cases Cho changePassword =================

    @Test
    @DisplayName("TC-07: Đổi mật khẩu thành công khi đúng pass cũ và pass mới khớp nhau")
    void changePassword_Success() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword123")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword123", "$2a$10$oldHashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$10$newHashedPassword");

        userService.changePassword(validPrincipal, request);

        assertEquals("$2a$10$newHashedPassword", user.getPasswordHash());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("TC-09: Ném PASSWORD_CONFIRM_NOT_MATCH khi mật khẩu mới và xác nhận không khớp")
    void changePassword_PasswordConfirmNotMatch_ThrowsException() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword123")
                .newPassword("newPassword123")
                .confirmPassword("differentPassword")
                .build();

        AppException exception = assertThrows(AppException.class, () -> userService.changePassword(validPrincipal, request));

        assertEquals(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-08: Ném OLD_PASSWORD_INCORRECT khi nhập sai mật khẩu hiện tại")
    void changePassword_WrongCurrentPassword_ThrowsException() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrongOldPassword")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongOldPassword", "$2a$10$oldHashedPassword")).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> userService.changePassword(validPrincipal, request));

        assertEquals(ErrorCode.OLD_PASSWORD_INCORRECT, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném USER_NOT_FOUND khi tài khoản không tồn tại lúc đổi mật khẩu")
    void changePassword_UserNotFound_ThrowsException() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword123")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> userService.changePassword(validPrincipal, request));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-08b: Ném NEW_PASSWORD_SAME_AS_OLD khi mật khẩu mới trùng với mật khẩu hiện tại")
    void changePassword_NewPasswordSameAsOld_ThrowsException() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword123")
                .newPassword("oldPassword123")
                .confirmPassword("oldPassword123")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword123", "$2a$10$oldHashedPassword")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> userService.changePassword(validPrincipal, request));

        assertEquals(ErrorCode.NEW_PASSWORD_SAME_AS_OLD, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }
}
