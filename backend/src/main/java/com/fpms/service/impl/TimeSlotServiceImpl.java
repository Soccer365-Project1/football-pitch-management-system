package com.fpms.service.impl;

import com.fpms.dto.request.TimeSlotRequest;
import com.fpms.dto.response.TimeSlotResponse;
import com.fpms.entity.TimeSlot;
import com.fpms.entity.enums.BookingStatus;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.mapper.TimeSlotMapper;
import com.fpms.repository.BookingRepository;
import com.fpms.repository.TimeSlotRepository;
import com.fpms.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimeSlotServiceImpl implements TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final BookingRepository bookingRepository;
    private final TimeSlotMapper timeSlotMapper;

    private static final List<BookingStatus> ACTIVE_BOOKING_STATUSES = List.of(
            BookingStatus.PENDING_HOLD,
            BookingStatus.WAITING_APPROVAL,
            BookingStatus.CONFIRMED,
            BookingStatus.IN_PROGRESS,
            BookingStatus.WAITING_CANCELLATION
    );

    @Override
    public List<TimeSlotResponse> getAllTimeSlots(Boolean activeOnly) {
        log.info("Lấy danh sách khung giờ - activeOnly: {}", activeOnly);
        List<TimeSlot> slots;
        if (Boolean.TRUE.equals(activeOnly)) {
            slots = timeSlotRepository.findAllByIsActiveTrueOrderByStartTimeAsc();
        } else {
            slots = timeSlotRepository.findAllByOrderByStartTimeAsc();
        }
        return timeSlotMapper.toTimeSlotResponseList(slots);
    }

    @Override
    public TimeSlotResponse getTimeSlotById(Long id) {
        log.info("Lấy thông tin chi tiết khung giờ ID: {}", id);
        TimeSlot timeSlot = timeSlotRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TIME_SLOT_NOT_FOUND));
        return timeSlotMapper.toTimeSlotResponse(timeSlot);
    }

    @Override
    @Transactional
    public TimeSlotResponse createTimeSlot(TimeSlotRequest request) {
        log.info("Tạo mới khung giờ: {} - {}, isPeakHour: {}",
                request.getStartTime(), request.getEndTime(), request.getIsPeakHour());

        validateTimeSlot(request.getStartTime(), request.getEndTime());

        if (timeSlotRepository.existsOverlappingSlot(null, request.getStartTime(), request.getEndTime())) {
            log.warn("Khung giờ bị trùng lặp/chồng chéo: {} - {}", request.getStartTime(), request.getEndTime());
            throw new AppException(ErrorCode.TIME_SLOT_OVERLAPPING);
        }

        TimeSlot timeSlot = timeSlotMapper.toTimeSlot(request);
        timeSlot.setIsActive(true);
        if (timeSlot.getIsPeakHour() == null) {
            timeSlot.setIsPeakHour(false);
        }

        TimeSlot savedSlot = timeSlotRepository.save(timeSlot);
        log.info("Tạo khung giờ thành công với ID: {}", savedSlot.getId());
        return timeSlotMapper.toTimeSlotResponse(savedSlot);
    }

    @Override
    @Transactional
    public TimeSlotResponse updateTimeSlot(Long id, TimeSlotRequest request) {
        log.info("Cập nhật khung giờ ID: {} - {} - {}, isPeakHour: {}",
                id, request.getStartTime(), request.getEndTime(), request.getIsPeakHour());

        TimeSlot timeSlot = timeSlotRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TIME_SLOT_NOT_FOUND));

        validateTimeSlot(request.getStartTime(), request.getEndTime());

        if (timeSlotRepository.existsOverlappingSlot(id, request.getStartTime(), request.getEndTime())) {
            log.warn("Khung giờ cập nhật bị chồng chéo với khung giờ khác: {} - {}",
                    request.getStartTime(), request.getEndTime());
            throw new AppException(ErrorCode.TIME_SLOT_OVERLAPPING);
        }

        timeSlot.setStartTime(request.getStartTime());
        timeSlot.setEndTime(request.getEndTime());
        if (request.getIsPeakHour() != null) {
            timeSlot.setIsPeakHour(request.getIsPeakHour());
        }

        TimeSlot updatedSlot = timeSlotRepository.save(timeSlot);
        log.info("Cập nhật khung giờ ID: {} thành công", id);
        return timeSlotMapper.toTimeSlotResponse(updatedSlot);
    }

    @Override
    @Transactional
    public void deleteTimeSlot(Long id) {
        log.info("Xóa khung giờ ID: {}", id);
        TimeSlot timeSlot = timeSlotRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TIME_SLOT_NOT_FOUND));

        boolean hasActiveBookings = bookingRepository
                .existsByTimeSlotIdAndBookingDateGreaterThanEqualAndStatusInAndIsDeletedFalse(
                        id, LocalDate.now(), ACTIVE_BOOKING_STATUSES);

        if (hasActiveBookings) {
            log.warn("Chặn xóa khung giờ ID: {} do có đơn đặt chưa hoàn tất", id);
            throw new AppException(ErrorCode.TIME_SLOT_HAS_ACTIVE_BOOKINGS);
        }

        timeSlot.setIsActive(false);
        timeSlotRepository.save(timeSlot);
        log.info("Xóa mềm khung giờ ID: {} thành công", id);
    }

    private void validateTimeSlot(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new AppException(ErrorCode.TIME_SLOT_INVALID_TIME);
        }
    }
}
