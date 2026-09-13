package com.fpms.service;

import com.fpms.dto.request.RegisterRequest;
import com.fpms.dto.response.UserResponse;
import com.fpms.entity.Role;
import com.fpms.entity.User;
import com.fpms.entity.enums.AuthProvider;
import com.fpms.entity.enums.RoleName;
import com.fpms.entity.enums.UserStatus;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.repository.RoleRepository;
import com.fpms.repository.UserRepository;
import com.fpms.security.UserPrincipal;
import com.fpms.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.fpms.mapper.UserMapper userMapper;

    @Mock
    private com.fpms.security.JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest validRequest;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        validRequest = RegisterRequest.builder()
                .fullName("Nguyễn Văn A")
                .email("nguyenvana@gmail.com")
                .phoneNumber("0912345678")
                .password("password123")
                .confirmPassword("password123")
                .build();

        customerRole = Role.builder()
                .id(1L)
                .roleName(RoleName.ROLE_CUSTOMER)
                .description("Khách hàng")
                .build();
    }

    @Test
    @DisplayName("TC-05: Đăng ký tài khoản thành công với dữ liệu hợp lệ")
    void register_Success() {
        when(userRepository.existsByEmail("nguyenvana@gmail.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0912345678")).thenReturn(false);
        when(roleRepository.findByRoleName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedPasswordSample");
        User mappedUser = User.builder()
                .fullName("Nguyễn Văn A")
                .phoneNumber("0912345678")
                .email("nguyenvana@gmail.com")
                .status(UserStatus.ACTIVE)
                .authProvider(AuthProvider.LOCAL)
                .build();
        when(userMapper.toUser(validRequest)).thenReturn(mappedUser);

        authService.register(validRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("Nguyễn Văn A", savedUser.getFullName());
        assertEquals("nguyenvana@gmail.com", savedUser.getEmail());
        assertEquals("0912345678", savedUser.getPhoneNumber());
        assertEquals("$2a$10$hashedPasswordSample", savedUser.getPasswordHash());
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
        assertEquals(AuthProvider.LOCAL, savedUser.getAuthProvider());
        assertEquals(customerRole, savedUser.getRole());
    }

    @Test
    @DisplayName("TC-03: Ném ngoại lệ khi mật khẩu và mật khẩu xác nhận không khớp")
    void register_PasswordConfirmNotMatch_ThrowsAppException() {
        validRequest.setConfirmPassword("differentPassword");

        AppException exception = assertThrows(AppException.class, () -> authService.register(validRequest));

        assertEquals(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-04: Ném ngoại lệ khi email đã tồn tại trên hệ thống")
    void register_EmailAlreadyExists_ThrowsAppException() {
        when(userRepository.existsByEmail("nguyenvana@gmail.com")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> authService.register(validRequest));

        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-04: Ném ngoại lệ khi số điện thoại đã tồn tại trên hệ thống")
    void register_PhoneNumberAlreadyExists_ThrowsAppException() {
        when(userRepository.existsByEmail("nguyenvana@gmail.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0912345678")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> authService.register(validRequest));

        assertEquals(ErrorCode.PHONE_ALREADY_EXISTS, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném ngoại lệ ROLE_NOT_FOUND khi vai trò ROLE_CUSTOMER chưa được khởi tạo")
    void register_RoleNotFound_ThrowsAppException() {
        when(userRepository.existsByEmail("nguyenvana@gmail.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0912345678")).thenReturn(false);
        when(roleRepository.findByRoleName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.register(validRequest));

        assertEquals(ErrorCode.ROLE_NOT_FOUND, exception.getErrorCode());
        verify(userRepository, never()).save(any());
    }

    // ================= Test Cases Cho Chặng 2 (Login) =================

    @Test
    @DisplayName("TC-08: Đăng nhập thành công bằng Email hợp lệ")
    void login_Success_WithEmail() {
        com.fpms.dto.request.LoginRequest request = com.fpms.dto.request.LoginRequest.builder()
                .loginId("nguyenvana@gmail.com")
                .password("password123")
                .build();

        User user = User.builder()
                .email("nguyenvana@gmail.com")
                .phoneNumber("0912345678")
                .passwordHash("$2a$10$hashedPassword")
                .status(UserStatus.ACTIVE)
                .role(customerRole)
                .build();
        user.setId(1L);

        when(userRepository.findByEmail("nguyenvana@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("mockAccessToken");
        when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("mockRefreshToken");

        com.fpms.dto.response.UserResponse userResponse = com.fpms.dto.response.UserResponse.builder()
                .id(1L)
                .email("nguyenvana@gmail.com")
                .build();
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        com.fpms.dto.response.AuthResponse authResponse = authService.login(request);

        assertNotNull(authResponse);
        assertEquals("mockAccessToken", authResponse.getAccessToken());
        assertEquals("mockRefreshToken", authResponse.getRefreshToken());
        assertEquals("Bearer", authResponse.getTokenType());
        assertEquals(1L, authResponse.getUser().getId());
    }

    @Test
    @DisplayName("TC-08: Đăng nhập thành công bằng Số điện thoại hợp lệ")
    void login_Success_WithPhoneNumber() {
        com.fpms.dto.request.LoginRequest request = com.fpms.dto.request.LoginRequest.builder()
                .loginId("0912345678")
                .password("password123")
                .build();

        User user = User.builder()
                .email("nguyenvana@gmail.com")
                .phoneNumber("0912345678")
                .passwordHash("$2a$10$hashedPassword")
                .status(UserStatus.ACTIVE)
                .role(customerRole)
                .build();
        user.setId(1L);

        when(userRepository.findByPhoneNumber("0912345678")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("mockAccessToken");
        when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("mockRefreshToken");

        com.fpms.dto.response.UserResponse userResponse = com.fpms.dto.response.UserResponse.builder()
                .id(1L)
                .phoneNumber("0912345678")
                .build();
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        com.fpms.dto.response.AuthResponse authResponse = authService.login(request);

        assertNotNull(authResponse);
        assertEquals("mockAccessToken", authResponse.getAccessToken());
        assertEquals("mockRefreshToken", authResponse.getRefreshToken());
    }

    @Test
    @DisplayName("TC-06: Ném INVALID_CREDENTIALS khi tài khoản không tồn tại")
    void login_UserNotFound_ThrowsInvalidCredentials() {
        com.fpms.dto.request.LoginRequest request = com.fpms.dto.request.LoginRequest.builder()
                .loginId("notfound@gmail.com")
                .password("password123")
                .build();

        when(userRepository.findByEmail("notfound@gmail.com")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));

        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
    }

    @Test
    @DisplayName("TC-06: Ném INVALID_CREDENTIALS khi mật khẩu không chính xác")
    void login_WrongPassword_ThrowsInvalidCredentials() {
        com.fpms.dto.request.LoginRequest request = com.fpms.dto.request.LoginRequest.builder()
                .loginId("nguyenvana@gmail.com")
                .password("wrongPassword")
                .build();

        User user = User.builder()
                .email("nguyenvana@gmail.com")
                .passwordHash("$2a$10$hashedPassword")
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail("nguyenvana@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "$2a$10$hashedPassword")).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));

        assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
    }

    @Test
    @DisplayName("TC-07: Ném USER_LOCKED khi tài khoản bị khóa")
    void login_UserLocked_ThrowsUserLocked() {
        com.fpms.dto.request.LoginRequest request = com.fpms.dto.request.LoginRequest.builder()
                .loginId("nguyenvana@gmail.com")
                .password("password123")
                .build();

        User user = User.builder()
                .email("nguyenvana@gmail.com")
                .passwordHash("$2a$10$hashedPassword")
                .status(UserStatus.LOCKED)
                .build();

        when(userRepository.findByEmail("nguyenvana@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$10$hashedPassword")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> authService.login(request));

        assertEquals(ErrorCode.USER_LOCKED, exception.getErrorCode());
    }

    // ================= Test Cases Cho Chặng 3 (Current User / Me) =================

    @Test
    @DisplayName("Lấy thông tin người dùng hiện tại thành công với UserPrincipal hợp lệ")
    void getCurrentUser_Success() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(1L)
                .email("nguyenvana@gmail.com")
                .build();

        User user = User.builder()
                .email("nguyenvana@gmail.com")
                .status(UserStatus.ACTIVE)
                .role(customerRole)
                .build();
        user.setId(1L);

        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .email("nguyenvana@gmail.com")
                .role(RoleName.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        UserResponse result = authService.getCurrentUser(principal);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("nguyenvana@gmail.com", result.getEmail());
    }

    @Test
    @DisplayName("Ném USER_NOT_FOUND khi User ID trong Principal không tồn tại trong DB")
    void getCurrentUser_UserNotFound_ThrowsException() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(999L)
                .email("notfound@gmail.com")
                .build();

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.getCurrentUser(principal));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Ném USER_LOCKED khi tài khoản người dùng đang bị khóa")
    void getCurrentUser_UserLocked_ThrowsException() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(1L)
                .email("locked@gmail.com")
                .build();

        User user = User.builder()
                .status(UserStatus.LOCKED)
                .build();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        AppException exception = assertThrows(AppException.class, () -> authService.getCurrentUser(principal));

        assertEquals(ErrorCode.USER_LOCKED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Ném INVALID_TOKEN khi UserPrincipal là null")
    void getCurrentUser_NullPrincipal_ThrowsException() {
        AppException exception = assertThrows(AppException.class, () -> authService.getCurrentUser(null));

        assertEquals(ErrorCode.INVALID_TOKEN, exception.getErrorCode());
    }
}

