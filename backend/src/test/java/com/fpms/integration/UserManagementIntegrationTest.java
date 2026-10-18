package com.fpms.integration;

import com.fpms.dto.request.UpdateUserStatusRequest;
import com.fpms.entity.Role;
import com.fpms.entity.User;
import com.fpms.entity.enums.AuthProvider;
import com.fpms.entity.enums.RoleName;
import com.fpms.entity.enums.UserStatus;
import com.fpms.exception.ErrorCode;
import com.fpms.repository.RoleRepository;
import com.fpms.repository.UserRepository;
import com.fpms.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bộ kiểm thử tích hợp tinh gọn cho Phân hệ Quản trị Người dùng (Subtask ST-04 / FPMS-157).
 * Áp dụng nguyên lý Kim tự tháp kiểm thử (Test Pyramid): Không lặp lại 5 Guards đã có ở Unit Test,
 * tập trung 100% vào tích hợp hệ thống thực tế trên CSDL PostgreSQL (fpms_test):
 * 1. Luồng chặn Token ngay lập tức khi tài khoản bị Admin khóa (Token Invalidation on Lock).
 * 2. Tìm kiếm và lọc đa tiêu chí bằng JPA Specification trên CSDL thực tế.
 * 3. Ranh giới bảo mật phân quyền URL (RBAC Security Boundary).
 */
class UserManagementIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role customerRole;
    private Role adminRole;

    private User defaultAdmin;
    private User defaultCustomer;

    @BeforeEach
    void setUp() {
        customerRole = roleRepository.findByRoleName(RoleName.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleName(RoleName.ROLE_CUSTOMER)
                        .description("Khách hàng mặc định")
                        .build()));

        adminRole = roleRepository.findByRoleName(RoleName.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .roleName(RoleName.ROLE_ADMIN)
                        .description("Quản trị viên hệ thống")
                        .build()));

        defaultAdmin = createTestUser("admin_" + randomSuffix() + "@fpms.com",
                randomPhone(), "Quản Trị Viên Chính", adminRole, UserStatus.ACTIVE);

        defaultCustomer = createTestUser("customer_" + randomSuffix() + "@fpms.com",
                randomPhone(), "Nguyễn Văn Khách", customerRole, UserStatus.ACTIVE);
    }

    private User createTestUser(String email, String phone, String fullName, Role role, UserStatus status) {
        User user = User.builder()
                .email(email)
                .phoneNumber(phone)
                .fullName(fullName)
                .passwordHash(passwordEncoder.encode("Password@123"))
                .status(status)
                .authProvider(AuthProvider.LOCAL)
                .role(role)
                .build();
        user.setIsDeleted(false);
        return userRepository.save(user);
    }

    private String tokenOf(User user) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(user);
    }

    private String randomSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String randomPhone() {
        return "09" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode() % 100000000));
    }

    // =========================================================================================
    // NHÓM 1: KIỂM THỬ TRỌNG YẾU - CHẶN TOKEN KHI KHÓA TÀI KHOẢN (TOKEN INVALIDATION ON LOCK)
    // =========================================================================================
    @Nested
    @DisplayName("1. Kiểm thử Kịch bản Trọng yếu: Chặn Token Khi Khóa Tài Khoản")
    class TokenInvalidationOnLockTests {

        @Test
        @DisplayName("TC_LOCK_01: Khách hàng ACTIVE truy cập tài nguyên bảo mật thành công")
        void testActiveUser_CanAccessProtectedProfile() throws Exception {
            mockMvc.perform(get("/api/v1/users/me")
                            .header("Authorization", tokenOf(defaultCustomer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.email").value(defaultCustomer.getEmail()))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("TC_LOCK_02: Admin gọi API khóa tài khoản người dùng thành công")
        void testAdmin_CanLockUserAccount() throws Exception {
            UpdateUserStatusRequest request = UpdateUserStatusRequest.builder()
                    .status(UserStatus.LOCKED)
                    .build();

            mockMvc.perform(patch("/api/v1/admin/users/" + defaultCustomer.getId() + "/status")
                            .header("Authorization", tokenOf(defaultAdmin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("LOCKED"));

            User updated = userRepository.findById(defaultCustomer.getId()).orElseThrow();
            assertEquals(UserStatus.LOCKED, updated.getStatus());
        }

        @Test
        @DisplayName("TC_LOCK_03: [CORE SECURITY] User đang đăng nhập bị Admin khóa -> Request tiếp theo bằng token cũ bị chặn 401")
        void testLockedUser_OldTokenImmediatelyBlocked_Returns401() throws Exception {
            String customerToken = tokenOf(defaultCustomer);

            // Request đầu tiên khi ACTIVE -> 200 OK
            mockMvc.perform(get("/api/v1/users/me")
                            .header("Authorization", customerToken))
                    .andExpect(status().isOk());

            // Admin khóa tài khoản
            UpdateUserStatusRequest lockRequest = UpdateUserStatusRequest.builder()
                    .status(UserStatus.LOCKED)
                    .build();

            mockMvc.perform(patch("/api/v1/admin/users/" + defaultCustomer.getId() + "/status")
                            .header("Authorization", tokenOf(defaultAdmin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(lockRequest)))
                    .andExpect(status().isOk());

            // Khách hàng tiếp tục gửi request bằng đúng token cũ -> Bị chặn ngay lập tức (401 UNAUTHORIZED)
            mockMvc.perform(get("/api/v1/users/me")
                            .header("Authorization", customerToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_TOKEN.getCode()));
        }

        @Test
        @DisplayName("TC_LOCK_04: Admin mở khóa tài khoản người dùng -> Khôi phục quyền truy cập bình thường")
        void testAdmin_CanUnlockUser_AndUserCanAccessAgain() throws Exception {
            // Khóa tài khoản
            defaultCustomer.setStatus(UserStatus.LOCKED);
            userRepository.save(defaultCustomer);

            // Admin gọi API mở khóa
            UpdateUserStatusRequest unlockRequest = UpdateUserStatusRequest.builder()
                    .status(UserStatus.ACTIVE)
                    .build();

            mockMvc.perform(patch("/api/v1/admin/users/" + defaultCustomer.getId() + "/status")
                            .header("Authorization", tokenOf(defaultAdmin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(unlockRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));

            // Khách hàng gửi request -> Thành công 200 OK
            mockMvc.perform(get("/api/v1/users/me")
                            .header("Authorization", tokenOf(defaultCustomer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.email").value(defaultCustomer.getEmail()));
        }
    }

    // =========================================================================================
    // NHÓM 2: KIỂM THỬ TRUY VẤN ĐỘNG JPA SPECIFICATION & BỘ LỌC ĐA TIÊU CHÍ
    // =========================================================================================
    @Nested
    @DisplayName("2. Kiểm thử JPA Specification: Tìm Kiếm, Lọc Đa Tiêu Chí & Phân Trang")
    class JpaSpecificationFilterTests {

        @Test
        @DisplayName("TC_SPEC_01: Tìm kiếm theo từ khóa keyword (họ tên/email/phone) không phân biệt hoa thường")
        void testSearch_ByKeyword_CaseInsensitive() throws Exception {
            String uniqueEmail = "dangvanlam_" + randomSuffix() + "@fpms.com";
            createTestUser(uniqueEmail, randomPhone(), "Đặng Văn Lâm Thủ Môn", customerRole, UserStatus.ACTIVE);

            // 1. Tìm theo họ tên
            mockMvc.perform(get("/api/v1/admin/users")
                            .header("Authorization", tokenOf(defaultAdmin))
                            .param("keyword", "đặng văn lâm"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$.data.items[0].fullName", containsString("Đặng Văn Lâm")));

            // 2. Tìm theo email
            mockMvc.perform(get("/api/v1/admin/users")
                            .header("Authorization", tokenOf(defaultAdmin))
                            .param("keyword", uniqueEmail))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(1)))
                    .andExpect(jsonPath("$.data.items[0].email").value(uniqueEmail));
        }

        @Test
        @DisplayName("TC_SPEC_02: Lọc kết hợp vai trò, trạng thái & phân trang (page, size, sắp xếp createdAt DESC)")
        void testFilter_CombinedRoleStatusAndPagination() throws Exception {
            createTestUser("spec_active1_" + randomSuffix() + "@fpms.com",
                    randomPhone(), "Khách Hàng Spec 1", customerRole, UserStatus.ACTIVE);
            createTestUser("spec_active2_" + randomSuffix() + "@fpms.com",
                    randomPhone(), "Khách Hàng Spec 2", customerRole, UserStatus.ACTIVE);

            mockMvc.perform(get("/api/v1/admin/users")
                            .header("Authorization", tokenOf(defaultAdmin))
                            .param("role", "ROLE_CUSTOMER")
                            .param("status", "ACTIVE")
                            .param("page", "1")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.pageSize").value(2))
                    .andExpect(jsonPath("$.data.items", hasSize(lessThanOrEqualTo(2))))
                    .andExpect(jsonPath("$.data.items[*].role", everyItem(equalTo("ROLE_CUSTOMER"))))
                    .andExpect(jsonPath("$.data.items[*].status", everyItem(equalTo("ACTIVE"))));
        }

        @Test
        @DisplayName("TC_SPEC_03: Tài khoản bị xóa mềm (isDeleted = true) không xuất hiện trong kết quả truy vấn")
        void testSoftDeletedUser_IsExcludedFromResults() throws Exception {
            String deletedEmail = "soft_deleted_" + randomSuffix() + "@fpms.com";
            User deletedUser = createTestUser(deletedEmail, randomPhone(), "User Đã Xóa Mềm", customerRole, UserStatus.ACTIVE);
            deletedUser.setIsDeleted(true);
            userRepository.save(deletedUser);

            mockMvc.perform(get("/api/v1/admin/users")
                            .header("Authorization", tokenOf(defaultAdmin))
                            .param("keyword", deletedEmail))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(0)));
        }
    }

    // =========================================================================================
    // NHÓM 3: KIỂM THỬ RANH GIỚI BẢO MẬT PHÂN QUYỀN URL (RBAC SECURITY BOUNDARY)
    // =========================================================================================
    @Nested
    @DisplayName("3. Kiểm thử Ranh Giới Bảo Mật URL")
    class RbacSecurityBoundaryTests {

        @Test
        @DisplayName("TC_SEC_01: Chặn truy cập trái phép vào /api/v1/admin/users (Ẩn danh -> 401, User thường -> 403)")
        void testUnauthorizedAccess_BlockedProperly() throws Exception {
            // 1. Không có token -> 401 UNAUTHORIZED
            mockMvc.perform(get("/api/v1/admin/users"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_TOKEN.getCode()));

            // 2. Token của ROLE_CUSTOMER -> 403 FORBIDDEN
            mockMvc.perform(get("/api/v1/admin/users")
                            .header("Authorization", tokenOf(defaultCustomer)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()));
        }
    }
}
