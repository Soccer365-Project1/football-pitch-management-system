package com.fpms.controller;

import com.fpms.dto.response.PitchTypeResponse;
import com.fpms.dto.response.PublicPitchResponse;
import com.fpms.entity.enums.PitchStatus;
import com.fpms.exception.GlobalExceptionHandler;
import com.fpms.service.PitchService;
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

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PitchControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PitchService pitchService;

    @InjectMocks
    private PitchController pitchController;

    private PublicPitchResponse pitchResponse1;
    private PitchTypeResponse pitchTypeResponse5;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pitchController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        pitchTypeResponse5 = PitchTypeResponse.builder()
                .id(1L)
                .name("Sân 5 người")
                .playerCapacity(10)
                .build();

        pitchResponse1 = PublicPitchResponse.builder()
                .id(10L)
                .name("Sân 5A")
                .pitchType(pitchTypeResponse5)
                .status(PitchStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("TC-U08: Lấy tất cả sân hoạt động không truyền pitchTypeId thành công")
    void getActivePitches_WithoutFilter_Success() throws Exception {
        when(pitchService.getActivePitches(null)).thenReturn(List.of(pitchResponse1));

        mockMvc.perform(get("/api/v1/pitches")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].name").value("Sân 5A"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].pitchType.name").value("Sân 5 người"));

        verify(pitchService, times(1)).getActivePitches(null);
    }

    @Test
    @DisplayName("TC-U09: Lấy danh sách sân lọc theo pitchTypeId thành công")
    void getActivePitches_WithPitchTypeId_Success() throws Exception {
        when(pitchService.getActivePitches(1L)).thenReturn(List.of(pitchResponse1));

        mockMvc.perform(get("/api/v1/pitches")
                        .param("pitchTypeId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(10));

        verify(pitchService, times(1)).getActivePitches(1L);
    }

    @Test
    @DisplayName("TC-U10: Lấy tất cả loại sân qua /api/v1/pitches/types thành công")
    void getPitchTypes_Success() throws Exception {
        when(pitchService.getAllPitchTypes()).thenReturn(List.of(pitchTypeResponse5));

        mockMvc.perform(get("/api/v1/pitches/types")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Sân 5 người"))
                .andExpect(jsonPath("$.data[0].playerCapacity").value(10));

        verify(pitchService, times(1)).getAllPitchTypes();
    }
}
