package com.fpms.service.impl;

import com.fpms.dto.request.ChangePasswordRequest;
import com.fpms.dto.request.UpdateProfileRequest;
import com.fpms.dto.response.UserResponse;
import com.fpms.entity.User;
import com.fpms.entity.enums.UserStatus;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.mapper.UserMapper;
import com.fpms.repository.UserRepository;
import com.fpms.security.UserPrincipal;
import com.fpms.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(UserPrincipal userPrincipal) {
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
    public UserResponse updateMyProfile(UserPrincipal userPrincipal, UpdateProfileRequest request) {
        if (userPrincipal == null || userPrincipal.getId() == null) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        String fullName = request.getFullName().trim();
        String phoneNumber = request.getPhoneNumber().trim();

        // Kiểm tra số điện thoại mới có bị trùng với tài khoản người dùng khác hay không
        if (userRepository.existsByPhoneNumberAndIdNot(phoneNumber, user.getId())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        user.setFullName(fullName);
        user.setPhoneNumber(phoneNumber);

        User updatedUser = userRepository.save(user);
        log.info("Đã cập nhật thông tin cá nhân thành công cho tài khoản: userId={}, phone={}", user.getId(), phoneNumber);

        return userMapper.toUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public void changePassword(UserPrincipal userPrincipal, ChangePasswordRequest request) {
        if (userPrincipal == null || userPrincipal.getId() == null) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        // 1. Kiểm tra mật khẩu mới và xác nhận mật khẩu khớp nhau
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH);
        }

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        // 2. So khớp mật khẩu hiện tại với mã băm BCrypt trong CSDL
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.OLD_PASSWORD_INCORRECT);
        }

        // 3. Kiểm tra mật khẩu mới không được trùng với mật khẩu hiện tại
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.NEW_PASSWORD_SAME_AS_OLD);
        }

        // 4. Băm mật khẩu mới bằng BCrypt và cập nhật người dùng
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Đã đổi mật khẩu tài khoản thành công: userId={}, email={}", user.getId(), user.getEmail());
    }
}
