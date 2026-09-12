package com.fpms.repository;

import com.fpms.entity.Role;
import com.fpms.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleName(RoleName roleName);

    @Query("SELECT r FROM Role r WHERE r.roleName = :name")
    Optional<Role> findByName(@Param("name") RoleName name);
}
