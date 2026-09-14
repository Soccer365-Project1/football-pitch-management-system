package com.fpms.service.impl;

import com.fpms.dto.request.ForgotPasswordRequest;
import com.fpms.dto.request.GoogleLoginRequest;
import com.fpms.dto.request.LoginRequest;
import com.fpms.dto.request.RegisterRequest;
import com.fpms.dto.request.ResetPasswordRequest;
import com.fpms.dto.request.VerifyOtpRequest;
import com.fpms.dto.response.AuthResponse;
import com.fpms.dto.response.UserResponse;
import com.fpms.entity.PasswordReset;
import com.fpms.entity.Role;
import com.fpms.entity.User;
import com.fpms.entity.enums.AuthProvider;
import com.fpms.entity.enums.RoleName;
import com.fpms.entity.enums.UserStatus;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.mapper.UserMapper;
import com.fpms.repository.PasswordResetRepository;
import com.fpms.repository.RoleRepository;
import com.fpms.repository.UserRepository;
import com.fpms.security.GoogleTokenVerifier;
import com.fpms.security.JwtTokenProvider;
import com.fpms.security.UserPrincipal;
import com.fpms.service.AuthService;
import com.fpms.service.EmailService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // 1. Kiểm tra mật khẩu xác nhận
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH);
        }

        String email = request.getEmail().trim().toLowerCase();
        String phoneNumber = request.getPhoneNumber().trim();

        // 2. Kiểm tra email trùng lặp
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 3. Kiểm tra số điện thoại trùng lặp
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        // 4. Lấy vai trò mặc định ROLE_CUSTOMER
        Role customerRole = roleRepository.findByRoleName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        // 5. Khởi tạo User qua UserMapper và thiết lập các trường hệ thống
        User user = userMapper.toUser(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setRole(customerRole);

        userRepository.save(user);
        log.info("Đăng ký tài khoản người dùng thành công: email={}, phone={}", email, phoneNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String loginId = request.getLoginId().trim();

        // 1. Nhận diện email hoặc số điện thoại để tìm kiếm người dùng
        User user = (loginId.contains("@")
                ? userRepository.findByEmail(loginId.toLowerCase())
                : userRepository.findByPhoneNumber(loginId))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        // 2. So khớp mật khẩu với BCrypt
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 3. Kiểm tra trạng thái tài khoản
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        // 4. Sinh cặp Token JWT (Access Token 1h, Refresh Token 7d)
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        // 5. Ánh xạ UserResponse và đóng gói AuthResponse
        UserResponse userResponse = userMapper.toUserResponse(user);

        log.info("Đăng nhập thành công cho tài khoản: userId={}, email={}", user.getId(), user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        // 1. Xác thực Google ID Token và giải mã payload
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(request.getIdToken());
        String email = payload.getEmail();
        String fullName = (String) payload.get("name");
        String pictureUrl = (String) payload.get("picture");

        if (email == null || email.isBlank()) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        String normalizedEmail = email.trim().toLowerCase();

        // 2. Tìm hoặc tự động tạo mới tài khoản nếu chưa tồn tại
        User user = userRepository.findByEmail(normalizedEmail).orElseGet(() -> {
            Role customerRole = roleRepository.findByRoleName(RoleName.ROLE_CUSTOMER)
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

            User newUser = User.builder()
                    .email(normalizedEmail)
                    .fullName(fullName != null && !fullName.isBlank() ? fullName : "Người dùng Google")
                    .avatarUrl(pictureUrl)
                    .status(UserStatus.ACTIVE)
                    .authProvider(AuthProvider.GOOGLE)
                    .role(customerRole)
                    .build();

            log.info("Tạo mới tài khoản qua Google OAuth: email={}", normalizedEmail);
            return userRepository.save(newUser);
        });

        // 3. Kiểm tra trạng thái tài khoản
        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        // 4. Cập nhật ảnh đại diện nếu Google cung cấp ảnh mới
        if (pictureUrl != null && !pictureUrl.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(pictureUrl);
            userRepository.save(user);
        }

        // 5. Sinh cặp Token JWT (Access Token 1h, Refresh Token 7d)
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        UserResponse userResponse = userMapper.toUserResponse(user);

        log.info("Đăng nhập bằng Google thành công: userId={}, email={}", user.getId(), user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserPrincipal userPrincipal) {
        if (userPrincipal == null || userPrincipal.getId() == null) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // 1. Kiểm tra tài khoản người dùng
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        // 2. Kiểm tra Cooldown 60 giây chống spam gửi mã liên tục
        Optional<PasswordReset> latestOtp = passwordResetRepository
                .findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(email);

        if (latestOtp.isPresent()) {
            LocalDateTime cooldownUntil = latestOtp.get().getCreatedAt().plusSeconds(60);
            if (LocalDateTime.now().isBefore(cooldownUntil)) {
                throw new AppException(ErrorCode.OTP_COOLDOWN_ACTIVE);
            }
        }

        // 3. Vô hiệu hóa tất cả các OTP cũ chưa sử dụng của email này
        List<PasswordReset> oldOtps = passwordResetRepository.findByEmailAndIsUsedFalse(email);
        for (PasswordReset old : oldOtps) {
            old.setIsUsed(true);
        }
        passwordResetRepository.saveAll(oldOtps);

        // 4. Sinh mã OTP 6 số ngẫu nhiên an toàn bằng SecureRandom (100000 - 999999)
        String otpCode = String.format("%06d", secureRandom.nextInt(900000) + 100000);

        // 5. Lưu bản ghi OTP mới vào CSDL (Thời hạn 10 phút)
        PasswordReset passwordReset = PasswordReset.builder()
                .email(email)
                .otpCode(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .isUsed(false)
                .createdAt(LocalDateTime.now())
                .build();
        passwordResetRepository.save(passwordReset);

        // 6. Gửi email bất đồng bộ qua EmailService (SendGrid hoặc Dev Console log)
        emailService.sendOtpEmail(email, otpCode, 10);
        log.info("Đã tạo mã OTP và gửi email đặt lại mật khẩu cho tài khoản: email={}", email);
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyOtp(VerifyOtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String otpCode = request.getOtpCode().trim();

        // 1. Tìm bản ghi OTP mới nhất chưa sử dụng
        PasswordReset resetRecord = passwordResetRepository
                .findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new AppException(ErrorCode.OTP_INVALID));

        // 2. Kiểm tra hết hạn (quá 10 phút)
        if (resetRecord.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        // 3. So khớp mã OTP
        if (!resetRecord.getOtpCode().equals(otpCode)) {
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        log.info("Xác thực mã OTP thành công cho email: {}", email);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String otpCode = request.getOtpCode().trim();

        // 1. Kiểm tra mật khẩu xác nhận
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH);
        }

        // 2. Tìm người dùng
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        // 3. Xác thực OTP
        PasswordReset resetRecord = passwordResetRepository
                .findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new AppException(ErrorCode.OTP_INVALID));

        if (resetRecord.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        if (!resetRecord.getOtpCode().equals(otpCode)) {
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        // 4. Băm mật khẩu mới bằng BCrypt và cập nhật user
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 5. Đánh dấu mã OTP đã được sử dụng
        resetRecord.setIsUsed(true);
        passwordResetRepository.save(resetRecord);

        log.info("Đặt lại mật khẩu thành công cho tài khoản: userId={}, email={}", user.getId(), email);
    }
}
