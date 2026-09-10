package com.fpms.entity;

import com.fpms.common.entity.BaseEntity;
import com.fpms.entity.enums.PaymentMethodCode;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment_methods")
public class PaymentMethod extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "code", length = 30, nullable = false, unique = true)
    private PaymentMethodCode code;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "is_maintenance", nullable = false)
    @Builder.Default
    private Boolean isMaintenance = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "description", length = 255)
    private String description;
}
