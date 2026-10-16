package com.fpms.dto.response.report;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thống kê tổng quan người dùng hệ thống")
public class UserStatisticResponse {

    @Schema(description = "Tổng số người dùng (không bao gồm tài khoản bị xóa)", example = "250")
    private Long totalUsers;

    @Schema(description = "Tổng số khách hàng", example = "235")
    private Long totalCustomers;

    @Schema(description = "Tổng số nhân viên", example = "12")
    private Long totalStaffs;

    @Schema(description = "Số tài khoản đang bị khóa", example = "3")
    private Long totalLockedUsers;

    @Schema(description = "Số người dùng mới đăng ký trong tháng hiện tại", example = "45")
    private Long newUsersThisMonth;
}
