package com.fpms.entity;

import com.fpms.entity.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", length = 50, nullable = false, unique = true)
    private RoleName roleName;

    @Column(name = "description", length = 255)
    private String description;
}
