package com.fpms.service;

import com.fpms.common.response.PageResponse;
import com.fpms.dto.request.TransactionSearchRequest;
import com.fpms.dto.response.TransactionDetailResponse;
import com.fpms.dto.response.TransactionSummaryResponse;
import com.fpms.entity.*;
import com.fpms.entity.enums.*;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.mapper.PaymentTransactionMapper;
import com.fpms.repository.PaymentMethodRepository;
import com.fpms.repository.PaymentTransactionRepository;
import com.fpms.service.impl.PaymentTransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentTransactionServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private PaymentTransactionMapper paymentTransactionMapper;

    @InjectMocks
    private PaymentTransactionServiceImpl paymentTransactionService;

    private PaymentTransaction mockTxn;
    private Booking mockBooking;
    private PaymentMethod mockMethod;
    private User mockStaff;
    private User mockCustomer;
    private TransactionSummaryResponse mockSummary;
    private TransactionDetailResponse mockDetail;

    @BeforeEach
    void setUp() {
        mockCustomer = User.builder()
                .fullName("Nguyễn Khách Hàng")
                .phoneNumber("0988776655")
                .email("customer@fpms.vn")
                .build();
        mockCustomer.setId(10L);

        mockStaff = User.builder()
                .fullName("Trần Thu Ngân")
                .email("staff@fpms.vn")
                .build();
        mockStaff.setId(20L);

        mockMethod = PaymentMethod.builder()
                .code(PaymentMethodCode.VNPAY)
                .name("VNPAY")
                .isMaintenance(false)
                .build();
        mockMethod.setId(1L);

        mockBooking = Booking.builder()
                .bookingCode("#B1001")
                .customer(mockCustomer)
                .pitchNameSnapshot("Sân 5A")
                .bookingDate(LocalDate.of(2026, 11, 10))
                .startTimeSnapshot(LocalTime.of(17, 30))
                .endTimeSnapshot(LocalTime.of(19, 0))
                .status(BookingStatus.CONFIRMED)
                .totalPitchAmount(BigDecimal.valueOf(300000))
                .depositAmount(BigDecimal.valueOf(90000))
                .remainingAmount(BigDecimal.valueOf(210000))
                .additionalFee(BigDecimal.ZERO)
                .build();
        mockBooking.setId(100L);

        mockTxn = PaymentTransaction.builder()
                .id(1L)
                .transactionCode("TXN_123456_100")
                .booking(mockBooking)
                .paymentMethod(mockMethod)
                .amount(BigDecimal.valueOf(90000))
                .paymentType(PaymentType.DEPOSIT_30)
                .status(TransactionStatus.SUCCESS)
                .processedBy(mockStaff)
                .referenceCode("VNP_REF_999")
                .note("Khách cọc online")
                .createdAt(LocalDateTime.of(2026, 11, 1, 10, 0))
                .build();

        mockSummary = TransactionSummaryResponse.builder()
                .id(1L)
                .transactionCode("TXN_123456_100")
                .bookingId(100L)
                .amount(BigDecimal.valueOf(90000))
                .paymentMethodCode(PaymentMethodCode.VNPAY)
                .paymentMethodName("VNPAY")
                .paymentType(PaymentType.DEPOSIT_30)
                .status(TransactionStatus.SUCCESS)
                .createdAt(LocalDateTime.of(2026, 11, 1, 10, 0))
                .build();

        mockDetail = TransactionDetailResponse.builder()
                .id(1L)
                .transactionCode("TXN_123456_100")
                .amount(BigDecimal.valueOf(90000))
                .referenceCode("VNP_REF_999")
                .processedByName("Trần Thu Ngân")
                .processedByEmail("staff@fpms.vn")
                .bookingId(100L)
                .bookingCode("#B1001")
                .pitchName("Sân 5A")
                .bookingDate(LocalDate.of(2026, 11, 10))
                .timeSlot("17:30 - 19:00")
                .customerName("Nguyễn Khách Hàng")
                .customerPhone("0988776655")
                .build();
    }

    @Test
    @DisplayName("getAllTransactions - Thành công trả về danh sách phân trang")
    void getAllTransactions_Success() {
        TransactionSearchRequest request = TransactionSearchRequest.builder()
                .status(TransactionStatus.SUCCESS)
                .paymentType(PaymentType.DEPOSIT_30)
                .fromDate(LocalDate.of(2026, 11, 1))
                .toDate(LocalDate.of(2026, 11, 30))
                .build();

        Page<PaymentTransaction> page = new PageImpl<>(List.of(mockTxn));
        when(paymentTransactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);
        when(paymentTransactionMapper.toSummaryResponse(mockTxn)).thenReturn(mockSummary);

        PageResponse<TransactionSummaryResponse> result = paymentTransactionService.getAllTransactions(request);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        TransactionSummaryResponse item = result.getItems().get(0);
        assertEquals("TXN_123456_100", item.getTransactionCode());
        assertEquals(100L, item.getBookingId());
        assertEquals(BigDecimal.valueOf(90000), item.getAmount());
        assertEquals(PaymentMethodCode.VNPAY, item.getPaymentMethodCode());
        assertEquals(TransactionStatus.SUCCESS, item.getStatus());
        verify(paymentTransactionMapper, times(1)).toSummaryResponse(mockTxn);
    }

    @Test
    @DisplayName("getAllTransactions - Ném ngoại lệ khi fromDate > toDate")
    void getAllTransactions_InvalidDateRange_ThrowsException() {
        TransactionSearchRequest request = TransactionSearchRequest.builder()
                .fromDate(LocalDate.of(2026, 11, 30))
                .toDate(LocalDate.of(2026, 11, 1))
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                paymentTransactionService.getAllTransactions(request));

        assertEquals(ErrorCode.INVALID_DATE_RANGE, ex.getErrorCode());
        verify(paymentTransactionRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("getTransactionDetail - Thành công trả về đầy đủ thông tin giao dịch và đơn đặt")
    void getTransactionDetail_Success() {
        when(paymentTransactionRepository.findById(1L)).thenReturn(Optional.of(mockTxn));
        when(paymentTransactionMapper.toDetailResponse(mockTxn)).thenReturn(mockDetail);

        TransactionDetailResponse result = paymentTransactionService.getTransactionDetail(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("TXN_123456_100", result.getTransactionCode());
        assertEquals(BigDecimal.valueOf(90000), result.getAmount());
        assertEquals("VNP_REF_999", result.getReferenceCode());
        assertEquals("Trần Thu Ngân", result.getProcessedByName());
        assertEquals("staff@fpms.vn", result.getProcessedByEmail());
        assertEquals("#B1001", result.getBookingCode());
        assertEquals("Sân 5A", result.getPitchName());
        assertEquals(LocalDate.of(2026, 11, 10), result.getBookingDate());
        assertEquals("17:30 - 19:00", result.getTimeSlot());
        assertEquals("Nguyễn Khách Hàng", result.getCustomerName());
        assertEquals("0988776655", result.getCustomerPhone());
        verify(paymentTransactionMapper, times(1)).toDetailResponse(mockTxn);
    }

    @Test
    @DisplayName("getTransactionDetail - Ném ngoại lệ PAYMENT_TRANSACTION_NOT_FOUND khi ID không tồn tại")
    void getTransactionDetail_NotFound_ThrowsException() {
        when(paymentTransactionRepository.findById(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                paymentTransactionService.getTransactionDetail(999L));

        assertEquals(ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("exportTransactionsExcel - Thành công tạo mảng byte Excel khi có dữ liệu thỏa mãn")
    void exportTransactionsExcel_Success() {
        TransactionSearchRequest request = TransactionSearchRequest.builder()
                .status(TransactionStatus.SUCCESS)
                .fromDate(LocalDate.of(2026, 11, 1))
                .toDate(LocalDate.of(2026, 11, 30))
                .build();

        when(paymentTransactionRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(mockTxn));

        byte[] result = paymentTransactionService.exportTransactionsExcel(request);

        assertNotNull(result);
        assertTrue(result.length > 0);
        // Header bytes của file ZIP / OOXML (.xlsx) bắt đầu bằng 'PK' (0x50, 0x4B)
        assertEquals(0x50, result[0]);
        assertEquals(0x4B, result[1]);
    }

    @Test
    @DisplayName("exportTransactionsExcel - Ném ngoại lệ REPORT_DATA_EMPTY khi danh sách rỗng")
    void exportTransactionsExcel_Empty_ThrowsException() {
        TransactionSearchRequest request = TransactionSearchRequest.builder().build();

        when(paymentTransactionRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of());

        AppException ex = assertThrows(AppException.class, () ->
                paymentTransactionService.exportTransactionsExcel(request));

        assertEquals(ErrorCode.REPORT_DATA_EMPTY, ex.getErrorCode());
    }

    @Test
    @DisplayName("exportTransactionsExcel - Ném ngoại lệ INVALID_DATE_RANGE khi fromDate > toDate")
    void exportTransactionsExcel_InvalidDateRange_ThrowsException() {
        TransactionSearchRequest request = TransactionSearchRequest.builder()
                .fromDate(LocalDate.of(2026, 11, 30))
                .toDate(LocalDate.of(2026, 11, 1))
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                paymentTransactionService.exportTransactionsExcel(request));

        assertEquals(ErrorCode.INVALID_DATE_RANGE, ex.getErrorCode());
        verify(paymentTransactionRepository, never()).findAll(any(Specification.class), any(org.springframework.data.domain.Sort.class));
    }
}
