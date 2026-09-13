package com.fpms.service.impl;

import com.fpms.dto.request.GoogleLoginRequest;
import com.fpms.dto.request.LoginRequest;
import com.fpms.dto.request.RegisterRequest;
import com.fpms.dto.response.AuthResponse;
import com.fpms.dto.response.UserResponse;
import com.fpms.entity.Role;
import com.fpms.entity.User;
import com.fpms.entity.enums.AuthProvider;
import com.fpms.entity.enums.RoleName;
import com.fpms.entity.enums.UserStatus;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.mapper.UserMapper;
import com.fpms.repository.RoleRepository;
import com.fpms.repository.UserRepository;
import com.fpms.security.GoogleTokenVerifier;
import com.fpms.security.JwtTokenProvider;
import com.fpms.security.UserPrincipal;
import com.fpms.service.AuthService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final GoogleTokenVerifier googleTokenVerifier;

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
}
