package com.fpms.service.impl;

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
import com.fpms.service.AdminUserService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    @Override
    public PageResponse<UserResponse> getUsers(String keyword, RoleName role, UserStatus status, int page, int size) {
        log.info("Quản trị viên truy vấn danh sách người dùng - keyword: {}, role: {}, status: {}, page: {}, size: {}",
                keyword, role, status, page, size);

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Chỉ lấy những tài khoản chưa bị xóa mềm
            predicates.add(cb.isFalse(root.get("isDeleted")));

            // 2. Tìm kiếm theo từ khóa trong họ tên, email hoặc số điện thoại
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("fullName")), pattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);
                Predicate phoneMatch = cb.like(cb.lower(root.get("phoneNumber")), pattern);
                predicates.add(cb.or(nameMatch, emailMatch, phoneMatch));
            }

            // 3. Lọc theo vai trò
            if (role != null) {
                predicates.add(cb.equal(root.get("role").get("roleName"), role));
            }

            // 4. Lọc theo trạng thái
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<User> userPage = userRepository.findAll(spec, pageable);

        Page<UserResponse> dtoPage = userPage.map(userMapper::toUserResponse);
        return PageResponse.from(dtoPage);
    }

    @Override
    public UserResponse getUserById(Long id) {
        log.info("Quản trị viên xem chi tiết người dùng ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(UserPrincipal currentAdmin, Long id, UpdateUserStatusRequest request) {
        log.info("Quản trị viên (ID: {}) cập nhật trạng thái người dùng (ID: {}) -> {}",
                currentAdmin != null ? currentAdmin.getId() : null, id, request.getStatus());

        // Guard 1: Chặn Admin tự khóa tài khoản của chính mình
        if (currentAdmin != null && currentAdmin.getId().equals(id)) {
            throw new AppException(ErrorCode.CANNOT_LOCK_SELF);
        }

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Guard 2: Chặn khóa tài khoản của Quản trị viên khác
        if (targetUser.getRole() != null && targetUser.getRole().getRoleName() == RoleName.ROLE_ADMIN) {
            throw new AppException(ErrorCode.CANNOT_LOCK_ADMIN);
        }

        targetUser.setStatus(request.getStatus());
        User savedUser = userRepository.save(targetUser);
        log.info("Đã cập nhật trạng thái người dùng thành công: userId={}, newStatus={}", savedUser.getId(), savedUser.getStatus());

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUserRole(UserPrincipal currentAdmin, Long id, UpdateUserRoleRequest request) {
        log.info("Quản trị viên (ID: {}) cập nhật vai trò người dùng (ID: {}) -> {}",
                currentAdmin != null ? currentAdmin.getId() : null, id, request.getRole());

        // Guard 5: Chặn leo thang đặc quyền cố tình gán ROLE_ADMIN
        if (request.getRole() == null || request.getRole() == RoleName.ROLE_ADMIN) {
            throw new AppException(ErrorCode.INVALID_ROLE_ASSIGNMENT);
        }

        // Guard 3: Chặn Admin tự thay đổi vai trò của chính mình
        if (currentAdmin != null && currentAdmin.getId().equals(id)) {
            throw new AppException(ErrorCode.CANNOT_CHANGE_OWN_ROLE);
        }

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Guard 4: Chặn thay đổi vai trò của Quản trị viên khác
        if (targetUser.getRole() != null && targetUser.getRole().getRoleName() == RoleName.ROLE_ADMIN) {
            throw new AppException(ErrorCode.CANNOT_MODIFY_ADMIN);
        }

        Role newRole = roleRepository.findByRoleName(request.getRole())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        targetUser.setRole(newRole);
        User savedUser = userRepository.save(targetUser);
        log.info("Đã cập nhật vai trò người dùng thành công: userId={}, newRole={}", savedUser.getId(), savedUser.getRole().getRoleName());

        return userMapper.toUserResponse(savedUser);
    }
}
