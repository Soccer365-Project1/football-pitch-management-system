package com.fpms.service;

import com.fpms.common.response.PageResponse;
import com.fpms.dto.request.AdminBookingCancelRequest;
import com.fpms.dto.request.AdminBookingSearchRequest;
import com.fpms.dto.request.CollectPaymentRequest;
import com.fpms.dto.request.CounterBookingCreationRequest;
import com.fpms.dto.response.AdminBookingDetailResponse;
import com.fpms.dto.response.AdminBookingSummaryResponse;
import com.fpms.entity.*;
import com.fpms.entity.enums.*;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.mapper.AdminBookingMapper;
import com.fpms.repository.*;
import com.fpms.security.UserPrincipal;
import com.fpms.service.impl.AdminBookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private PitchRepository pitchRepository;
    @Mock
    private TimeSlotRepository timeSlotRepository;
    @Mock
    private HolidayRepository holidayRepository;
    @Mock
    private PriceMatrixRepository priceMatrixRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Spy
    private AdminBookingMapper adminBookingMapper = new AdminBookingMapper();

    @InjectMocks
    private AdminBookingServiceImpl adminBookingService;

    private Pitch pitch;
    private PitchType pitchType;
    private TimeSlot timeSlot;
    private PriceMatrix priceMatrix;
    private User staffUser;
    private User customerUser;
    private UserPrincipal staffPrincipal;
    private Booking booking;

    @BeforeEach
    void setUp() {
        pitchType = new PitchType();
        pitchType.setId(1L);
        pitchType.setName("Sân 5 người");

        pitch = new Pitch();
        pitch.setId(1L);
        pitch.setName("Sân A1");
        pitch.setPitchType(pitchType);
        pitch.setStatus(PitchStatus.ACTIVE);

        timeSlot = new TimeSlot();
        timeSlot.setId(1L);
        timeSlot.setStartTime(LocalTime.of(18, 0));
        timeSlot.setEndTime(LocalTime.of(19, 30));
        timeSlot.setIsPeakHour(true);

        priceMatrix = new PriceMatrix();
        priceMatrix.setId(1L);
        priceMatrix.setPitchType(pitchType);
        priceMatrix.setPrice(BigDecimal.valueOf(500000));
        priceMatrix.setIsPeakHour(true);

        Role staffRole = new Role();
        staffRole.setRoleName(RoleName.ROLE_STAFF);

        staffUser = new User();
        staffUser.setId(2L);
        staffUser.setFullName("Nguyễn Văn Nhân Viên");
        staffUser.setRole(staffRole);

        Role customerRole = new Role();
        customerRole.setRoleName(RoleName.ROLE_CUSTOMER);

        customerUser = new User();
        customerUser.setId(3L);
        customerUser.setFullName("Lê Văn Khách");
        customerUser.setPhoneNumber("0987654321");
        customerUser.setRole(customerRole);

        staffPrincipal = UserPrincipal.create(staffUser);

        booking = Booking.builder()
                .bookingCode("#B173000000000")
                .pitch(pitch)
                .timeSlot(timeSlot)
                .bookingDate(LocalDate.now().plusDays(1))
                .customer(customerUser)
                .guestName("Lê Văn Khách")
                .guestPhone("0987654321")
                .staff(staffUser)
                .priceSnapshot(BigDecimal.valueOf(500000))
                .startTimeSnapshot(timeSlot.getStartTime())
                .endTimeSnapshot(timeSlot.getEndTime())
                .pitchNameSnapshot(pitch.getName())
                .totalPitchAmount(BigDecimal.valueOf(500000))
                .depositAmount(BigDecimal.valueOf(150000))
                .additionalFee(BigDecimal.ZERO)
                .remainingAmount(BigDecimal.valueOf(350000))
                .bookingType(BookingType.ONLINE)
                .status(BookingStatus.WAITING_APPROVAL)
                .build();
        booking.setId(10L);
    }

    @Test
    @DisplayName("getBookings: Tra cứu danh sách phân trang thành công")
    void getBookings_Success() {
        AdminBookingSearchRequest searchRequest = AdminBookingSearchRequest.builder()
                .page(1)
                .size(10)
                .keyword("0987")
                .build();

        Page<Booking> page = new PageImpl<>(List.of(booking));
        when(bookingRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<AdminBookingSummaryResponse> response = adminBookingService.getBookings(searchRequest);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals("#B173000000000", response.getItems().get(0).getBookingCode());
        assertEquals("Lê Văn Khách", response.getItems().get(0).getCustomerName());
    }

    @Test
    @DisplayName("getBookingDetail: Lấy chi tiết đơn đặt sân thành công kèm giao dịch")
    void getBookingDetail_ValidId_ReturnsFullDetails() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(paymentTransactionRepository.findByBookingIdOrderByCreatedAtDesc(10L)).thenReturn(Collections.emptyList());

        AdminBookingDetailResponse response = adminBookingService.getBookingDetail(10L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("#B173000000000", response.getBookingCode());
        assertEquals(BookingStatus.WAITING_APPROVAL, response.getStatus());
        assertEquals(BigDecimal.valueOf(500000), response.getGrandTotal());
    }

    @Test
    @DisplayName("getBookingDetail: Ném ngoại lệ BOOKING_NOT_FOUND khi ID không tồn tại")
    void getBookingDetail_NotFound_ThrowsException() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> adminBookingService.getBookingDetail(99L));
        assertEquals(ErrorCode.BOOKING_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("createCounterBooking: Đặt sân tại quầy thành công (UC011.1)")
    void createCounterBooking_Success() {
        CounterBookingCreationRequest request = CounterBookingCreationRequest.builder()
                .pitchId(1L)
                .timeSlotId(1L)
                .bookingDate(LocalDate.now().plusDays(2))
                .guestName("Trần Văn Vãng Lai")
                .guestPhone("0912345678")
                .paidAmount(BigDecimal.valueOf(200000))
                .paymentMethod(PaymentMethodCode.CASH)
                .build();

        when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlot));
        when(bookingRepository.existsByPitchIdAndTimeSlotIdAndBookingDateAndStatusNotIn(anyLong(), anyLong(), any(), any()))
                .thenReturn(false);
        when(priceMatrixRepository.findByPitchTypeIdAndIsPeakHourAndDayType(anyLong(), anyBoolean(), any()))
                .thenReturn(Optional.of(priceMatrix));
        when(userRepository.findById(2L)).thenReturn(Optional.of(staffUser));

        PaymentMethod cashMethod = new PaymentMethod();
        cashMethod.setCode(PaymentMethodCode.CASH);
        cashMethod.setName("Tiền mặt");
        when(paymentMethodRepository.findByCode(PaymentMethodCode.CASH)).thenReturn(Optional.of(cashMethod));

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(20L);
            return b;
        });

        AdminBookingDetailResponse response = adminBookingService.createCounterBooking(request, staffPrincipal);

        assertNotNull(response);
        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
        assertEquals(BookingType.AT_COUNTER, response.getBookingType());
        verify(paymentTransactionRepository, times(1)).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("createCounterBooking: Ném ngoại lệ khi ca đá đã bị người khác đặt")
    void createCounterBooking_Overlap_ThrowsAlreadyBooked() {
        CounterBookingCreationRequest request = CounterBookingCreationRequest.builder()
                .pitchId(1L)
                .timeSlotId(1L)
                .bookingDate(LocalDate.now().plusDays(2))
                .guestName("Trần Văn Vãng Lai")
                .guestPhone("0912345678")
                .build();

        when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlot));
        when(bookingRepository.existsByPitchIdAndTimeSlotIdAndBookingDateAndStatusNotIn(anyLong(), anyLong(), any(), any()))
                .thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> adminBookingService.createCounterBooking(request, staffPrincipal));
        assertEquals(ErrorCode.BOOKING_TIME_SLOT_ALREADY_BOOKED, ex.getErrorCode());
    }

    @Test
    @DisplayName("approveBooking: Duyệt đơn online thành công sang CONFIRMED (UC011.2)")
    void approveBooking_WaitingApproval_Success() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(userRepository.findById(2L)).thenReturn(Optional.of(staffUser));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminBookingDetailResponse response = adminBookingService.approveBooking(10L, staffPrincipal);

        assertNotNull(response);
        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    @DisplayName("approveBooking: Ném lỗi khi đơn không ở trạng thái WAITING_APPROVAL")
    void approveBooking_InvalidStatus_ThrowsException() {
        booking.setStatus(BookingStatus.CONFIRMED);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        AppException ex = assertThrows(AppException.class, () -> adminBookingService.approveBooking(10L, staffPrincipal));
        assertEquals(ErrorCode.BOOKING_NOT_WAITING_APPROVAL, ex.getErrorCode());
    }

    @Test
    @DisplayName("cancelAndRefundBooking: Hủy đơn và hoàn cọc thành công (UC011.3)")
    void cancelAndRefundBooking_ValidReason_Success() {
        booking.setStatus(BookingStatus.CONFIRMED);
        AdminBookingCancelRequest request = AdminBookingCancelRequest.builder()
                .cancelReason("Thời tiết mưa bão ngập sân")
                .refundMethod(PaymentMethodCode.CASH)
                .refundAmount(BigDecimal.valueOf(150000))
                .build();

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(userRepository.findById(2L)).thenReturn(Optional.of(staffUser));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentMethod cashMethod = new PaymentMethod();
        cashMethod.setCode(PaymentMethodCode.CASH);
        when(paymentMethodRepository.findByCode(PaymentMethodCode.CASH)).thenReturn(Optional.of(cashMethod));

        AdminBookingDetailResponse response = adminBookingService.cancelAndRefundBooking(10L, request, staffPrincipal);

        assertNotNull(response);
        assertEquals(BookingStatus.REFUNDED, response.getStatus());
        verify(paymentTransactionRepository, times(1)).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("cancelAndRefundBooking: Ném lỗi khi đơn đã COMPLETED")
    void cancelAndRefundBooking_AlreadyCompleted_ThrowsException() {
        booking.setStatus(BookingStatus.COMPLETED);
        AdminBookingCancelRequest request = AdminBookingCancelRequest.builder()
                .cancelReason("Khách yêu cầu hủy muộn")
                .refundMethod(PaymentMethodCode.CASH)
                .build();

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        AppException ex = assertThrows(AppException.class, () -> adminBookingService.cancelAndRefundBooking(10L, request, staffPrincipal));
        assertEquals(ErrorCode.BOOKING_ALREADY_CANCELLED_OR_COMPLETED, ex.getErrorCode());
    }

    @Test
    @DisplayName("collectPayment: Thu tiền 70% còn lại + phụ phí và đóng ca thành công (UC011.4)")
    void collectPayment_Success_CalculatesGrandTotal() {
        booking.setStatus(BookingStatus.CONFIRMED);
        CollectPaymentRequest request = CollectPaymentRequest.builder()
                .additionalFee(BigDecimal.valueOf(50000)) // Phụ phí 50k
                .paymentMethod(PaymentMethodCode.CASH)
                .paymentNote("Tiền sân + 5 chai nước")
                .build();

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(userRepository.findById(2L)).thenReturn(Optional.of(staffUser));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentMethod cashMethod = new PaymentMethod();
        cashMethod.setCode(PaymentMethodCode.CASH);
        when(paymentMethodRepository.findByCode(PaymentMethodCode.CASH)).thenReturn(Optional.of(cashMethod));

        AdminBookingDetailResponse response = adminBookingService.collectPayment(10L, request, staffPrincipal);

        assertNotNull(response);
        assertEquals(BookingStatus.COMPLETED, response.getStatus());
        assertEquals(BigDecimal.valueOf(50000), response.getAdditionalFee());
        assertEquals(BigDecimal.ZERO, response.getRemainingAmount());
        verify(paymentTransactionRepository, times(1)).save(argThat(txn ->
                txn.getAmount().compareTo(BigDecimal.valueOf(400000)) == 0 && // 500k - 150k + 50k = 400k
                txn.getPaymentType() == PaymentType.REMAINING_70
        ));
    }

    @Test
    @DisplayName("collectPayment: Ném lỗi khi đơn không ở trạng thái CONFIRMED hoặc IN_PROGRESS")
    void collectPayment_InvalidStatus_ThrowsException() {
        booking.setStatus(BookingStatus.CANCELLED);
        CollectPaymentRequest request = CollectPaymentRequest.builder()
                .paymentMethod(PaymentMethodCode.CASH)
                .build();

        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        AppException ex = assertThrows(AppException.class, () -> adminBookingService.collectPayment(10L, request, staffPrincipal));
        assertEquals(ErrorCode.BOOKING_CANNOT_COLLECT_PAYMENT, ex.getErrorCode());
    }
}
