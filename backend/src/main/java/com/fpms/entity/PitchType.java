package com.fpms.entity;

import com.fpms.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "pitch_types")
public class PitchType extends BaseEntity {

    @Column(name = "name", length = 50, nullable = false, unique = true)
    private String name;

    @Column(name = "player_capacity", nullable = false)
    private Integer playerCapacity;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
