package com.fpms.controller;

import com.fpms.common.response.ApiResponse;
import com.fpms.common.response.PageResponse;
import com.fpms.dto.request.UpdateUserRoleRequest;
import com.fpms.dto.request.UpdateUserStatusRequest;
import com.fpms.dto.response.UserResponse;
import com.fpms.entity.enums.RoleName;
import com.fpms.entity.enums.UserStatus;
import com.fpms.security.UserPrincipal;
import com.fpms.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "5. Admin User Management", description = "Các API quản trị người dùng (Xem danh sách, Khóa/Mở khóa, Cấp quyền Staff)")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "Lấy danh sách người dùng", description = "Tìm kiếm theo từ khóa (họ tên, email, sđt), lọc theo vai trò, trạng thái và phân trang")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<UserResponse> response = adminUserService.getUsers(keyword, role, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", response));
    }

    @Operation(summary = "Lấy chi tiết người dùng theo ID", description = "Truy xuất thông tin hồ sơ của một tài khoản người dùng cụ thể")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse response = adminUserService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", response));
    }

    @Operation(summary = "Khóa hoặc Mở khóa tài khoản người dùng", description = "Cập nhật trạng thái tài khoản thành ACTIVE hoặc LOCKED")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        UserResponse response = adminUserService.updateUserStatus(userPrincipal, id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái người dùng thành công", response));
    }

    @Operation(summary = "Cấp quyền Staff hoặc Thu hồi quyền Staff", description = "Chuyển đổi vai trò người dùng giữa ROLE_CUSTOMER và ROLE_STAFF")
    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        UserResponse response = adminUserService.updateUserRole(userPrincipal, id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật vai trò người dùng thành công", response));
    }
}
