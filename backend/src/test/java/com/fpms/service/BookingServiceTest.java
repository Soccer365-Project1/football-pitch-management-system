package com.fpms.service;

import com.fpms.dto.request.BookingCancelRequest;
import com.fpms.dto.request.BookingCreationRequest;
import com.fpms.dto.response.BookingResponse;
import com.fpms.dto.response.BookingSummaryResponse;
import com.fpms.entity.*;
import com.fpms.entity.enums.BookingStatus;
import com.fpms.entity.enums.BookingType;
import com.fpms.entity.enums.DayType;
import com.fpms.entity.enums.PitchStatus;
import com.fpms.entity.enums.RoleName;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.mapper.BookingMapper;
import com.fpms.repository.*;
import com.fpms.security.UserPrincipal;
import com.fpms.service.impl.BookingServiceImpl;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.List;
import java.util.Arrays;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

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
    private BookingMapper bookingMapper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private com.fpms.config.VNPayConfig vnPayConfig;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private BookingCancellationService bookingCancellationService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Pitch pitch;
    private TimeSlot timeSlot;
    private PriceMatrix priceMatrix;
    private User user;
    private UserPrincipal userPrincipal;
    private BookingCreationRequest request;

    @BeforeEach
    void setUp() {
        PitchType pitchType = new PitchType();
        pitchType.setId(1L);
        pitchType.setName("Sân 5 người");

        pitch = new Pitch();
        pitch.setId(1L);
        pitch.setName("Sân 1");
        pitch.setPitchType(pitchType);

        timeSlot = new TimeSlot();
        timeSlot.setId(1L);
        timeSlot.setStartTime(LocalTime.of(17, 0));
        timeSlot.setEndTime(LocalTime.of(18, 0));
        timeSlot.setIsPeakHour(true);

        priceMatrix = new PriceMatrix();
        priceMatrix.setId(1L);
        priceMatrix.setPitchType(pitchType);
        priceMatrix.setPrice(BigDecimal.valueOf(200000));
        priceMatrix.setIsPeakHour(true);

        Role role = new Role();
        role.setRoleName(RoleName.ROLE_CUSTOMER);

        user = new User();
        user.setId(1L);
        user.setRole(role);

        userPrincipal = UserPrincipal.create(user);

        request = new BookingCreationRequest();
        request.setPitchId(1L);
        request.setTimeSlotId(1L);
        request.setBookingDate(LocalDate.now().plusDays(1)); // Ngày mai
    }

    @Test
    @DisplayName("TC-S01: Tạo Booking thành công (Khách hàng online)")
    void createBooking_Online_Success() {
        // Mock
        when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlot));
        when(bookingRepository.existsByPitchIdAndTimeSlotIdAndBookingDateAndStatusNotIn(
                eq(1L), eq(1L), eq(request.getBookingDate()), anyList())).thenReturn(false);
        when(holidayRepository.existsByHolidayDate(request.getBookingDate())).thenReturn(false);
        when(priceMatrixRepository.findByPitchTypeIdAndIsPeakHourAndDayType(
                eq(1L), eq(true), any(DayType.class))).thenReturn(Optional.of(priceMatrix));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Booking bookingToSave = new Booking();
        when(bookingMapper.toBooking(request)).thenReturn(bookingToSave);

        Booking savedBooking = new Booking();
        savedBooking.setId(1L);
        savedBooking.setPitch(pitch);
        savedBooking.setTimeSlot(timeSlot);
        savedBooking.setBookingDate(request.getBookingDate());
        savedBooking.setStatus(BookingStatus.PENDING_HOLD);
        savedBooking.setBookingType(BookingType.ONLINE);
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingResponse mockResponse = new BookingResponse();
        mockResponse.setId(1L);
        when(bookingMapper.toBookingResponse(savedBooking)).thenReturn(mockResponse);

        // Khởi tạo TransactionSynchronizationManager cho test môi trường non-transactional
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }

        try {
            // Act
            BookingResponse response = bookingService.createBooking(request, userPrincipal);

            // Assert
            assertNotNull(response);
            assertEquals(1L, response.getId());
            verify(bookingRepository, times(1)).save(any(Booking.class));
            assertEquals(BookingStatus.PENDING_HOLD, bookingToSave.getStatus());
            assertEquals(BookingType.ONLINE, bookingToSave.getBookingType());
            assertNotNull(bookingToSave.getHoldExpiresAt());
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    @DisplayName("TC-S02: Tạo Booking thành công (Nhân viên đặt tại quầy)")
    void createBooking_AtCounter_Success() {
        // Cập nhật user role thành STAFF
        Role staffRole = new Role();
        staffRole.setRoleName(RoleName.ROLE_STAFF);
        user.setRole(staffRole);
        userPrincipal = UserPrincipal.create(user);

        // Mock
        when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlot));
        when(bookingRepository.existsByPitchIdAndTimeSlotIdAndBookingDateAndStatusNotIn(
                eq(1L), eq(1L), eq(request.getBookingDate()), anyList())).thenReturn(false);
        when(holidayRepository.existsByHolidayDate(request.getBookingDate())).thenReturn(false);
        when(priceMatrixRepository.findByPitchTypeIdAndIsPeakHourAndDayType(
                eq(1L), eq(true), any(DayType.class))).thenReturn(Optional.of(priceMatrix));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Booking bookingToSave = new Booking();
        when(bookingMapper.toBooking(request)).thenReturn(bookingToSave);
        Booking savedBooking = new Booking();
        savedBooking.setId(1L);
        savedBooking.setPitch(pitch);
        savedBooking.setTimeSlot(timeSlot);
        savedBooking.setBookingDate(request.getBookingDate());
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        when(bookingMapper.toBookingResponse(any())).thenReturn(new BookingResponse());

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }
        try {
            bookingService.createBooking(request, userPrincipal);
            assertEquals(BookingType.AT_COUNTER, bookingToSave.getBookingType());
            assertEquals(BookingStatus.CONFIRMED, bookingToSave.getStatus());
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    @DisplayName("TC-S03: Lỗi Pitch không tồn tại")
    void createBooking_PitchNotFound_Fails() {
        when(pitchRepository.findById(1L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> {
            bookingService.createBooking(request, userPrincipal);
        });

        assertEquals(ErrorCode.PITCH_NOT_FOUND, exception.getErrorCode());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-S03b: Lỗi Sân bóng đang bảo trì hoặc dừng hoạt động")
    void createBooking_PitchNotActive_Fails() {
        // 1. Kiểm tra khi sân đang bảo trì (MAINTENANCE)
        pitch.setStatus(PitchStatus.MAINTENANCE);
        when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));

        AppException exception = assertThrows(AppException.class, () -> {
            bookingService.createBooking(request, userPrincipal);
        });

        assertEquals(ErrorCode.PITCH_NOT_ACTIVE, exception.getErrorCode());

        // 2. Kiểm tra khi sân dừng hoạt động (INACTIVE)
        pitch.setStatus(PitchStatus.INACTIVE);
        AppException inactiveException = assertThrows(AppException.class, () -> {
            bookingService.createBooking(request, userPrincipal);
        });

        assertEquals(ErrorCode.PITCH_NOT_ACTIVE, inactiveException.getErrorCode());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-S04: Lỗi Trùng lịch (Slot đã có người đặt)")
    void createBooking_AlreadyBooked_Fails() {
        when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlot));
        when(bookingRepository.existsByPitchIdAndTimeSlotIdAndBookingDateAndStatusNotIn(
                eq(1L), eq(1L), eq(request.getBookingDate()), anyList())).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> {
            bookingService.createBooking(request, userPrincipal);
        });

        assertEquals(ErrorCode.BOOKING_TIME_SLOT_ALREADY_BOOKED, exception.getErrorCode());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-S05: Đặt cọc 30% thành công & Sinh URL thanh toán VNPay đúng chuẩn")
    void createBookingWithDepositPayment_Success() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }
        try {
            when(pitchRepository.findById(1L)).thenReturn(Optional.of(pitch));
            when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(timeSlot));
            when(bookingRepository.existsByPitchIdAndTimeSlotIdAndBookingDateAndStatusNotIn(
                    eq(1L), eq(1L), eq(request.getBookingDate()), anyList())).thenReturn(false);
            when(holidayRepository.existsByHolidayDate(any())).thenReturn(false);
            when(priceMatrixRepository.findByPitchTypeIdAndIsPeakHourAndDayType(eq(1L), eq(true), any(DayType.class)))
                    .thenReturn(Optional.of(priceMatrix));
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            Booking bookingToSave = new Booking();
            bookingToSave.setId(10L);
            bookingToSave.setBookingCode("#B123456");
            bookingToSave.setPitch(pitch);
            bookingToSave.setTimeSlot(timeSlot);
            bookingToSave.setBookingDate(request.getBookingDate());
            bookingToSave.setTotalPitchAmount(BigDecimal.valueOf(200000));
            bookingToSave.setDepositAmount(BigDecimal.valueOf(60000));
            bookingToSave.setStatus(BookingStatus.PENDING_HOLD);

            when(bookingMapper.toBooking(request)).thenReturn(bookingToSave);
            when(bookingRepository.save(any(Booking.class))).thenReturn(bookingToSave);

            when(bookingMapper.toBookingResponse(any(Booking.class))).thenAnswer(invocation -> {
                Booking b = invocation.getArgument(0);
                return BookingResponse.builder()
                        .bookingCode(b.getBookingCode())
                        .build();
            });
            when(bookingRepository.findByBookingCode(anyString())).thenAnswer(invocation -> {
                String code = invocation.getArgument(0);
                bookingToSave.setBookingCode(code);
                return Optional.of(bookingToSave);
            });

            PaymentMethod vnpayMethod = PaymentMethod.builder()
                    .code(com.fpms.entity.enums.PaymentMethodCode.VNPAY)
                    .name("VNPAY")
                    .build();
            when(paymentMethodRepository.findByCode(com.fpms.entity.enums.PaymentMethodCode.VNPAY))
                    .thenReturn(Optional.of(vnpayMethod));

            when(vnPayConfig.getVersion()).thenReturn("2.1.0");
            when(vnPayConfig.getCommand()).thenReturn("pay");
            when(vnPayConfig.getTmnCode()).thenReturn("TMNTEST");
            when(vnPayConfig.getReturnUrl()).thenReturn("http://localhost:5173/payment-result");
            when(vnPayConfig.getPayUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
            when(vnPayConfig.getHashSecret()).thenReturn("HASHSECRET123");

            org.springframework.mock.web.MockHttpServletRequest httpRequest = new org.springframework.mock.web.MockHttpServletRequest();
            httpRequest.setRemoteAddr("127.0.0.1");

            com.fpms.dto.response.BookingDepositResponse depositResponse =
                    bookingService.createBookingWithDepositPayment(request, userPrincipal, httpRequest);

            assertNotNull(depositResponse);
            assertNotNull(depositResponse.getBookingCode());
            assertTrue(depositResponse.getBookingCode().startsWith("#B"));
            assertNotNull(depositResponse.getPaymentUrl());
            assertTrue(depositResponse.getPaymentUrl().contains("vnp_Amount=6000000"), "vnp_Amount phải nhân 100 thành 6000000 (cọc 60.000)");
            assertTrue(depositResponse.getPaymentUrl().contains("vnp_SecureHash="), "URL phải chứa chữ ký SHA512");

            verify(paymentTransactionRepository, times(1)).save(any(PaymentTransaction.class));
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    @DisplayName("getMyBookings - Thành công khi người dùng đã đăng nhập")
    void getMyBookings_Success() {
        Booking booking = Booking.builder()
                .bookingCode("#B123456")
                .bookingDate(LocalDate.now())
                .status(BookingStatus.CONFIRMED)
                .build();
        booking.setId(10L);

        BookingSummaryResponse response = BookingSummaryResponse.builder()
                .id(10L)
                .bookingCode("#B123456")
                .pitchName("Sân 1")
                .status(BookingStatus.CONFIRMED)
                .build();

        org.springframework.data.domain.Page<Booking> page = new org.springframework.data.domain.PageImpl<>(
                List.of(booking),
                org.springframework.data.domain.PageRequest.of(0, 10),
                1
        );

        when(bookingRepository.findMyBookings(eq(userPrincipal.getId()), any(), any(), any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);
        when(bookingMapper.toBookingSummaryResponse(booking)).thenReturn(response);

        com.fpms.common.response.PageResponse<BookingSummaryResponse> result =
                bookingService.getMyBookings(userPrincipal, null, null, null, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getItems().size());
        assertEquals(10L, result.getItems().get(0).getId());
    }

    @Test
    @DisplayName("getMyBookings - Lỗi khi chưa xác thực (userPrincipal == null)")
    void getMyBookings_Unauthenticated() {
        AppException ex = assertThrows(AppException.class, () ->
                bookingService.getMyBookings(null, null, null, null, 1, 10));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    @DisplayName("getMyBookingDetail - Thành công khi đơn thuộc về người dùng")
    void getMyBookingDetail_Success() {
        Booking booking = Booking.builder()
                .bookingCode("#B123456")
                .status(BookingStatus.CONFIRMED)
                .build();
        booking.setId(10L);

        BookingResponse response = BookingResponse.builder()
                .id(10L)
                .bookingCode("#B123456")
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findByIdAndCustomerIdAndIsDeletedFalse(10L, userPrincipal.getId()))
                .thenReturn(Optional.of(booking));
        when(bookingMapper.toBookingResponse(booking)).thenReturn(response);

        BookingResponse result = bookingService.getMyBookingDetail(10L, userPrincipal);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("#B123456", result.getBookingCode());
    }

    @Test
    @DisplayName("getMyBookingDetail - Thất bại khi không tìm thấy hoặc không thuộc về người dùng")
    void getMyBookingDetail_NotFound() {
        when(bookingRepository.findByIdAndCustomerIdAndIsDeletedFalse(999L, userPrincipal.getId()))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                bookingService.getMyBookingDetail(999L, userPrincipal));
        assertEquals(ErrorCode.BOOKING_NOT_FOUND, ex.getErrorCode());
    }


    @Test
    @DisplayName("cancelBooking - Thành công khi trạng thái CONFIRMED và chưa đến start time")
    void cancelBooking_Success_Confirmed() {
        Booking booking = Booking.builder()
                .bookingCode("#B124")
                .status(BookingStatus.CONFIRMED)
                .bookingDate(LocalDate.now().plusDays(2))
                .startTimeSnapshot(LocalTime.of(17, 30))
                .customer(user)
                .pitch(pitch)
                .timeSlot(timeSlot)
                .depositAmount(BigDecimal.valueOf(150000))
                .build();
        booking.setId(21L);

        BookingCancelRequest cancelRequest = new BookingCancelRequest();
        cancelRequest.setReason("Có việc bận gia đình");

        BookingResponse response = BookingResponse.builder()
                .id(21L)
                .bookingCode("#B124")
                .status(BookingStatus.WAITING_CANCELLATION)
                .build();

        when(bookingRepository.findById(21L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toBookingResponse(booking)).thenReturn(response);

        BookingResponse result = bookingService.cancelBooking(21L, cancelRequest, userPrincipal);

        assertNotNull(result);
        assertEquals(BookingStatus.WAITING_CANCELLATION, booking.getStatus());
        verify(bookingCancellationService, times(1)).createCancellation(eq(booking), eq(user), eq(cancelRequest));
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    @DisplayName("cancelBooking - Thành công khi trạng thái WAITING_APPROVAL")
    void cancelBooking_Success_WaitingApproval() {
        Booking booking = Booking.builder()
                .bookingCode("#B128")
                .status(BookingStatus.WAITING_APPROVAL)
                .bookingDate(LocalDate.now().plusDays(2))
                .startTimeSnapshot(LocalTime.of(18, 0))
                .customer(user)
                .pitch(pitch)
                .timeSlot(timeSlot)
                .build();
        booking.setId(28L);

        BookingResponse response = BookingResponse.builder()
                .id(28L)
                .bookingCode("#B128")
                .status(BookingStatus.WAITING_CANCELLATION)
                .build();

        when(bookingRepository.findById(28L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toBookingResponse(booking)).thenReturn(response);

        BookingResponse result = bookingService.cancelBooking(28L, null, userPrincipal);

        assertNotNull(result);
        assertEquals(BookingStatus.WAITING_CANCELLATION, booking.getStatus());
        verify(bookingCancellationService, times(1)).createCancellation(eq(booking), eq(user), isNull());
        verify(bookingRepository, times(1)).save(booking);
    }

    @Test
    @DisplayName("cancelBooking - Thất bại khi trạng thái không phải WAITING_APPROVAL hoặc CONFIRMED")
    void cancelBooking_Failed_InvalidStatus() {
        Booking booking = Booking.builder()
                .bookingCode("#B125")
                .status(BookingStatus.IN_PROGRESS)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTimeSnapshot(LocalTime.of(19, 0))
                .customer(user)
                .build();
        booking.setId(22L);

        when(bookingRepository.findById(22L)).thenReturn(Optional.of(booking));

        AppException ex = assertThrows(AppException.class, () ->
                bookingService.cancelBooking(22L, null, userPrincipal));
        assertEquals(ErrorCode.CANNOT_CANCEL_BOOKING_STATUS, ex.getErrorCode());
        verify(bookingCancellationService, never()).createCancellation(any(), any(), any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelBooking - Thất bại khi thời gian đến ca đá còn dưới 24 tiếng")
    void cancelBooking_Failed_Within24Hours() {
        LocalDateTime startDateTime = LocalDateTime.now().plusHours(5);
        Booking booking = Booking.builder()
                .bookingCode("#B129")
                .status(BookingStatus.CONFIRMED)
                .bookingDate(startDateTime.toLocalDate())
                .startTimeSnapshot(startDateTime.toLocalTime())
                .customer(user)
                .build();
        booking.setId(29L);

        when(bookingRepository.findById(29L)).thenReturn(Optional.of(booking));

        AppException ex = assertThrows(AppException.class, () ->
                bookingService.cancelBooking(29L, null, userPrincipal));
        assertEquals(ErrorCode.CANNOT_CANCEL_WITHIN_24_HOURS, ex.getErrorCode());
        verify(bookingCancellationService, never()).createCancellation(any(), any(), any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelBooking - Thất bại khi không phải chủ đơn đặt sân")
    void cancelBooking_Failed_UnauthorizedUser() {
        User otherUser = new User();
        otherUser.setId(99L); // ID khác với userPrincipal (ID = 1L)

        Booking booking = Booking.builder()
                .bookingCode("#B127")
                .status(BookingStatus.CONFIRMED)
                .bookingDate(LocalDate.now().plusDays(1))
                .startTimeSnapshot(LocalTime.of(19, 0))
                .customer(otherUser)
                .build();
        booking.setId(24L);

        when(bookingRepository.findById(24L)).thenReturn(Optional.of(booking));

        AppException ex = assertThrows(AppException.class, () ->
                bookingService.cancelBooking(24L, null, userPrincipal));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
        verify(bookingCancellationService, never()).createCancellation(any(), any(), any());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelBooking - Thất bại khi chưa xác thực (userPrincipal == null)")
    void cancelBooking_Failed_Unauthenticated() {
        AppException ex = assertThrows(AppException.class, () ->
                bookingService.cancelBooking(25L, null, null));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    @Test
    @DisplayName("cancelBooking - Thất bại khi lý do hủy rỗng hoặc chỉ chứa khoảng trắng")
    void cancelBooking_Failed_BlankReason() {
        BookingCancelRequest blankRequest = new BookingCancelRequest();
        blankRequest.setReason("   ");

        AppException ex = assertThrows(AppException.class, () ->
                bookingService.cancelBooking(26L, blankRequest, userPrincipal));
        assertEquals(ErrorCode.BOOKING_CANCEL_REASON_REQUIRED, ex.getErrorCode());
    }
}
