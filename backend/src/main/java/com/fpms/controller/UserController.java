package com.fpms.controller;

import com.fpms.common.response.ApiResponse;
import com.fpms.dto.request.ChangePasswordRequest;
import com.fpms.dto.request.UpdateProfileRequest;
import com.fpms.dto.response.UserResponse;
import com.fpms.security.UserPrincipal;
import com.fpms.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "2. User Management", description = "Các API quản lý thông tin tài khoản người dùng, cập nhật hồ sơ cá nhân và đổi mật khẩu")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Lấy hồ sơ cá nhân", description = "Truy xuất toàn bộ thông tin hồ sơ của người dùng đang đăng nhập")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        UserResponse userResponse = userService.getMyProfile(userPrincipal);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hồ sơ cá nhân thành công", userResponse));
    }

    @Operation(summary = "Cập nhật hồ sơ cá nhân", description = "Cho phép người dùng cập nhật Họ và tên, Số điện thoại (Trường Email là cố định Read-only)")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserResponse updatedUser = userService.updateMyProfile(userPrincipal, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin cá nhân thành công", updatedUser));
    }

    @Operation(summary = "Đổi mật khẩu tài khoản", description = "Xác thực mật khẩu cũ và đổi sang mật khẩu mới")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(userPrincipal, request);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công. Vui lòng sử dụng mật khẩu mới cho lần đăng nhập sau", null));
    }
}
