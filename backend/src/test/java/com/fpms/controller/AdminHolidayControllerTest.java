package com.fpms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fpms.dto.request.HolidayRequest;
import com.fpms.dto.response.HolidayResponse;
import com.fpms.exception.ErrorCode;
import com.fpms.exception.GlobalExceptionHandler;
import com.fpms.service.HolidayService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminHolidayControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private HolidayService holidayService;

    @InjectMocks
    private AdminHolidayController adminHolidayController;

    private HolidayResponse holidayResponse1;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminHolidayController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        holidayResponse1 = HolidayResponse.builder()
                .id(1L)
                .holidayDate(LocalDate.of(2026, 9, 2))
                .name("Quốc khánh 2/9")
                .description("Nghỉ Quốc khánh")
                .build();
    }

    @Test
    @DisplayName("API GET /api/v1/admin/holidays - Thành công trả về 200 OK")
    void getAllHolidays_Success() throws Exception {
        when(holidayService.getAllHolidays(null)).thenReturn(List.of(holidayResponse1));

        mockMvc.perform(get("/api/v1/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Quốc khánh 2/9"))
                .andExpect(jsonPath("$.data[0].holidayDate").value("2026-09-02"));

        verify(holidayService, times(1)).getAllHolidays(null);
    }

    @Test
    @DisplayName("API GET /api/v1/admin/holidays/{id} - Thành công trả về 200 OK")
    void getHolidayById_Success() throws Exception {
        when(holidayService.getHolidayById(1L)).thenReturn(holidayResponse1);

        mockMvc.perform(get("/api/v1/admin/holidays/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Quốc khánh 2/9"));

        verify(holidayService, times(1)).getHolidayById(1L);
    }

    @Test
    @DisplayName("API POST /api/v1/admin/holidays - Tạo mới thành công trả về 201 CREATED")
    void createHoliday_Success() throws Exception {
        HolidayRequest request = HolidayRequest.builder()
                .holidayDate(LocalDate.of(2026, 9, 2))
                .name("Quốc khánh 2/9")
                .description("Nghỉ Quốc khánh")
                .build();

        when(holidayService.createHoliday(any(HolidayRequest.class))).thenReturn(holidayResponse1);

        mockMvc.perform(post("/api/v1/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(holidayService, times(1)).createHoliday(any(HolidayRequest.class));
    }

    @Test
    @DisplayName("API POST /api/v1/admin/holidays - Validation lỗi khi thiếu holidayDate, message lấy từ ErrorCode")
    void createHoliday_Validation_MissingDate() throws Exception {
        HolidayRequest request = HolidayRequest.builder()
                .holidayDate(null)
                .name("Quốc khánh 2/9")
                .build();

        mockMvc.perform(post("/api/v1/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("holidayDate"))
                .andExpect(jsonPath("$.errors[0].message").value(ErrorCode.HOLIDAY_DATE_REQUIRED.getMessage()));

        verify(holidayService, never()).createHoliday(any());
    }

    @Test
    @DisplayName("API POST /api/v1/admin/holidays - Validation lỗi khi thiếu name, message lấy từ ErrorCode")
    void createHoliday_Validation_BlankName() throws Exception {
        HolidayRequest request = HolidayRequest.builder()
                .holidayDate(LocalDate.of(2026, 9, 2))
                .name("")
                .build();

        mockMvc.perform(post("/api/v1/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value(ErrorCode.HOLIDAY_NAME_REQUIRED.getMessage()));

        verify(holidayService, never()).createHoliday(any());
    }

    @Test
    @DisplayName("API PUT /api/v1/admin/holidays/{id} - Cập nhật thành công trả về 200 OK")
    void updateHoliday_Success() throws Exception {
        HolidayRequest request = HolidayRequest.builder()
                .holidayDate(LocalDate.of(2026, 9, 2))
                .name("Quốc khánh 2/9 (Đã sửa)")
                .description("Nghỉ Quốc khánh")
                .build();

        when(holidayService.updateHoliday(eq(1L), any(HolidayRequest.class))).thenReturn(holidayResponse1);

        mockMvc.perform(put("/api/v1/admin/holidays/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(holidayService, times(1)).updateHoliday(eq(1L), any(HolidayRequest.class));
    }

    @Test
    @DisplayName("API DELETE /api/v1/admin/holidays/{id} - Xóa thành công trả về 200 OK")
    void deleteHoliday_Success() throws Exception {
        doNothing().when(holidayService).deleteHoliday(1L);

        mockMvc.perform(delete("/api/v1/admin/holidays/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(holidayService, times(1)).deleteHoliday(1L);
    }
}
