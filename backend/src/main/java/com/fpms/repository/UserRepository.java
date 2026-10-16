package com.fpms.repository;

import com.fpms.entity.User;
import com.fpms.entity.enums.RoleName;
import com.fpms.entity.enums.UserStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    long countByIsDeletedFalse();

    long countByIsDeletedFalseAndRoleRoleName(RoleName roleName);

    long countByIsDeletedFalseAndStatus(UserStatus status);

    long countByIsDeletedFalseAndCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByIsDeletedFalseAndRoleRoleNameAndCreatedAtBetween(RoleName roleName, LocalDateTime start, LocalDateTime end);
}
