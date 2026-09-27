package com.fpms.controller;

import com.fpms.entity.Booking;
import com.fpms.entity.Pitch;
import com.fpms.entity.PitchType;
import com.fpms.entity.PriceMatrix;
import com.fpms.entity.TimeSlot;
import com.fpms.entity.enums.BookingStatus;
import com.fpms.entity.enums.DayType;
import com.fpms.exception.ErrorCode;
import com.fpms.exception.GlobalExceptionHandler;
import com.fpms.repository.BookingRepository;
import com.fpms.repository.HolidayRepository;
import com.fpms.repository.PriceMatrixRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private PriceMatrixRepository priceMatrixRepository;

    @InjectMocks
    private BookingController bookingController;

    private Pitch pitch1;
    private TimeSlot slot1;
    private PitchType pitchType5;
    private Booking booking1;
    private PriceMatrix priceMatrix1;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        pitchType5 = PitchType.builder()
                .name("Sân 5 người")
                .playerCapacity(10)
                .build();
        pitchType5.setId(1L);

        pitch1 = Pitch.builder()
                .name("Sân 5A")
                .pitchType(pitchType5)
                .build();
        pitch1.setId(10L);

        slot1 = TimeSlot.builder()
                .isPeakHour(false)
                .isActive(true)
                .build();
        slot1.setId(20L);

        booking1 = Booking.builder()
                .pitch(pitch1)
                .timeSlot(slot1)
                .status(BookingStatus.CONFIRMED)
                .build();

        priceMatrix1 = PriceMatrix.builder()
                .pitchType(pitchType5)
                .dayType(DayType.WEEKDAY)
                .isPeakHour(false)
                .price(BigDecimal.valueOf(200000))
                .build();
    }

    @Test
    @DisplayName("TC-U01: Lấy lưới lịch đặt thành công vào ngày thường (Weekday)")
    void getScheduleGrid_Weekday_Success() throws Exception {
        // Tìm ngày thứ Tư tiếp theo trong tương lai
        LocalDate nextWednesday = LocalDate.now().plusWeeks(1).with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));

        when(holidayRepository.existsByHolidayDate(nextWednesday)).thenReturn(false);
        when(bookingRepository.findOccupyingBookings(eq(nextWednesday), isNull())).thenReturn(List.of(booking1));
        when(priceMatrixRepository.findByDayType(DayType.WEEKDAY)).thenReturn(List.of(priceMatrix1));

        mockMvc.perform(get("/api/v1/bookings/schedule-grid")
                        .param("date", nextWednesday.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.bookings[0].pitchId").value(10))
                .andExpect(jsonPath("$.data.bookings[0].timeSlotId").value(20))
                .andExpect(jsonPath("$.data.bookings[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.prices[0].pitchTypeId").value(1))
                .andExpect(jsonPath("$.data.prices[0].isPeakHour").value(false))
                .andExpect(jsonPath("$.data.prices[0].price").value(200000));

        verify(holidayRepository, times(1)).existsByHolidayDate(nextWednesday);
        verify(bookingRepository, times(1)).findOccupyingBookings(nextWednesday, null);
        verify(priceMatrixRepository, times(1)).findByDayType(DayType.WEEKDAY);
    }

    @Test
    @DisplayName("TC-U02: Lấy lưới lịch đặt thành công vào cuối tuần (Weekend)")
    void getScheduleGrid_Weekend_Success() throws Exception {
        // Tìm ngày Thứ Bảy tiếp theo trong tương lai
        LocalDate nextSaturday = LocalDate.now().plusWeeks(1).with(TemporalAdjusters.next(DayOfWeek.SATURDAY));

        PriceMatrix weekendPrice = PriceMatrix.builder()
                .pitchType(pitchType5)
                .dayType(DayType.WEEKEND)
                .isPeakHour(false)
                .price(BigDecimal.valueOf(250000))
                .build();

        when(holidayRepository.existsByHolidayDate(nextSaturday)).thenReturn(false);
        when(bookingRepository.findOccupyingBookings(eq(nextSaturday), isNull())).thenReturn(Collections.emptyList());
        when(priceMatrixRepository.findByDayType(DayType.WEEKEND)).thenReturn(List.of(weekendPrice));

        mockMvc.perform(get("/api/v1/bookings/schedule-grid")
                        .param("date", nextSaturday.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookings").isArray())
                .andExpect(jsonPath("$.data.bookings.length()").value(0))
                .andExpect(jsonPath("$.data.prices[0].price").value(250000));

        verify(priceMatrixRepository, times(1)).findByDayType(DayType.WEEKEND);
    }

    @Test
    @DisplayName("TC-U03: Lấy lưới lịch đặt vào ngày Lễ ưu tiên DayType là HOLIDAY")
    void getScheduleGrid_Holiday_Success() throws Exception {
        LocalDate holidayDate = LocalDate.now().plusDays(5);

        PriceMatrix holidayPrice = PriceMatrix.builder()
                .pitchType(pitchType5)
                .dayType(DayType.HOLIDAY)
                .isPeakHour(false)
                .price(BigDecimal.valueOf(300000))
                .build();

        when(holidayRepository.existsByHolidayDate(holidayDate)).thenReturn(true);
        when(bookingRepository.findOccupyingBookings(eq(holidayDate), isNull())).thenReturn(Collections.emptyList());
        when(priceMatrixRepository.findByDayType(DayType.HOLIDAY)).thenReturn(List.of(holidayPrice));

        mockMvc.perform(get("/api/v1/bookings/schedule-grid")
                        .param("date", holidayDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.prices[0].price").value(300000));

        verify(priceMatrixRepository, times(1)).findByDayType(DayType.HOLIDAY);
    }

    @Test
    @DisplayName("TC-U04: Lọc lịch đặt theo pitchTypeId cụ thể")
    void getScheduleGrid_WithPitchTypeId_Success() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(2);
        Long pitchTypeId = 1L;

        when(holidayRepository.existsByHolidayDate(futureDate)).thenReturn(false);
        when(bookingRepository.findOccupyingBookings(eq(futureDate), eq(pitchTypeId))).thenReturn(List.of(booking1));
        when(priceMatrixRepository.findByDayType(any(DayType.class))).thenReturn(List.of(priceMatrix1));

        mockMvc.perform(get("/api/v1/bookings/schedule-grid")
                        .param("date", futureDate.toString())
                        .param("pitchTypeId", pitchTypeId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookings.length()").value(1));

        verify(bookingRepository, times(1)).findOccupyingBookings(futureDate, pitchTypeId);
    }

    @Test
    @DisplayName("TC-U05: Chặn tìm kiếm ngày trong quá khứ ném PAST_DATE_NOT_ALLOWED trả về 400")
    void getScheduleGrid_PastDate_ThrowsException() throws Exception {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        mockMvc.perform(get("/api/v1/bookings/schedule-grid")
                        .param("date", yesterday.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.PAST_DATE_NOT_ALLOWED.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.PAST_DATE_NOT_ALLOWED.getMessage()));

        verifyNoInteractions(bookingRepository);
        verifyNoInteractions(priceMatrixRepository);
    }

    @Test
    @DisplayName("TC-U06: Thiếu tham số bắt buộc date trả về 400 Bad Request")
    void getScheduleGrid_MissingDate_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/schedule-grid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(bookingRepository);
    }

    @Test
    @DisplayName("TC-U07: Tham số date sai định dạng trả về 400 Bad Request")
    void getScheduleGrid_InvalidDateFormat_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/bookings/schedule-grid")
                        .param("date", "not-a-date")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(bookingRepository);
    }
}
