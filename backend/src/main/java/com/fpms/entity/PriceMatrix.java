package com.fpms.entity;

import com.fpms.common.entity.BaseEntity;
import com.fpms.entity.enums.DayType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "price_matrices",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_price_matrix", columnNames = {"pitch_type_id", "is_peak_hour", "day_type"})
    }
)
public class PriceMatrix extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pitch_type_id", nullable = false)
    private PitchType pitchType;

    @Column(name = "is_peak_hour", nullable = false)
    private Boolean isPeakHour;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type", length = 20, nullable = false)
    private DayType dayType;

    @Column(name = "price", precision = 12, scale = 2, nullable = false)
    private BigDecimal price;
}
