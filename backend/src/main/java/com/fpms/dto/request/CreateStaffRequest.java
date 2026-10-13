package com.fpms.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu tạo tài khoản nhân viên mới từ Quản trị viên")
public class CreateStaffRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự")
    @Schema(description = "Họ và tên nhân viên", example = "Nguyễn Thu Ngân")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[35789])[0-9]{8}$", message = "Số điện thoại không hợp lệ (phải gồm 10 chữ số và bắt đầu bằng 03, 05, 07, 08 hoặc 09)")
    @Schema(description = "Số điện thoại liên hệ", example = "0909090909")
    private String phoneNumber;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 150, message = "Email không được vượt quá 150 ký tự")
    @Schema(description = "Địa chỉ email nhân viên", example = "ngan.staff@fpms.com")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Pattern(
            regexp = "^(?=.{6,64}$)(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$",
            message = "Mật khẩu phải từ 6 đến 64 ký tự, bao gồm ít nhất 1 chữ cái, 1 chữ số, 1 ký tự đặc biệt và không chứa khoảng trắng"
    )
    @Schema(description = "Mật khẩu đăng nhập", example = "Password@123")
    private String password;

    @NotBlank(message = "Mật khẩu xác nhận không được để trống")
    @Schema(description = "Xác nhận lại mật khẩu", example = "Password@123")
    private String confirmPassword;
}
