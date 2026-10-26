package com.fpms.repository;

import com.fpms.entity.PaymentMethod;
import com.fpms.entity.enums.PaymentMethodCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    Optional<PaymentMethod> findByCode(PaymentMethodCode code);
    boolean existsByCode(PaymentMethodCode code);
}
