package com.fpms.config;

import com.fpms.entity.Role;
import com.fpms.entity.User;
import com.fpms.entity.enums.AuthProvider;
import com.fpms.entity.enums.RoleName;
import com.fpms.entity.enums.UserStatus;
import com.fpms.repository.RoleRepository;
import com.fpms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        initRoles();
        initDefaultAdmin();
    }

    private void initRoles() {
        createRoleIfNotFound(RoleName.ROLE_ADMIN, "Chủ sân - Toàn quyền quản trị");
        createRoleIfNotFound(RoleName.ROLE_STAFF, "Nhân viên - Vận hành ca, duyệt đơn, thu tiền");
        createRoleIfNotFound(RoleName.ROLE_CUSTOMER, "Khách hàng - Đặt sân và thanh toán");
    }

    private Role createRoleIfNotFound(RoleName roleName, String description) {
        return roleRepository.findByRoleName(roleName).orElseGet(() -> {
            Role role = Role.builder()
                    .roleName(roleName)
                    .description(description)
                    .build();
            Role savedRole = roleRepository.save(role);
            log.info("Khởi tạo vai trò mặc định: {}", roleName);
            return savedRole;
        });
    }

    private void initDefaultAdmin() {
        String adminEmail = "admin@soccer365.vn";
        String adminPhone = "0901234567";

        if (!userRepository.existsByEmail(adminEmail) && !userRepository.existsByPhoneNumber(adminPhone)) {
            Role adminRole = roleRepository.findByRoleName(RoleName.ROLE_ADMIN)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy vai trò ROLE_ADMIN khi tạo tài khoản Admin mặc định"));

            User admin = User.builder()
                    .email(adminEmail)
                    .phoneNumber(adminPhone)
                    .passwordHash(passwordEncoder.encode("123456"))
                    .fullName("Chủ Sân Admin")
                    .status(UserStatus.ACTIVE)
                    .authProvider(AuthProvider.LOCAL)
                    .role(adminRole)
                    .build();

            userRepository.save(admin);
            log.info("Khởi tạo tài khoản Admin mặc định thành công: Email={}, Mật khẩu mặc định=123456", adminEmail);
        }
    }
}
