package com.fpms.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Yêu cầu cập nhật thông tin người dùng từ Quản trị viên")
public class UpdateUserRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ và tên phải từ 2 đến 100 ký tự")
    @Schema(description = "Họ và tên người dùng", example = "Nguyễn Hữu Giáp Nguyên")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[35789])[0-9]{8}$", message = "Số điện thoại không hợp lệ (phải gồm 10 chữ số và bắt đầu bằng 03, 05, 07, 08 hoặc 09)")
    @Schema(description = "Số điện thoại liên hệ", example = "0343243431")
    private String phoneNumber;
}
