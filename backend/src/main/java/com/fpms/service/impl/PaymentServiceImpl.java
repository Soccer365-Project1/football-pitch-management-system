package com.fpms.service.impl;

import com.fpms.config.VNPayConfig;
import com.fpms.dto.response.BookingEventDto;
import com.fpms.dto.response.PaymentReturnResponse;
import com.fpms.entity.Booking;
import com.fpms.entity.PaymentTransaction;
import com.fpms.entity.enums.BookingStatus;
import com.fpms.entity.enums.TransactionStatus;
import com.fpms.repository.BookingRepository;
import com.fpms.repository.PaymentTransactionRepository;
import com.fpms.service.PaymentService;
import com.fpms.util.VNPayUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final VNPayConfig vnPayConfig;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final BookingRepository bookingRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public PaymentReturnResponse verifyAndProcessReturn(Map<String, String> queryParams) {
        String vnpSecureHash = queryParams.get("vnp_SecureHash");
        Map<String, String> cleanParams = new HashMap<>(queryParams);
        cleanParams.remove("vnp_SecureHash");
        cleanParams.remove("vnp_SecureHashType");

        String hashData = VNPayUtil.buildHashData(cleanParams);
        String calculatedHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);

        if (vnpSecureHash == null || !calculatedHash.equalsIgnoreCase(vnpSecureHash)) {
            log.warn("VNPay Return: Chữ ký không hợp lệ. Expected={}, Received={}", calculatedHash, vnpSecureHash);
            return PaymentReturnResponse.builder()
                    .status("INVALID_SIGNATURE")
                    .message("Chữ ký giao dịch không hợp lệ")
                    .build();
        }

        String responseCode = queryParams.get("vnp_ResponseCode");
        String txnRef = queryParams.get("vnp_TxnRef");
        String transactionNo = queryParams.get("vnp_TransactionNo");
        String bankCode = queryParams.get("vnp_BankCode");
        String payDate = queryParams.get("vnp_PayDate");
        String amountStr = queryParams.get("vnp_Amount");

        BigDecimal amount = BigDecimal.ZERO;
        if (amountStr != null) {
            try {
                amount = BigDecimal.valueOf(Long.parseLong(amountStr) / 100);
            } catch (NumberFormatException ignored) {}
        }

        PaymentTransaction transaction = paymentTransactionRepository.findByTransactionCode(txnRef).orElse(null);
        if (transaction == null) {
            log.warn("VNPay Return: Không tìm thấy giao dịch với txnRef={}", txnRef);
            return PaymentReturnResponse.builder()
                    .status("FAILED")
                    .message("Không tìm thấy thông tin giao dịch tương ứng")
                    .build();
        }

        Booking booking = transaction.getBooking();

        if ("00".equals(responseCode)) {
            // Nếu giao dịch chưa được cập nhật thành công (ví dụ IPN chưa tới hoặc chạy localhost)
            if (transaction.getStatus() == TransactionStatus.PENDING) {
                transaction.setStatus(TransactionStatus.SUCCESS);
                transaction.setReferenceCode(transactionNo);
                paymentTransactionRepository.save(transaction);

                if (booking != null && booking.getStatus() == BookingStatus.PENDING_HOLD) {
                    booking.setStatus(BookingStatus.WAITING_APPROVAL);
                    booking.setHoldExpiresAt(null);
                    bookingRepository.save(booking);
                    notifyBookingUpdated(booking);
                }
            }

            return PaymentReturnResponse.builder()
                    .status("SUCCESS")
                    .bookingCode(booking != null ? booking.getBookingCode() : null)
                    .bookingId(booking != null ? booking.getId() : null)
                    .amount(amount)
                    .transactionNo(transactionNo)
                    .bankCode(bankCode)
                    .payDate(payDate)
                    .message("Đặt cọc thành công! Khung giờ của bạn đã được đảm bảo.")
                    .build();
        } else if ("24".equals(responseCode)) {
            if (transaction.getStatus() == TransactionStatus.PENDING) {
                transaction.setStatus(TransactionStatus.FAILED);
                paymentTransactionRepository.save(transaction);
            }

            boolean isHoldExpired = booking == null || booking.getHoldExpiresAt() == null || !booking.getHoldExpiresAt().isAfter(LocalDateTime.now().plusMinutes(2));

            if (booking != null && booking.getStatus() == BookingStatus.PENDING_HOLD) {
                if (isHoldExpired) {
                    booking.setStatus(BookingStatus.CANCELLED);
                    booking.setHoldExpiresAt(null);
                    bookingRepository.save(booking);
                    notifyBookingUpdated(booking);
                    log.info("VNPay Return: Người dùng hủy thanh toán đơn {}, thời gian giữ sân còn dưới 2 phút -> CANCELLED và giải phóng giữ sân", booking.getBookingCode());
                } else {
                    log.info("VNPay Return: Người dùng hủy thanh toán đơn {}, nhưng thời gian giữ sân còn trên 2 phút (đến {}), giữ nguyên PENDING_HOLD để thanh toán lại",
                            booking.getBookingCode(), booking.getHoldExpiresAt());
                }
            }

            return PaymentReturnResponse.builder()
                    .status(isHoldExpired ? "CANCELLED" : "PENDING_HOLD")
                    .bookingCode(booking != null ? booking.getBookingCode() : null)
                    .bookingId(booking != null ? booking.getId() : null)
                    .amount(amount)
                    .message(isHoldExpired
                            ? "Bạn đã hủy giao dịch trên VNPAY hoặc thời gian giữ chỗ còn lại dưới 2 phút không đủ để thanh toán lại. Đơn đặt sân đã được hủy và giải phóng khung giờ."
                            : "Giao dịch thanh toán chưa hoàn tất. Khung giờ của bạn vẫn đang được tạm giữ, bạn có thể thanh toán lại.")
                    .build();
        } else {
            return PaymentReturnResponse.builder()
                    .status("FAILED")
                    .bookingCode(booking != null ? booking.getBookingCode() : null)
                    .bookingId(booking != null ? booking.getId() : null)
                    .amount(amount)
                    .message("Thanh toán tiền cọc không thành công (Mã lỗi: " + responseCode + ")")
                    .build();
        }
    }

    @Override
    @Transactional
    public Map<String, String> processIPN(Map<String, String> queryParams) {
        Map<String, String> response = new HashMap<>();
        try {
            String vnpSecureHash = queryParams.get("vnp_SecureHash");
            Map<String, String> cleanParams = new HashMap<>(queryParams);
            cleanParams.remove("vnp_SecureHash");
            cleanParams.remove("vnp_SecureHashType");

            String hashData = VNPayUtil.buildHashData(cleanParams);
            String calculatedHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);

            // 1. Kiểm tra chữ ký
            if (vnpSecureHash == null || !calculatedHash.equalsIgnoreCase(vnpSecureHash)) {
                log.warn("VNPay IPN: Invalid checksum");
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
                return response;
            }

            String txnRef = queryParams.get("vnp_TxnRef");
            PaymentTransaction transaction = paymentTransactionRepository.findByTransactionCode(txnRef).orElse(null);

            // 2. Kiểm tra giao dịch tồn tại
            if (transaction == null) {
                log.warn("VNPay IPN: Order not found for txnRef={}", txnRef);
                response.put("RspCode", "01");
                response.put("Message", "Order not Found");
                return response;
            }

            // 3. Kiểm tra số tiền (vnp_Amount / 100)
            long vnpAmount = Long.parseLong(queryParams.get("vnp_Amount")) / 100;
            if (transaction.getAmount().longValue() != vnpAmount) {
                log.warn("VNPay IPN: Invalid amount. Expected={}, Received={}", transaction.getAmount(), vnpAmount);
                response.put("RspCode", "04");
                response.put("Message", "Invalid Amount");
                return response;
            }

            // 4. Kiểm tra Idempotency
            if (transaction.getStatus() != TransactionStatus.PENDING) {
                response.put("RspCode", "02");
                response.put("Message", "Order already confirmed");
                return response;
            }

            // 5. Cập nhật dữ liệu
            String responseCode = queryParams.get("vnp_ResponseCode");
            String transactionNo = queryParams.get("vnp_TransactionNo");
            Booking booking = transaction.getBooking();

            if ("00".equals(responseCode)) {
                transaction.setStatus(TransactionStatus.SUCCESS);
                transaction.setReferenceCode(transactionNo);
                paymentTransactionRepository.save(transaction);

                if (booking != null) {
                    booking.setStatus(BookingStatus.WAITING_APPROVAL);
                    booking.setHoldExpiresAt(null);
                    bookingRepository.save(booking);
                    notifyBookingUpdated(booking);
                }
                log.info("VNPay IPN: Xác nhận đặt cọc thành công cho đơn {}", booking != null ? booking.getBookingCode() : txnRef);
            } else {
                transaction.setStatus(TransactionStatus.FAILED);
                paymentTransactionRepository.save(transaction);

                if ("24".equals(responseCode) && booking != null && booking.getStatus() == BookingStatus.PENDING_HOLD) {
                    boolean isHoldExpired = booking.getHoldExpiresAt() == null || !booking.getHoldExpiresAt().isAfter(LocalDateTime.now().plusMinutes(2));
                    if (isHoldExpired) {
                        booking.setStatus(BookingStatus.CANCELLED);
                        booking.setHoldExpiresAt(null);
                        bookingRepository.save(booking);
                        notifyBookingUpdated(booking);
                        log.info("VNPay IPN: Đơn {} thời gian holding còn dưới 2 phút, chuyển sang CANCELLED và giải phóng giữ sân", booking.getBookingCode());
                    } else {
                        log.info("VNPay IPN: Khách hủy thanh toán đơn {}, thời gian holding còn trên 2 phút (đến {}), giữ nguyên PENDING_HOLD để thanh toán lại",
                                booking.getBookingCode(), booking.getHoldExpiresAt());
                    }
                } else {
                    log.info("VNPay IPN: Giao dịch thất bại mã lỗi={}", responseCode);
                }
            }

            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            return response;

        } catch (Exception e) {
            log.error("VNPay IPN: Lỗi khi xử lý IPN", e);
            response.put("RspCode", "99");
            response.put("Message", "Unknown Error");
            return response;
        }
    }

    private void notifyBookingUpdated(Booking booking) {
        BookingEventDto event = BookingEventDto.builder()
                .action("BOOKING_UPDATED")
                .pitchId(booking.getPitch().getId())
                .timeSlotId(booking.getTimeSlot().getId())
                .date(booking.getBookingDate().toString())
                .status(booking.getStatus().name())
                .build();

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        messagingTemplate.convertAndSend("/topic/schedule", event);
                    } catch (Exception ex) {
                        log.error("Không thể gửi websocket event cập nhật booking sau khi commit", ex);
                    }
                }
            });
        } else {
            try {
                messagingTemplate.convertAndSend("/topic/schedule", event);
            } catch (Exception ex) {
                log.error("Không thể gửi websocket event cập nhật booking", ex);
            }
        }
    }
}
