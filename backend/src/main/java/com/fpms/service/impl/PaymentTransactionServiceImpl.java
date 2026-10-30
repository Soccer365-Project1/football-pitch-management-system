package com.fpms.service.impl;

import com.fpms.dto.request.TransactionRequest;
import com.fpms.entity.Booking;
import com.fpms.entity.PaymentMethod;
import com.fpms.entity.PaymentTransaction;
import com.fpms.entity.enums.TransactionStatus;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.repository.PaymentMethodRepository;
import com.fpms.repository.PaymentTransactionRepository;
import com.fpms.service.PaymentTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTransactionServiceImpl implements PaymentTransactionService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    @Override
    @Transactional
    public PaymentTransaction createTransaction(TransactionRequest request) {
        if (request == null || request.getBooking() == null) {
            throw new IllegalArgumentException("Thông tin giao dịch và đơn đặt sân không được để trống");
        }

        Booking booking = request.getBooking();

        // Ưu tiên dùng paymentMethod đã truyền vào, nếu chưa có thì tìm theo paymentMethodCode
        PaymentMethod paymentMethod = request.getPaymentMethod();
        if (paymentMethod == null && request.getPaymentMethodCode() != null) {
            paymentMethod = paymentMethodRepository.findByCode(request.getPaymentMethodCode())
                    .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_METHOD_NOT_FOUND));
        }

        String txnCode = "TXN_" + System.currentTimeMillis() + "_" + booking.getId();
        BigDecimal amount = request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO;
        TransactionStatus status = request.getStatus() != null ? request.getStatus() : TransactionStatus.PENDING;

        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionCode(txnCode)
                .booking(booking)
                .paymentMethod(paymentMethod)
                .amount(amount)
                .paymentType(request.getPaymentType())
                .status(status)
                .processedBy(request.getProcessedBy())
                .note(request.getNote())
                .createdAt(LocalDateTime.now())
                .build();

        PaymentTransaction saved = paymentTransactionRepository.save(transaction);
        log.info("Đã tạo giao dịch thanh toán {}: Mã={}, Booking={}, Số tiền={}, Phương thức={}, Trạng thái={}",
                request.getPaymentType(), txnCode, booking.getBookingCode(), amount,
                paymentMethod != null ? paymentMethod.getCode() : null, status);

        return saved;
    }
}
