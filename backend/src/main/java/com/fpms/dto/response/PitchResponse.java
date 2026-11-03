package com.fpms.dto.response;

import com.fpms.entity.enums.PitchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PitchResponse implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private PitchTypeResponse pitchType;
    private PitchStatus status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
