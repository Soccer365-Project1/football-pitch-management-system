package com.fpms.entity;

import com.fpms.common.entity.BaseEntity;
import com.fpms.entity.enums.PitchStatus;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "pitches")
public class Pitch extends BaseEntity {

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pitch_type_id", nullable = false)
    private PitchType pitchType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private PitchStatus status = PitchStatus.ACTIVE;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
