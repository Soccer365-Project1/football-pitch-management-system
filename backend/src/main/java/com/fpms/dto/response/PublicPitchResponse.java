package com.fpms.dto.response;

import com.fpms.entity.enums.PitchStatus;
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
public class PublicPitchResponse {
    private Long id;
    private String name;
    private PitchTypeResponse pitchType;
    private PitchStatus status;
    private String description;
}
