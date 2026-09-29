package com.fpms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fpms.dto.request.BookingCreationRequest;
import com.fpms.dto.response.BookingResponse;
import com.fpms.dto.response.ScheduleGridItemResponse;
import com.fpms.dto.response.ScheduleGridResponse;
import com.fpms.dto.response.PriceItemResponse;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.exception.GlobalExceptionHandler;
import com.fpms.security.UserPrincipal;
import com.fpms.service.BookingService;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("TC-C01: POST /api/v1/bookings - Đặt sân thành công")
    void createBooking_Success() throws Exception {
        BookingCreationRequest request = new BookingCreationRequest();
        request.setPitchId(1L);
        request.setTimeSlotId(1L);
        request.setBookingDate(LocalDate.now().plusDays(1));
        request.setGuestName("Nguyen Van A");
        request.setGuestPhone("0901234567");

        BookingResponse response = new BookingResponse();
        response.setId(100L);

        when(bookingService.createBooking(any(BookingCreationRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đặt sân thành công"))
                .andExpect(jsonPath("$.data.id").value(100));

        verify(bookingService, times(1)).createBooking(any(BookingCreationRequest.class), any());
    }

    @Test
    @DisplayName("TC-C02: POST /api/v1/bookings - Lỗi Validation (Thiếu pitchId)")
    void createBooking_MissingPitchId_BadRequest() throws Exception {
        BookingCreationRequest request = new BookingCreationRequest();
        request.setTimeSlotId(1L);
        request.setBookingDate(LocalDate.now().plusDays(1));
        request.setGuestName("Nguyen Van A");
        request.setGuestPhone("0901234567");

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verifyNoInteractions(bookingService);
    }

    @Test
    @DisplayName("TC-C03: GET /api/v1/bookings/schedule-grid - Lấy lịch thành công")
    void getScheduleGrid_Success() throws Exception {
        LocalDate date = LocalDate.now().plusDays(1);
        
        ScheduleGridResponse gridResponse = new ScheduleGridResponse();
        gridResponse.setBookings(List.of(new ScheduleGridItemResponse(1L, 1L, "CONFIRMED")));
        gridResponse.setPrices(List.of(new PriceItemResponse(1L, true, BigDecimal.valueOf(200000))));

        when(bookingService.getScheduleGrid(eq(date), isNull())).thenReturn(gridResponse);

        mockMvc.perform(get("/api/v1/bookings/schedule-grid")
                        .param("date", date.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookings[0].pitchId").value(1))
                .andExpect(jsonPath("$.data.bookings[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.prices[0].price").value(200000));
    }

    @Test
    @DisplayName("TC-C04: GET /api/v1/bookings/schedule-grid - Lỗi ngày trong quá khứ")
    void getScheduleGrid_PastDate_ThrowsException() throws Exception {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        
        when(bookingService.getScheduleGrid(eq(pastDate), isNull()))
                .thenThrow(new AppException(ErrorCode.PAST_DATE_NOT_ALLOWED));

        mockMvc.perform(get("/api/v1/bookings/schedule-grid")
                        .param("date", pastDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.PAST_DATE_NOT_ALLOWED.getCode()));
    }
}
