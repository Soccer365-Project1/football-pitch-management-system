package com.fpms.repository;

import com.fpms.entity.PaymentTransaction;
import com.fpms.entity.enums.PaymentType;
import com.fpms.entity.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long>, JpaSpecificationExecutor<PaymentTransaction> {
    Optional<PaymentTransaction> findByTransactionCode(String transactionCode);
    List<PaymentTransaction> findByBookingId(Long bookingId);
    List<PaymentTransaction> findByBookingIdOrderByCreatedAtDesc(Long bookingId);

    Optional<PaymentTransaction> findFirstByBookingIdAndPaymentTypeAndStatusOrderByCreatedAtDesc(
            Long bookingId,
            PaymentType paymentType,
            TransactionStatus status
    );

    @Query("SELECT pt.paymentMethod.code, COUNT(pt), COALESCE(SUM(pt.amount), 0) " +
           "FROM PaymentTransaction pt " +
           "WHERE pt.createdAt >= :startDateTime AND pt.createdAt <= :endDateTime " +
           "AND pt.status = :status " +
           "GROUP BY pt.paymentMethod.code")
    List<Object[]> aggregateTransactionsByPaymentMethodBetweenDates(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("status") TransactionStatus status);
}
