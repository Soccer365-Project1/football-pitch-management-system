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

import com.fpms.dto.request.ForgotPasswordRequest;
import com.fpms.dto.request.ResetPasswordRequest;
import com.fpms.dto.request.VerifyOtpRequest;
import com.fpms.entity.PasswordReset;
import com.fpms.repository.PasswordResetRepository;
import com.fpms.security.GoogleTokenVerifier;
import com.fpms.service.EmailService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
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
    private PasswordResetRepository passwordResetRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.fpms.mapper.UserMapper userMapper;

    @Mock
    private com.fpms.security.JwtTokenProvider jwtTokenProvider;

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @Mock
    private EmailService emailService;

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

    // ================= Test Cases Cho Subtask ST-02 (Quên & Đặt Lại Mật Khẩu) =================

    // --- forgotPassword ---

    @Test
    @DisplayName("TC-01: Yêu cầu cấp OTP thành công khi email tồn tại và không bị spam")
    void forgotPassword_Success() {
        String email = "nguyenvana@gmail.com";
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email(email).build();

        User user = User.builder()
                .email(email)
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(1L);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordResetRepository.findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(email))
                .thenReturn(Optional.empty());
        when(passwordResetRepository.findByEmailAndIsUsedFalse(email))
                .thenReturn(Collections.emptyList());

        authService.forgotPassword(request);

        ArgumentCaptor<PasswordReset> resetCaptor = ArgumentCaptor.forClass(PasswordReset.class);
        verify(passwordResetRepository, times(1)).save(resetCaptor.capture());
        PasswordReset savedReset = resetCaptor.getValue();
        assertEquals(email, savedReset.getEmail());
        assertNotNull(savedReset.getOtpCode());
        assertEquals(6, savedReset.getOtpCode().length());
        assertFalse(savedReset.getIsUsed());
        assertNotNull(savedReset.getExpiresAt());

        verify(emailService, times(1)).sendOtpEmail(eq(email), eq(savedReset.getOtpCode()), eq(10));
    }

    @Test
    @DisplayName("TC-02: Ném USER_NOT_FOUND khi yêu cầu OTP với email không tồn tại")
    void forgotPassword_UserNotFound_ThrowsException() {
        String email = "unknown@gmail.com";
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email(email).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.forgotPassword(request));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(emailService, never()).sendOtpEmail(any(), any(), anyInt());
        verify(passwordResetRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-03: Ném USER_LOCKED khi yêu cầu OTP cho tài khoản bị khóa")
    void forgotPassword_UserLocked_ThrowsException() {
        String email = "locked@gmail.com";
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email(email).build();

        User user = User.builder()
                .email(email)
                .status(UserStatus.LOCKED)
                .build();
        user.setId(1L);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        AppException exception = assertThrows(AppException.class, () -> authService.forgotPassword(request));

        assertEquals(ErrorCode.USER_LOCKED, exception.getErrorCode());
        verify(emailService, never()).sendOtpEmail(any(), any(), anyInt());
        verify(passwordResetRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-04: Ném OTP_COOLDOWN_ACTIVE khi gửi yêu cầu liên tục trong vòng 60 giây")
    void forgotPassword_CooldownActive_ThrowsException() {
        String email = "nguyenvana@gmail.com";
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email(email).build();

        User user = User.builder()
                .email(email)
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(1L);

        PasswordReset recentReset = PasswordReset.builder()
                .id(10L)
                .email(email)
                .otpCode("123456")
                .createdAt(LocalDateTime.now().minusSeconds(30))
                .isUsed(false)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordResetRepository.findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(email))
                .thenReturn(Optional.of(recentReset));

        AppException exception = assertThrows(AppException.class, () -> authService.forgotPassword(request));

        assertEquals(ErrorCode.OTP_COOLDOWN_ACTIVE, exception.getErrorCode());
        verify(emailService, never()).sendOtpEmail(any(), any(), anyInt());
        verify(passwordResetRepository, never()).save(any());
    }

    @Test
    @DisplayName("Vô hiệu hóa toàn bộ OTP cũ khi sinh mã OTP mới")
    void forgotPassword_InvalidatesOldOtps() {
        String email = "nguyenvana@gmail.com";
        ForgotPasswordRequest request = ForgotPasswordRequest.builder().email(email).build();

        User user = User.builder()
                .email(email)
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(1L);

        PasswordReset oldReset1 = PasswordReset.builder().id(1L).email(email).isUsed(false).createdAt(LocalDateTime.now().minusMinutes(5)).build();
        PasswordReset oldReset2 = PasswordReset.builder().id(2L).email(email).isUsed(false).createdAt(LocalDateTime.now().minusMinutes(3)).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordResetRepository.findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(email))
                .thenReturn(Optional.of(PasswordReset.builder().id(2L).email(email).isUsed(false).createdAt(LocalDateTime.now().minusSeconds(65)).build()));
        when(passwordResetRepository.findByEmailAndIsUsedFalse(email))
                .thenReturn(List.of(oldReset1, oldReset2));

        authService.forgotPassword(request);

        assertTrue(oldReset1.getIsUsed());
        assertTrue(oldReset2.getIsUsed());
        verify(passwordResetRepository, times(1)).saveAll(anyList());
        verify(passwordResetRepository, times(1)).save(any(PasswordReset.class));
        verify(emailService, times(1)).sendOtpEmail(eq(email), anyString(), eq(10));
    }

    // --- verifyOtp ---

    @Test
    @DisplayName("TC-05: Xác thực OTP thành công với mã hợp lệ và còn hạn")
    void verifyOtp_Success() {
        String email = "nguyenvana@gmail.com";
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email(email)
                .otpCode("849201")
                .build();

        PasswordReset validRecord = PasswordReset.builder()
                .id(1L)
                .email(email)
                .otpCode("849201")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .build();

        when(passwordResetRepository.findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(email))
                .thenReturn(Optional.of(validRecord));

        assertDoesNotThrow(() -> authService.verifyOtp(request));
    }

    @Test
    @DisplayName("Ném OTP_INVALID khi không tìm thấy bản ghi OTP chưa sử dụng")
    void verifyOtp_NotFound_ThrowsException() {
        String email = "nguyenvana@gmail.com";
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email(email)
                .otpCode("849201")
                .build();

        when(passwordResetRepository.findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(email))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.verifyOtp(request));

        assertEquals(ErrorCode.OTP_INVALID, exception.getErrorCode());
    }

    @Test
    @DisplayName("Ném OTP_EXPIRED khi mã OTP đã hết hạn 10 phút")
    void verifyOtp_Expired_ThrowsException() {
        String email = "nguyenvana@gmail.com";
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email(email)
                .otpCode("849201")
                .build();

        PasswordReset expiredRecord = PasswordReset.builder()
                .id(1L)
                .email(email)
                .otpCode("849201")
                .expiresAt(LocalDateTime.now().minusSeconds(10))
                .isUsed(false)
                .build();

        when(passwordResetRepository.findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(email))
                .thenReturn(Optional.of(expiredRecord));

        AppException exception = assertThrows(AppException.class, () -> authService.verifyOtp(request));

        assertEquals(ErrorCode.OTP_EXPIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("TC-06: Ném OTP_INVALID khi nhập sai mã OTP")
    void verifyOtp_WrongOtp_ThrowsException() {
        String email = "nguyenvana@gmail.com";
        VerifyOtpRequest request = VerifyOtpRequest.builder()
                .email(email)
                .otpCode("000000")
                .build();

        PasswordReset record = PasswordReset.builder()
                .id(1L)
                .email(email)
                .otpCode("849201")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .build();

        when(passwordResetRepository.findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(email))
                .thenReturn(Optional.of(record));

        AppException exception = assertThrows(AppException.class, () -> authService.verifyOtp(request));

        assertEquals(ErrorCode.OTP_INVALID, exception.getErrorCode());
    }

    // --- resetPassword ---

    @Test
    @DisplayName("TC-07: Đặt lại mật khẩu thành công và đánh dấu OTP đã sử dụng")
    void resetPassword_Success() {
        String email = "nguyenvana@gmail.com";
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email(email)
                .otpCode("849201")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build();

        User user = User.builder()
                .email(email)
                .passwordHash("oldHashedPassword")
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(1L);

        PasswordReset resetRecord = PasswordReset.builder()
                .id(1L)
                .email(email)
                .otpCode("849201")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordResetRepository.findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(email))
                .thenReturn(Optional.of(resetRecord));
        when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$10$newHashedPassword");

        authService.resetPassword(request);

        assertEquals("$2a$10$newHashedPassword", user.getPasswordHash());
        verify(userRepository, times(1)).save(user);

        assertTrue(resetRecord.getIsUsed());
        verify(passwordResetRepository, times(1)).save(resetRecord);
    }

    @Test
    @DisplayName("TC-08: Ném PASSWORD_CONFIRM_NOT_MATCH khi mật khẩu xác nhận không khớp")
    void resetPassword_PasswordConfirmNotMatch_ThrowsException() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("nguyenvana@gmail.com")
                .otpCode("849201")
                .newPassword("newPassword123")
                .confirmPassword("differentPassword")
                .build();

        AppException exception = assertThrows(AppException.class, () -> authService.resetPassword(request));

        assertEquals(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(passwordResetRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném USER_NOT_FOUND khi đặt lại mật khẩu cho email không tồn tại")
    void resetPassword_UserNotFound_ThrowsException() {
        String email = "unknown@gmail.com";
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email(email)
                .otpCode("849201")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.resetPassword(request));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(passwordResetRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném USER_LOCKED khi đặt lại mật khẩu cho tài khoản bị khóa")
    void resetPassword_UserLocked_ThrowsException() {
        String email = "locked@gmail.com";
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email(email)
                .otpCode("849201")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build();

        User user = User.builder()
                .email(email)
                .status(UserStatus.LOCKED)
                .build();
        user.setId(1L);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        AppException exception = assertThrows(AppException.class, () -> authService.resetPassword(request));

        assertEquals(ErrorCode.USER_LOCKED, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(passwordResetRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-09: Ném OTP_INVALID khi OTP không tồn tại hoặc đã bị sử dụng")
    void resetPassword_OtpNotFoundOrUsed_ThrowsException() {
        String email = "nguyenvana@gmail.com";
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email(email)
                .otpCode("849201")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build();

        User user = User.builder()
                .email(email)
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(1L);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordResetRepository.findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(email))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.resetPassword(request));

        assertEquals(ErrorCode.OTP_INVALID, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(passwordResetRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném OTP_EXPIRED khi mã OTP đã hết hạn tại bước reset password")
    void resetPassword_OtpExpired_ThrowsException() {
        String email = "nguyenvana@gmail.com";
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email(email)
                .otpCode("849201")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build();

        User user = User.builder()
                .email(email)
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(1L);

        PasswordReset expiredRecord = PasswordReset.builder()
                .id(1L)
                .email(email)
                .otpCode("849201")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .isUsed(false)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordResetRepository.findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(email))
                .thenReturn(Optional.of(expiredRecord));

        AppException exception = assertThrows(AppException.class, () -> authService.resetPassword(request));

        assertEquals(ErrorCode.OTP_EXPIRED, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(passwordResetRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ném OTP_INVALID khi mã OTP không khớp tại bước reset password")
    void resetPassword_WrongOtp_ThrowsException() {
        String email = "nguyenvana@gmail.com";
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email(email)
                .otpCode("999999")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build();

        User user = User.builder()
                .email(email)
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(1L);

        PasswordReset record = PasswordReset.builder()
                .id(1L)
                .email(email)
                .otpCode("849201")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordResetRepository.findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(email))
                .thenReturn(Optional.of(record));

        AppException exception = assertThrows(AppException.class, () -> authService.resetPassword(request));

        assertEquals(ErrorCode.OTP_INVALID, exception.getErrorCode());
        verify(userRepository, never()).save(any());
        verify(passwordResetRepository, never()).save(any());
    }
}

