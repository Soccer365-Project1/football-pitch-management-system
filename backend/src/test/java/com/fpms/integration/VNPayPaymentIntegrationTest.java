package com.fpms.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fpms.config.VNPayConfig;
import com.fpms.dto.request.BookingCreationRequest;
import com.fpms.entity.*;
import com.fpms.entity.enums.*;
import com.fpms.repository.*;
import com.fpms.util.VNPayUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Integration Test: VNPay Payment & Booking Deposit E2E")
class VNPayPaymentIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PitchRepository pitchRepository;

    @Autowired
    private PitchTypeRepository pitchTypeRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private PriceMatrixRepository priceMatrixRepository;

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private VNPayConfig vnPayConfig;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private Long testPitchId;
    private Long testTimeSlotId;
    private LocalDate testBookingDate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // 1. Tạo loại sân & sân
        PitchType pitchType = pitchTypeRepository.findByNameIgnoreCaseAndIsDeletedFalse("Sân 5 test E2E")
                .orElseGet(() -> pitchTypeRepository.save(PitchType.builder()
                        .name("Sân 5 test E2E")
                        .playerCapacity(10)
                        .description("Sân test")
                        .build()));

        Pitch pitch = pitchRepository.save(Pitch.builder()
                .name("Sân 5A Test")
                .pitchType(pitchType)
                .status(PitchStatus.ACTIVE)
                .build());
        testPitchId = pitch.getId();

        // 2. Tạo khung giờ
        TimeSlot slot = timeSlotRepository.save(TimeSlot.builder()
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 30))
                .isPeakHour(false)
                .build());
        testTimeSlotId = slot.getId();

        // 3. Tạo bảng giá (giá 300.000 VNĐ -> cọc 30% là 90.000 VNĐ)
        priceMatrixRepository.save(PriceMatrix.builder()
                .pitchType(pitchType)
                .isPeakHour(false)
                .dayType(DayType.WEEKDAY)
                .price(BigDecimal.valueOf(300000))
                .build());

        // 4. Khởi tạo phương thức VNPAY
        if (!paymentMethodRepository.existsByCode(PaymentMethodCode.VNPAY)) {
            paymentMethodRepository.save(PaymentMethod.builder()
                    .code(PaymentMethodCode.VNPAY)
                    .name("Cổng VNPAY")
                    .isActive(true)
                    .isMaintenance(false)
                    .build());
        }

        testBookingDate = LocalDate.now().plusDays(2);
    }

    @Test
    @DisplayName("E2E Test: Đặt sân -> Tạo URL VNPay -> Giả lập IPN thành công (Mã 00) -> Chốt đơn WAITING_APPROVAL")
    void testBookingDeposit_And_IPNConfirmSuccess() throws Exception {
        // Bước 1: Gọi API khởi tạo đặt cọc 30%
        BookingCreationRequest request = new BookingCreationRequest();
        request.setPitchId(testPitchId);
        request.setTimeSlotId(testTimeSlotId);
        request.setBookingDate(testBookingDate);
        request.setGuestName("Nguyễn Văn Test");
        request.setGuestPhone("0909123456");

        MvcResult result = mockMvc.perform(post("/api/v1/bookings/deposit-checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.depositAmount").value(90000))
                .andExpect(jsonPath("$.data.paymentUrl").isString())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String bookingCode = root.path("data").path("bookingCode").asText();
        String paymentUrl = root.path("data").path("paymentUrl").asText();

        assertNotNull(bookingCode);
        assertTrue(paymentUrl.contains("vnp_Amount=9000000"), "Số tiền sang VNPay phải là 9.000.000");

        // Trích xuất txnRef từ URL
        String txnRef = paymentUrl.substring(paymentUrl.indexOf("vnp_TxnRef=") + 11);
        txnRef = txnRef.substring(0, txnRef.indexOf("&"));

        // Kiểm tra Database: Trạng thái ban đầu là PENDING_HOLD
        Booking booking = bookingRepository.findByBookingCode(bookingCode).orElseThrow();
        assertEquals(BookingStatus.PENDING_HOLD, booking.getStatus());
        assertNotNull(booking.getHoldExpiresAt(), "Phải có thời hạn giữ chỗ 10 phút");

        PaymentTransaction transaction = paymentTransactionRepository.findByTransactionCode(txnRef).orElseThrow();
        assertEquals(TransactionStatus.PENDING, transaction.getStatus());
        assertEquals(0, new BigDecimal("90000").compareTo(transaction.getAmount()));

        // Bước 2: Giả lập VNPAY gọi IPN Webhook Server-to-Server chốt đơn
        Map<String, String> ipnParams = new HashMap<>();
        ipnParams.put("vnp_TxnRef", txnRef);
        ipnParams.put("vnp_Amount", "9000000");
        ipnParams.put("vnp_ResponseCode", "00");
        ipnParams.put("vnp_TransactionNo", "VNP_E2E_888999");

        String hashData = VNPayUtil.buildHashData(ipnParams);
        String vnpSecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);

        mockMvc.perform(get("/api/v1/payments/vnpay/ipn")
                        .param("vnp_TxnRef", txnRef)
                        .param("vnp_Amount", "9000000")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TransactionNo", "VNP_E2E_888999")
                        .param("vnp_SecureHash", vnpSecureHash))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"))
                .andExpect(jsonPath("$.Message").value("Confirm Success"));

        // Bước 3: Kiểm tra Database sau khi chốt đơn thành công
        Booking updatedBooking = bookingRepository.findByBookingCode(bookingCode).orElseThrow();
        assertEquals(BookingStatus.WAITING_APPROVAL, updatedBooking.getStatus(), "Đơn phải chuyển sang WAITING_APPROVAL");
        assertNull(updatedBooking.getHoldExpiresAt(), "Thời hạn tạm giữ 10 phút phải được giải phóng");

        PaymentTransaction updatedTxn = paymentTransactionRepository.findByTransactionCode(txnRef).orElseThrow();
        assertEquals(TransactionStatus.SUCCESS, updatedTxn.getStatus());
        assertEquals("VNP_E2E_888999", updatedTxn.getReferenceCode());
    }

    @Test
    @DisplayName("E2E Test: Đặt sân -> Khách bấm Hủy trên VNPay (Mã 24) -> Trạng thái CANCELLED & giải phóng khung giờ")
    void testBookingDeposit_And_UserCancelled() throws Exception {
        // Bước 1: Tạo đơn cọc
        BookingCreationRequest request = new BookingCreationRequest();
        request.setPitchId(testPitchId);
        request.setTimeSlotId(testTimeSlotId);
        request.setBookingDate(testBookingDate);
        request.setGuestName("Khách Hủy Thử");
        request.setGuestPhone("0912345678");

        MvcResult result = mockMvc.perform(post("/api/v1/bookings/deposit-checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String bookingCode = root.path("data").path("bookingCode").asText();
        String paymentUrl = root.path("data").path("paymentUrl").asText();

        String txnRef = paymentUrl.substring(paymentUrl.indexOf("vnp_TxnRef=") + 11);
        txnRef = txnRef.substring(0, txnRef.indexOf("&"));

        // Bước 2: Giả lập Return URL khi khách bấm HỦY (Mã 24)
        Map<String, String> returnParams = new HashMap<>();
        returnParams.put("vnp_TxnRef", txnRef);
        returnParams.put("vnp_Amount", "9000000");
        returnParams.put("vnp_ResponseCode", "24");

        String hashData = VNPayUtil.buildHashData(returnParams);
        String vnpSecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);

        mockMvc.perform(get("/api/v1/payments/vnpay/payment-return")
                        .param("vnp_TxnRef", txnRef)
                        .param("vnp_Amount", "9000000")
                        .param("vnp_ResponseCode", "24")
                        .param("vnp_SecureHash", vnpSecureHash))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.bookingCode").value(bookingCode));

        // Bước 3: Kiểm tra Database -> Đơn bị CANCELLED và holdExpiresAt = null
        Booking cancelledBooking = bookingRepository.findByBookingCode(bookingCode).orElseThrow();
        assertEquals(BookingStatus.CANCELLED, cancelledBooking.getStatus());
        assertNull(cancelledBooking.getHoldExpiresAt());

        PaymentTransaction failedTxn = paymentTransactionRepository.findByTransactionCode(txnRef).orElseThrow();
        assertEquals(TransactionStatus.FAILED, failedTxn.getStatus());

        // Bước 4: Kiểm tra lại bảng lịch -> Khung giờ này KHÔNG còn bị chiếm giữ nữa
        mockMvc.perform(get("/api/v1/bookings/schedule-grid")
                        .param("date", testBookingDate.toString())
                        .param("pitchTypeId", cancelledBooking.getPitch().getPitchType().getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookings[?(@.bookingCode == '" + bookingCode + "')]").doesNotExist());
    }
}
