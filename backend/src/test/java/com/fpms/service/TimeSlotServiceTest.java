package com.fpms.service;

import com.fpms.dto.request.TimeSlotRequest;
import com.fpms.dto.response.PublicTimeSlotResponse;
import com.fpms.dto.response.TimeSlotResponse;
import com.fpms.entity.TimeSlot;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.mapper.TimeSlotMapper;
import com.fpms.repository.BookingRepository;
import com.fpms.repository.TimeSlotRepository;
import com.fpms.service.impl.TimeSlotServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeSlotServiceTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private TimeSlotMapper timeSlotMapper;

    @InjectMocks
    private TimeSlotServiceImpl timeSlotService;

    private TimeSlot slot1;
    private TimeSlot slot2;
    private TimeSlotResponse slotResponse1;
    private TimeSlotResponse slotResponse2;
    private TimeSlotRequest validRequest;

    @BeforeEach
    void setUp() {
        slot1 = TimeSlot.builder()
                .id(1L)
                .startTime(LocalTime.of(6, 0))
                .endTime(LocalTime.of(7, 30))
                .isPeakHour(false)
                .isActive(true)
                .build();

        slot2 = TimeSlot.builder()
                .id(2L)
                .startTime(LocalTime.of(17, 30))
                .endTime(LocalTime.of(19, 0))
                .isPeakHour(true)
                .isActive(true)
                .build();

        slotResponse1 = TimeSlotResponse.builder()
                .id(1L)
                .startTime(LocalTime.of(6, 0))
                .endTime(LocalTime.of(7, 30))
                .isPeakHour(false)
                .isActive(true)
                .build();

        slotResponse2 = TimeSlotResponse.builder()
                .id(2L)
                .startTime(LocalTime.of(17, 30))
                .endTime(LocalTime.of(19, 0))
                .isPeakHour(true)
                .isActive(true)
                .build();

        validRequest = TimeSlotRequest.builder()
                .startTime(LocalTime.of(6, 0))
                .endTime(LocalTime.of(7, 30))
                .isPeakHour(false)
                .build();
    }

    // =========================================================================
    // UT-01: Lấy danh sách khung giờ
    // =========================================================================
    @Test
    @DisplayName("UT-01: Lấy toàn bộ danh sách khung giờ sắp xếp thành công")
    void getAllTimeSlots_Success() {
        when(timeSlotRepository.findAllByOrderByStartTimeAsc()).thenReturn(List.of(slot1, slot2));
        when(timeSlotMapper.toTimeSlotResponseList(anyList())).thenReturn(List.of(slotResponse1, slotResponse2));

        List<TimeSlotResponse> result = timeSlotService.getAllTimeSlots(false);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(LocalTime.of(6, 0), result.get(0).getStartTime());
        assertEquals(LocalTime.of(17, 30), result.get(1).getStartTime());
        verify(timeSlotRepository).findAllByOrderByStartTimeAsc();
    }

    // =========================================================================
    // UT-02 & UT-03: Lấy chi tiết khung giờ
    // =========================================================================
    @Test
    @DisplayName("UT-02: Lấy chi tiết khung giờ theo ID hợp lệ thành công")
    void getTimeSlotById_Success() {
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(slot1));
        when(timeSlotMapper.toTimeSlotResponse(slot1)).thenReturn(slotResponse1);

        TimeSlotResponse result = timeSlotService.getTimeSlotById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(LocalTime.of(6, 0), result.getStartTime());
        assertEquals(LocalTime.of(7, 30), result.getEndTime());
        assertFalse(result.getIsPeakHour());
        verify(timeSlotRepository).findById(1L);
    }

    @Test
    @DisplayName("UT-03: Lấy chi tiết khung giờ với ID không tồn tại ném TIME_SLOT_NOT_FOUND")
    void getTimeSlotById_NotFound_ThrowsException() {
        when(timeSlotRepository.findById(99L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> timeSlotService.getTimeSlotById(99L));

        assertEquals(ErrorCode.TIME_SLOT_NOT_FOUND, exception.getErrorCode());
        verify(timeSlotRepository).findById(99L);
        verify(timeSlotMapper, never()).toTimeSlotResponse(any());
    }

    // =========================================================================
    // UT-04 đến UT-08: Thêm mới khung giờ
    // =========================================================================
    @Test
    @DisplayName("UT-04: Thêm ca đá mới thành công (Giờ thường)")
    void createTimeSlot_NormalHour_Success() {
        when(timeSlotRepository.existsOverlappingSlot(isNull(), eq(validRequest.getStartTime()), eq(validRequest.getEndTime())))
                .thenReturn(false);
        when(timeSlotMapper.toTimeSlot(validRequest)).thenReturn(slot1);
        when(timeSlotRepository.save(any(TimeSlot.class))).thenReturn(slot1);
        when(timeSlotMapper.toTimeSlotResponse(slot1)).thenReturn(slotResponse1);

        TimeSlotResponse result = timeSlotService.createTimeSlot(validRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertFalse(result.getIsPeakHour());
        verify(timeSlotRepository).save(any(TimeSlot.class));
    }

    @Test
    @DisplayName("UT-05: Thêm ca đá mới thành công (Giờ vàng - Peak Hour)")
    void createTimeSlot_PeakHour_Success() {
        TimeSlotRequest peakRequest = TimeSlotRequest.builder()
                .startTime(LocalTime.of(17, 30))
                .endTime(LocalTime.of(19, 0))
                .isPeakHour(true)
                .build();

        when(timeSlotRepository.existsOverlappingSlot(isNull(), eq(peakRequest.getStartTime()), eq(peakRequest.getEndTime())))
                .thenReturn(false);
        when(timeSlotMapper.toTimeSlot(peakRequest)).thenReturn(slot2);
        when(timeSlotRepository.save(any(TimeSlot.class))).thenReturn(slot2);
        when(timeSlotMapper.toTimeSlotResponse(slot2)).thenReturn(slotResponse2);

        TimeSlotResponse result = timeSlotService.createTimeSlot(peakRequest);

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertTrue(result.getIsPeakHour());
        verify(timeSlotRepository).save(any(TimeSlot.class));
    }

    @Test
    @DisplayName("UT-06: Thêm ca đá có giờ kết thúc < giờ bắt đầu ném TIME_SLOT_INVALID_TIME")
    void createTimeSlot_EndTimeBeforeStartTime_ThrowsException() {
        TimeSlotRequest invalidRequest = TimeSlotRequest.builder()
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(7, 0))
                .isPeakHour(false)
                .build();

        AppException exception = assertThrows(AppException.class, () -> timeSlotService.createTimeSlot(invalidRequest));

        assertEquals(ErrorCode.TIME_SLOT_INVALID_TIME, exception.getErrorCode());
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-07: Thêm ca đá có giờ kết thúc == giờ bắt đầu ném TIME_SLOT_INVALID_TIME")
    void createTimeSlot_EndTimeEqualsStartTime_ThrowsException() {
        TimeSlotRequest invalidRequest = TimeSlotRequest.builder()
                .startTime(LocalTime.of(7, 0))
                .endTime(LocalTime.of(7, 0))
                .isPeakHour(false)
                .build();

        AppException exception = assertThrows(AppException.class, () -> timeSlotService.createTimeSlot(invalidRequest));

        assertEquals(ErrorCode.TIME_SLOT_INVALID_TIME, exception.getErrorCode());
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-08: Thêm ca đá bị chồng chéo thời gian ném TIME_SLOT_OVERLAPPING")
    void createTimeSlot_Overlapping_ThrowsException() {
        TimeSlotRequest overlapRequest = TimeSlotRequest.builder()
                .startTime(LocalTime.of(6, 30))
                .endTime(LocalTime.of(8, 0))
                .isPeakHour(false)
                .build();

        when(timeSlotRepository.existsOverlappingSlot(isNull(), eq(overlapRequest.getStartTime()), eq(overlapRequest.getEndTime())))
                .thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> timeSlotService.createTimeSlot(overlapRequest));

        assertEquals(ErrorCode.TIME_SLOT_OVERLAPPING, exception.getErrorCode());
        verify(timeSlotRepository, never()).save(any());
    }

    // =========================================================================
    // UT-09 đến UT-13: Chỉnh sửa khung giờ
    // =========================================================================
    @Test
    @DisplayName("UT-09: Chỉnh sửa khung giờ thành công")
    void updateTimeSlot_Success() {
        TimeSlotRequest updateRequest = TimeSlotRequest.builder()
                .startTime(LocalTime.of(5, 30))
                .endTime(LocalTime.of(7, 0))
                .isPeakHour(false)
                .build();

        TimeSlot updatedSlot = TimeSlot.builder()
                .id(1L)
                .startTime(LocalTime.of(5, 30))
                .endTime(LocalTime.of(7, 0))
                .isPeakHour(false)
                .isActive(true)
                .build();

        TimeSlotResponse updatedResponse = TimeSlotResponse.builder()
                .id(1L)
                .startTime(LocalTime.of(5, 30))
                .endTime(LocalTime.of(7, 0))
                .isPeakHour(false)
                .isActive(true)
                .build();

        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(slot1));
        when(timeSlotRepository.existsOverlappingSlot(eq(1L), eq(updateRequest.getStartTime()), eq(updateRequest.getEndTime())))
                .thenReturn(false);
        when(timeSlotRepository.save(any(TimeSlot.class))).thenReturn(updatedSlot);
        when(timeSlotMapper.toTimeSlotResponse(updatedSlot)).thenReturn(updatedResponse);

        TimeSlotResponse result = timeSlotService.updateTimeSlot(1L, updateRequest);

        assertNotNull(result);
        assertEquals(LocalTime.of(5, 30), result.getStartTime());
        assertEquals(LocalTime.of(7, 0), result.getEndTime());
        verify(timeSlotRepository).save(any(TimeSlot.class));
    }

    @Test
    @DisplayName("UT-10: Chỉnh sửa khung giờ không tồn tại ném TIME_SLOT_NOT_FOUND")
    void updateTimeSlot_NotFound_ThrowsException() {
        when(timeSlotRepository.findById(99L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> timeSlotService.updateTimeSlot(99L, validRequest));

        assertEquals(ErrorCode.TIME_SLOT_NOT_FOUND, exception.getErrorCode());
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-11: Chỉnh sửa giờ bị chồng chéo với ca khác ném TIME_SLOT_OVERLAPPING")
    void updateTimeSlot_Overlapping_ThrowsException() {
        TimeSlotRequest overlapRequest = TimeSlotRequest.builder()
                .startTime(LocalTime.of(17, 0))
                .endTime(LocalTime.of(18, 30))
                .isPeakHour(false)
                .build();

        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(slot1));
        when(timeSlotRepository.existsOverlappingSlot(eq(1L), eq(overlapRequest.getStartTime()), eq(overlapRequest.getEndTime())))
                .thenReturn(true);

        AppException exception = assertThrows(AppException.class,
                () -> timeSlotService.updateTimeSlot(1L, overlapRequest));

        assertEquals(ErrorCode.TIME_SLOT_OVERLAPPING, exception.getErrorCode());
        verify(timeSlotRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-12: Chuyển đổi trạng thái Giờ thường sang Giờ vàng qua update thành công")
    void updateTimeSlot_ChangePeakHour_Success() {
        TimeSlotRequest peakRequest = TimeSlotRequest.builder()
                .startTime(LocalTime.of(6, 0))
                .endTime(LocalTime.of(7, 30))
                .isPeakHour(true)
                .build();

        TimeSlot peakSlot = TimeSlot.builder()
                .id(1L)
                .startTime(LocalTime.of(6, 0))
                .endTime(LocalTime.of(7, 30))
                .isPeakHour(true)
                .isActive(true)
                .build();

        TimeSlotResponse peakResponse = TimeSlotResponse.builder()
                .id(1L)
                .startTime(LocalTime.of(6, 0))
                .endTime(LocalTime.of(7, 30))
                .isPeakHour(true)
                .isActive(true)
                .build();

        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(slot1));
        when(timeSlotRepository.existsOverlappingSlot(eq(1L), eq(peakRequest.getStartTime()), eq(peakRequest.getEndTime())))
                .thenReturn(false);
        when(timeSlotRepository.save(any(TimeSlot.class))).thenReturn(peakSlot);
        when(timeSlotMapper.toTimeSlotResponse(peakSlot)).thenReturn(peakResponse);

        TimeSlotResponse result = timeSlotService.updateTimeSlot(1L, peakRequest);

        assertNotNull(result);
        assertTrue(result.getIsPeakHour());
        verify(timeSlotRepository).save(any(TimeSlot.class));
    }

    @Test
    @DisplayName("UT-13: Cập nhật giữ nguyên giờ (không bị lỗi overlap với chính nó)")
    void updateTimeSlot_SameTime_Success() {
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(slot1));
        when(timeSlotRepository.existsOverlappingSlot(eq(1L), eq(validRequest.getStartTime()), eq(validRequest.getEndTime())))
                .thenReturn(false);
        when(timeSlotRepository.save(any(TimeSlot.class))).thenReturn(slot1);
        when(timeSlotMapper.toTimeSlotResponse(slot1)).thenReturn(slotResponse1);

        TimeSlotResponse result = timeSlotService.updateTimeSlot(1L, validRequest);

        assertNotNull(result);
        assertEquals(LocalTime.of(6, 0), result.getStartTime());
        assertEquals(LocalTime.of(7, 30), result.getEndTime());
        verify(timeSlotRepository).save(any(TimeSlot.class));
    }

    // =========================================================================
    // UT-14 & UT-15: Xóa khung giờ
    // =========================================================================
    @Test
    @DisplayName("UT-14: Xóa khung giờ khi CÓ đơn đặt chưa hoàn tất ném TIME_SLOT_HAS_ACTIVE_BOOKINGS")
    void deleteTimeSlot_HasActiveBookings_ThrowsException() {
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(slot1));
        when(bookingRepository.existsByTimeSlotIdAndBookingDateGreaterThanEqualAndStatusInAndIsDeletedFalse(
                eq(1L), any(LocalDate.class), anyCollection()))
                .thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> timeSlotService.deleteTimeSlot(1L));

        assertEquals(ErrorCode.TIME_SLOT_HAS_ACTIVE_BOOKINGS, exception.getErrorCode());
        verify(timeSlotRepository, never()).save(any());
        assertTrue(slot1.getIsActive());
    }

    @Test
    @DisplayName("UT-15: Xóa khung giờ khi KHÔNG có đơn đặt nào thành công")
    void deleteTimeSlot_NoActiveBookings_Success() {
        when(timeSlotRepository.findById(1L)).thenReturn(Optional.of(slot1));
        when(bookingRepository.existsByTimeSlotIdAndBookingDateGreaterThanEqualAndStatusInAndIsDeletedFalse(
                eq(1L), any(LocalDate.class), anyCollection()))
                .thenReturn(false);

        timeSlotService.deleteTimeSlot(1L);

        assertFalse(slot1.getIsActive());
        verify(timeSlotRepository).save(slot1);
    }

    // =========================================================================
    // UT-16: Public API lấy danh sách khung giờ đang hoạt động
    // =========================================================================
    @Test
    @DisplayName("UT-16: Lấy danh sách khung giờ công khai (chỉ lấy active và sắp xếp theo startTime)")
    void getPublicTimeSlots_Success() {
        PublicTimeSlotResponse publicSlot = PublicTimeSlotResponse.builder()
                .id(1L)
                .startTime(LocalTime.of(6, 0))
                .endTime(LocalTime.of(7, 30))
                .isPeakHour(false)
                .build();

        when(timeSlotRepository.findAllByIsActiveTrueOrderByStartTimeAsc()).thenReturn(List.of(slot1, slot2));
        when(timeSlotMapper.toPublicTimeSlotResponseList(anyList())).thenReturn(List.of(publicSlot));

        List<PublicTimeSlotResponse> result = timeSlotService.getPublicTimeSlots();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(LocalTime.of(6, 0), result.get(0).getStartTime());
        verify(timeSlotRepository, times(1)).findAllByIsActiveTrueOrderByStartTimeAsc();
    }
}
