package com.fpms.service;

import com.fpms.config.VNPayConfig;
import com.fpms.dto.response.PaymentReturnResponse;
import com.fpms.entity.Booking;
import com.fpms.entity.Pitch;
import com.fpms.entity.TimeSlot;
import com.fpms.entity.PaymentTransaction;
import com.fpms.entity.enums.BookingStatus;
import com.fpms.entity.enums.TransactionStatus;
import com.fpms.repository.BookingRepository;
import com.fpms.repository.PaymentTransactionRepository;
import com.fpms.service.impl.PaymentServiceImpl;
import com.fpms.util.VNPayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: PaymentServiceImpl (VNPay Return & IPN)")
class PaymentServiceImplTest {

    @Mock
    private VNPayConfig vnPayConfig;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private final String hashSecret = "SECRETKEY123456789";
    private Booking testBooking;
    private PaymentTransaction testTransaction;

    @BeforeEach
    void setUp() {
        lenient().when(vnPayConfig.getHashSecret()).thenReturn(hashSecret);

        Pitch pitch = new Pitch();
        pitch.setId(1L);

        TimeSlot slot = new TimeSlot();
        slot.setId(1L);

        testBooking = Booking.builder()
                .bookingCode("#B123456")
                .bookingDate(LocalDate.now())
                .status(BookingStatus.PENDING_HOLD)
                .depositAmount(new BigDecimal("90000"))
                .pitch(pitch)
                .timeSlot(slot)
                .build();
        testBooking.setId(10L);

        testTransaction = PaymentTransaction.builder()
                .transactionCode("TXN_123")
                .amount(new BigDecimal("90000"))
                .status(TransactionStatus.PENDING)
                .booking(testBooking)
                .build();
    }

    @Test
    @DisplayName("Return URL: Phát hiện chữ ký giả mạo / không hợp lệ")
    void testVerifyAndProcessReturn_InvalidChecksum() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "TXN_123");
        params.put("vnp_Amount", "9000000");
        params.put("vnp_SecureHash", "FAKE_SIGNATURE_9999");

        PaymentReturnResponse response = paymentService.verifyAndProcessReturn(params);

        assertEquals("INVALID_SIGNATURE", response.getStatus());
        verify(paymentTransactionRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Return URL: Thanh toán thành công (Mã 00) -> WAITING_APPROVAL & giải phóng hold")
    void testVerifyAndProcessReturn_Success_Code00() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "TXN_123");
        params.put("vnp_Amount", "9000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "VNPAY_998877");

        String hashData = VNPayUtil.buildHashData(params);
        String validHash = VNPayUtil.hmacSHA512(hashSecret, hashData);
        params.put("vnp_SecureHash", validHash);

        when(paymentTransactionRepository.findByTransactionCode("TXN_123"))
                .thenReturn(Optional.of(testTransaction));

        PaymentReturnResponse response = paymentService.verifyAndProcessReturn(params);

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("VNPAY_998877", response.getTransactionNo());
        assertEquals(TransactionStatus.SUCCESS, testTransaction.getStatus());
        assertEquals(BookingStatus.WAITING_APPROVAL, testBooking.getStatus());
        assertNull(testBooking.getHoldExpiresAt(), "Hạn 10 phút phải được xóa bỏ khi cọc xong");

        verify(paymentTransactionRepository).save(testTransaction);
        verify(bookingRepository).save(testBooking);
    }

    @Test
    @DisplayName("Return URL: Khách bấm Hủy (Mã 24) -> CANCELLED & giải phóng khung giờ ngay lập tức")
    void testVerifyAndProcessReturn_Cancelled_Code24() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "TXN_123");
        params.put("vnp_Amount", "9000000");
        params.put("vnp_ResponseCode", "24");

        String hashData = VNPayUtil.buildHashData(params);
        String validHash = VNPayUtil.hmacSHA512(hashSecret, hashData);
        params.put("vnp_SecureHash", validHash);

        when(paymentTransactionRepository.findByTransactionCode("TXN_123"))
                .thenReturn(Optional.of(testTransaction));

        PaymentReturnResponse response = paymentService.verifyAndProcessReturn(params);

        assertEquals("CANCELLED", response.getStatus());
        assertEquals(TransactionStatus.FAILED, testTransaction.getStatus());
        assertEquals(BookingStatus.CANCELLED, testBooking.getStatus());
        assertNull(testBooking.getHoldExpiresAt());

        verify(paymentTransactionRepository).save(testTransaction);
        verify(bookingRepository).save(testBooking);
    }

    @Test
    @DisplayName("IPN: Thanh toán thành công (Mã 00) -> Trả về RspCode 00 Confirm Success")
    void testProcessIPN_Success() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "TXN_123");
        params.put("vnp_Amount", "9000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "VNPAY_IPN_5544");

        String hashData = VNPayUtil.buildHashData(params);
        String validHash = VNPayUtil.hmacSHA512(hashSecret, hashData);
        params.put("vnp_SecureHash", validHash);

        when(paymentTransactionRepository.findByTransactionCode("TXN_123"))
                .thenReturn(Optional.of(testTransaction));

        Map<String, String> result = paymentService.processIPN(params);

        assertEquals("00", result.get("RspCode"));
        assertEquals("Confirm Success", result.get("Message"));
        assertEquals(TransactionStatus.SUCCESS, testTransaction.getStatus());
        assertEquals(BookingStatus.WAITING_APPROVAL, testBooking.getStatus());

        verify(paymentTransactionRepository).save(testTransaction);
        verify(bookingRepository).save(testBooking);
    }

    @Test
    @DisplayName("IPN: Số tiền thanh toán không khớp -> Trả về RspCode 04 Invalid Amount")
    void testProcessIPN_InvalidAmount() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "TXN_123");
        params.put("vnp_Amount", "5000000"); // 50.000 trong khi đơn yêu cầu 90.000
        params.put("vnp_ResponseCode", "00");

        String hashData = VNPayUtil.buildHashData(params);
        String validHash = VNPayUtil.hmacSHA512(hashSecret, hashData);
        params.put("vnp_SecureHash", validHash);

        when(paymentTransactionRepository.findByTransactionCode("TXN_123"))
                .thenReturn(Optional.of(testTransaction));

        Map<String, String> result = paymentService.processIPN(params);

        assertEquals("04", result.get("RspCode"));
        assertEquals("Invalid Amount", result.get("Message"));
    }

    @Test
    @DisplayName("IPN: Idempotency - Đơn đã được xác nhận trước đó -> Trả về RspCode 02 Order already confirmed")
    void testProcessIPN_AlreadyConfirmed() {
        testTransaction.setStatus(TransactionStatus.SUCCESS); // Đã thành công từ trước

        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "TXN_123");
        params.put("vnp_Amount", "9000000");
        params.put("vnp_ResponseCode", "00");

        String hashData = VNPayUtil.buildHashData(params);
        String validHash = VNPayUtil.hmacSHA512(hashSecret, hashData);
        params.put("vnp_SecureHash", validHash);

        when(paymentTransactionRepository.findByTransactionCode("TXN_123"))
                .thenReturn(Optional.of(testTransaction));

        Map<String, String> result = paymentService.processIPN(params);

        assertEquals("02", result.get("RspCode"));
        assertEquals("Order already confirmed", result.get("Message"));
        verify(bookingRepository, never()).save(any());
    }
}
