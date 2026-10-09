package com.fpms.dto.request;

import com.fpms.entity.enums.UserStatus;
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
@Schema(description = "Yêu cầu thay đổi trạng thái tài khoản người dùng")
public class UpdateUserStatusRequest {

    @NotNull(message = "Trạng thái người dùng không được để trống")
    @Schema(description = "Trạng thái tài khoản mới (ACTIVE hoặc LOCKED)", example = "LOCKED")
    private UserStatus status;
}
