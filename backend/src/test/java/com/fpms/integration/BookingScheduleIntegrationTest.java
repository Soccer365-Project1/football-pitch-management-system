package com.fpms.integration;

import com.fpms.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class BookingScheduleIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("TC-I01: Integration test lấy lưới lịch đặt và bảng giá thành công")
    void getScheduleGrid_Integration_Success() throws Exception {
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/v1/bookings/schedule-grid")
                        .param("date", today.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.bookings").isArray())
                .andExpect(jsonPath("$.data.prices").isArray());
    }

    @Test
    @DisplayName("TC-I02: Integration test chặn ngày trong quá khứ trả về 400 và PAST_DATE_NOT_ALLOWED")
    void getScheduleGrid_PastDate_Integration_Fails() throws Exception {
        LocalDate pastDate = LocalDate.now().minusDays(1);

        mockMvc.perform(get("/api/v1/bookings/schedule-grid")
                        .param("date", pastDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ErrorCode.PAST_DATE_NOT_ALLOWED.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.PAST_DATE_NOT_ALLOWED.getMessage()));
    }

    @Test
    @DisplayName("TC-I03: Integration test lấy danh sách sân bóng cho khách hàng")
    void getPitches_Integration_Success() throws Exception {
        mockMvc.perform(get("/api/v1/pitches")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("TC-I04: Integration test lấy danh sách loại sân bóng")
    void getPitchTypes_Integration_Success() throws Exception {
        mockMvc.perform(get("/api/v1/pitches/types")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("TC-I05: Integration test lấy danh sách khung giờ công khai")
    void getTimeSlots_Integration_Success() throws Exception {
        mockMvc.perform(get("/api/v1/timeslots")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }
}
