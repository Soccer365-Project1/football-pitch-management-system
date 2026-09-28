package com.fpms.repository;

import com.fpms.entity.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

    /**
     * Tìm bản ghi OTP mới nhất chưa sử dụng của email được chỉ định
     *
     * @param email địa chỉ email người dùng
     * @return Optional chứa PasswordReset mới nhất nếu có
     */
    Optional<PasswordReset> findTopByEmailAndIsUsedFalseOrderByCreatedAtDesc(String email);

    /**
     * Lấy toàn bộ danh sách các OTP chưa sử dụng của email để vô hiệu hóa
     *
     * @param email địa chỉ email người dùng
     * @return danh sách các PasswordReset chưa dùng
     */
    List<PasswordReset> findByEmailAndIsUsedFalse(String email);
}
