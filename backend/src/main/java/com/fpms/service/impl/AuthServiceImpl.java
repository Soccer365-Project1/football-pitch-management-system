package com.fpms.service.impl;

import com.fpms.dto.request.RegisterRequest;
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
import com.fpms.service.AuthService;
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
}
