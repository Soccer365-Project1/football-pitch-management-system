package com.fpms.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class LoginRequest {

    @NotBlank(message = "Tài khoản (Email hoặc Số điện thoại) không được để trống")
    private String loginId;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
