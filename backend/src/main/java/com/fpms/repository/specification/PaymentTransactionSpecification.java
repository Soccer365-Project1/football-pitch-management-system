package com.fpms.repository.specification;

import com.fpms.dto.request.TransactionSearchRequest;
import com.fpms.entity.Booking;
import com.fpms.entity.PaymentTransaction;
import com.fpms.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp xây dựng Specification động cho truy vấn tìm kiếm và lọc danh sách giao dịch thanh toán (PaymentTransaction).
 */
public class PaymentTransactionSpecification {

    private PaymentTransactionSpecification() {
        // Private constructor to prevent instantiation
    }

    public static Specification<PaymentTransaction> filterTransactions(TransactionSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            // 1. Lọc theo trạng thái giao dịch
            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }

            // 2. Lọc theo loại thanh toán (DEPOSIT_30, REMAINING_70, FULL_PAYMENT, REFUND)
            if (request.getPaymentType() != null) {
                predicates.add(cb.equal(root.get("paymentType"), request.getPaymentType()));
            }

            // 3. Lọc theo mã phương thức thanh toán (VNPAY, CASH, MOMO...)
            if (request.getPaymentMethodCode() != null) {
                predicates.add(cb.equal(root.get("paymentMethod").get("code"), request.getPaymentMethodCode()));
            }

            // 4. Lọc theo khoảng ngày tạo giao dịch (createdAt)
            if (request.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.getFromDate().atStartOfDay()));
            }
            if (request.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.getToDate().atTime(LocalTime.MAX)));
            }

            // 5. Lọc theo khoảng số tiền
            if (request.getMinAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), request.getMinAmount()));
            }
            if (request.getMaxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), request.getMaxAmount()));
            }

            // 6. Tìm kiếm theo từ khóa (keyword) kết hợp searchField
            String keyword = request.getCleanKeyword();
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                String field = request.getSearchField() != null ? request.getSearchField().toUpperCase().trim() : "ALL";

                Join<PaymentTransaction, Booking> bookingJoin = root.join("booking", JoinType.LEFT);
                Join<Booking, User> customerJoin = bookingJoin.join("customer", JoinType.LEFT);

                switch (field) {
                    case "TRANSACTION_CODE":
                        predicates.add(cb.like(cb.lower(root.get("transactionCode")), pattern));
                        break;
                    case "BOOKING_CODE":
                        predicates.add(cb.like(cb.lower(bookingJoin.get("bookingCode")), pattern));
                        break;
                    case "REFERENCE_CODE":
                        predicates.add(cb.like(cb.lower(root.get("referenceCode")), pattern));
                        break;
                    case "PHONE":
                        predicates.add(cb.or(
                                cb.like(cb.lower(customerJoin.get("phoneNumber")), pattern),
                                cb.like(cb.lower(bookingJoin.get("guestPhone")), pattern)
                        ));
                        break;
                    case "ALL":
                    default:
                        predicates.add(cb.or(
                                cb.like(cb.lower(root.get("transactionCode")), pattern),
                                cb.like(cb.lower(bookingJoin.get("bookingCode")), pattern),
                                cb.like(cb.lower(root.get("referenceCode")), pattern),
                                cb.like(cb.lower(customerJoin.get("phoneNumber")), pattern),
                                cb.like(cb.lower(bookingJoin.get("guestPhone")), pattern)
                        ));
                        break;
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

