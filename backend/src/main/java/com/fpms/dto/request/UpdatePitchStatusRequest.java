package com.fpms.dto.request;

import com.fpms.entity.enums.PitchStatus;
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
public class UpdatePitchStatusRequest {

    @NotNull(message = "Trạng thái sân không được để trống")
    private PitchStatus status;
}
