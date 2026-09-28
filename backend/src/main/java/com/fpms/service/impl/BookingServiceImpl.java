package com.fpms.service.impl;

import com.fpms.dto.request.BookingCreationRequest;
import com.fpms.dto.response.BookingEventDto;
import com.fpms.dto.response.BookingResponse;
import com.fpms.dto.response.ScheduleGridResponse;
import com.fpms.dto.response.ScheduleGridItemResponse;
import com.fpms.dto.response.PriceItemResponse;
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
import com.fpms.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final PitchRepository pitchRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final HolidayRepository holidayRepository;
    private final PriceMatrixRepository priceMatrixRepository;
    private final BookingMapper bookingMapper;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private DayType determineDayType(LocalDate date) {
        if (holidayRepository.existsByHolidayDate(date)) {
            return DayType.HOLIDAY;
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return DayType.WEEKEND;
        }
        return DayType.WEEKDAY;
    }

    @Override
    @Transactional
    public BookingResponse createBooking(BookingCreationRequest request, UserPrincipal userPrincipal) {
        // 1. Validate Pitch and TimeSlot
        Pitch pitch = pitchRepository.findById(request.getPitchId())
                .orElseThrow(() -> new AppException(ErrorCode.PITCH_NOT_FOUND));

        TimeSlot timeSlot = timeSlotRepository.findById(request.getTimeSlotId())
                .orElseThrow(() -> new AppException(ErrorCode.TIME_SLOT_NOT_FOUND));

        // 2. Check for overlapping bookings
        boolean isOccupied = bookingRepository.existsByPitchIdAndTimeSlotIdAndBookingDateAndStatusNotIn(
                pitch.getId(),
                timeSlot.getId(),
                request.getBookingDate(),
                Arrays.asList(BookingStatus.CANCELLED, BookingStatus.REFUNDED)
        );

        if (isOccupied) {
            throw new AppException(ErrorCode.BOOKING_TIME_SLOT_ALREADY_BOOKED); 
        }

        // 3. Determine Price
        DayType dayType = determineDayType(request.getBookingDate());
        PriceMatrix priceMatrix = priceMatrixRepository.findByPitchTypeIdAndIsPeakHourAndDayType(
                pitch.getPitchType().getId(),
                timeSlot.getIsPeakHour(),
                dayType
        ).orElseThrow(() -> new RuntimeException("Price matrix not configured for this time slot"));

        // 4. Determine User and Booking Type
        User user = null;
        if (userPrincipal != null && userPrincipal.getId() != null) {
            user = userRepository.findById(userPrincipal.getId()).orElse(null);
        }

        Booking booking = bookingMapper.toBooking(request);
        
        if (user != null) {
            if (user.getRole().getRoleName() == RoleName.ROLE_STAFF || user.getRole().getRoleName() == RoleName.ROLE_ADMIN) {
                booking.setStaff(user);
                booking.setBookingType(BookingType.AT_COUNTER);
            } else {
                booking.setCustomer(user);
                booking.setBookingType(BookingType.ONLINE);
            }
        } else {
            booking.setBookingType(BookingType.ONLINE);
        }

        booking.setBookingCode("#B" + System.currentTimeMillis());
        booking.setPitch(pitch);
        booking.setTimeSlot(timeSlot);
        booking.setPriceSnapshot(priceMatrix.getPrice());
        booking.setStartTimeSnapshot(timeSlot.getStartTime());
        booking.setEndTimeSnapshot(timeSlot.getEndTime());
        booking.setPitchNameSnapshot(pitch.getName());
        booking.setTotalPitchAmount(priceMatrix.getPrice());
        booking.setDepositAmount(BigDecimal.ZERO); // Vì confirm luôn chưa có thanh toán VNPay
        booking.setAdditionalFee(BigDecimal.ZERO);
        booking.setRemainingAmount(priceMatrix.getPrice());
        booking.setStatus(BookingStatus.CONFIRMED); // Force confirm right away

        booking = bookingRepository.save(booking);

        // Notify clients about the new booking ONLY AFTER transaction commits
        BookingEventDto event = BookingEventDto.builder()
                .action("BOOKING_CREATED")
                .pitchId(booking.getPitch().getId())
                .timeSlotId(booking.getTimeSlot().getId())
                .date(booking.getBookingDate().toString())
                .build();
                
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend("/topic/schedule", event);
            }
        });

        // 5. Map to Response
        return bookingMapper.toBookingResponse(booking);
    }

    @Override
    public ScheduleGridResponse getScheduleGrid(LocalDate date, Long pitchTypeId) {
        if (date.isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.PAST_DATE_NOT_ALLOWED);
        }

        ScheduleGridResponse response = new ScheduleGridResponse();

        // 1. Dữ liệu Bookings
        List<Booking> bookings = bookingRepository.findOccupyingBookings(date, pitchTypeId);

        List<ScheduleGridItemResponse> gridItems = bookings.stream()
                .map(b -> new ScheduleGridItemResponse(
                        b.getPitch().getId(),
                        b.getTimeSlot().getId(),
                        b.getStatus().name()
                ))
                .collect(Collectors.toList());

        response.setBookings(gridItems);

        // 2. Dữ liệu Bảng giá
        DayType currentDayType = determineDayType(date);
        List<PriceMatrix> matrices = priceMatrixRepository.findByDayType(currentDayType);

        List<PriceItemResponse> priceItems = matrices.stream()
                .map(m -> new PriceItemResponse(m.getPitchType().getId(), m.getIsPeakHour(), m.getPrice()))
                .collect(Collectors.toList());

        response.setPrices(priceItems);

        return response;
    }
}
