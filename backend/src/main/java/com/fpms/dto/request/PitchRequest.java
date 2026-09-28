package com.fpms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class PitchRequest {

    @NotBlank(message = "Tên sân bóng không được để trống")
    @Size(max = 100, message = "Tên sân không được vượt quá 100 ký tự")
    private String name;

    @NotNull(message = "Vui lòng chọn loại sân bóng")
    private Long pitchTypeId;

    private String description;
}
