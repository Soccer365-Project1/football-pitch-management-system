package com.fpms.dto.response;

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
public class PitchTypeResponse implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Integer playerCapacity;
    private String description;
}
