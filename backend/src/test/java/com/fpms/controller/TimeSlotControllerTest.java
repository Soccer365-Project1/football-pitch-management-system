package com.fpms.controller;

import com.fpms.dto.response.PublicTimeSlotResponse;
import com.fpms.exception.GlobalExceptionHandler;
import com.fpms.service.TimeSlotService;
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

import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TimeSlotControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TimeSlotService timeSlotService;

    @InjectMocks
    private TimeSlotController timeSlotController;

    private PublicTimeSlotResponse timeSlotResponse1;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(timeSlotController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        timeSlotResponse1 = PublicTimeSlotResponse.builder()
                .id(1L)
                .startTime(LocalTime.of(17, 30))
                .endTime(LocalTime.of(19, 0))
                .isPeakHour(true)
                .build();
    }

    @Test
    @DisplayName("TC-U11: Lấy danh sách khung giờ công khai thành công trả về 200 OK")
    void getActiveTimeSlots_Success() throws Exception {
        when(timeSlotService.getPublicTimeSlots()).thenReturn(List.of(timeSlotResponse1));

        mockMvc.perform(get("/api/v1/timeslots")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].startTime").value("17:30"))
                .andExpect(jsonPath("$.data[0].endTime").value("19:00"))
                .andExpect(jsonPath("$.data[0].isPeakHour").value(true));

        verify(timeSlotService, times(1)).getPublicTimeSlots();
    }
}
