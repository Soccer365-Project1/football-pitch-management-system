package com.fpms.service;

import com.fpms.dto.request.BookingCreationRequest;
import com.fpms.dto.response.BookingResponse;
import com.fpms.entity.*;
import com.fpms.entity.enums.BookingStatus;
import com.fpms.entity.enums.BookingType;
import com.fpms.entity.enums.DayType;
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
    private SimpMessagingTemplate messagingTemplate;

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
        savedBooking.setStatus(BookingStatus.CONFIRMED);
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
            assertEquals(BookingStatus.CONFIRMED, bookingToSave.getStatus());
            assertEquals(BookingType.ONLINE, bookingToSave.getBookingType());
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
}
