package com.fpms.dto.request;

import com.fpms.entity.enums.RoleName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu cập nhật vai trò người dùng (Cấp / Thu hồi quyền Staff)")
public class UpdateUserRoleRequest {

    @NotNull(message = "Vai trò người dùng không được để trống")
    @Schema(description = "Vai trò mới cần gán (ROLE_CUSTOMER hoặc ROLE_STAFF)", example = "ROLE_STAFF")
    private RoleName role;
}
