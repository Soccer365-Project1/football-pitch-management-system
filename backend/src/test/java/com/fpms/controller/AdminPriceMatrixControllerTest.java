package com.fpms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpms.dto.request.PriceRateItemRequest;
import com.fpms.dto.request.SavePitchTypePricingRequest;
import com.fpms.dto.response.PitchTypePricingMatrixResponse;
import com.fpms.dto.response.PriceRateResponse;
import com.fpms.entity.enums.DayType;
import com.fpms.exception.ErrorCode;
import com.fpms.exception.GlobalExceptionHandler;
import com.fpms.service.PriceMatrixService;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminPriceMatrixControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PriceMatrixService priceMatrixService;

    @InjectMocks
    private AdminPriceMatrixController adminPriceMatrixController;

    private PitchTypePricingMatrixResponse sampleMatrixResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminPriceMatrixController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();

        sampleMatrixResponse = PitchTypePricingMatrixResponse.builder()
                .pitchTypeId(1L)
                .pitchTypeName("Sân 5 người")
                .playerCapacity(10)
                .rates(List.of(
                        PriceRateResponse.builder()
                                .id(1L)
                                .dayType(DayType.WEEKDAY)
                                .isPeakHour(false)
                                .price(new BigDecimal("200000.00"))
                                .build()
                ))
                .build();
    }

    @Test
    @DisplayName("API GET /api/v1/admin/pricing/pitch-types/{id} - Lấy bảng giá theo loại sân thành công")
    void getPricingByPitchTypeId_Success() throws Exception {
        when(priceMatrixService.getPricingByPitchTypeId(1L)).thenReturn(sampleMatrixResponse);

        mockMvc.perform(get("/api/v1/admin/pricing/pitch-types/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pitchTypeId").value(1))
                .andExpect(jsonPath("$.data.pitchTypeName").value("Sân 5 người"))
                .andExpect(jsonPath("$.data.rates[0].price").value(200000.00));

        verify(priceMatrixService, times(1)).getPricingByPitchTypeId(1L);
    }

    @Test
    @DisplayName("API PUT /api/v1/admin/pricing/pitch-types/{id} - Lưu bảng giá theo loại sân thành công")
    void savePricingForPitchType_Success() throws Exception {
        SavePitchTypePricingRequest request = SavePitchTypePricingRequest.builder()
                .rates(List.of(
                        PriceRateItemRequest.builder()
                                .dayType(DayType.WEEKDAY)
                                .isPeakHour(false)
                                .price(new BigDecimal("200000.00"))
                                .build()
                ))
                .build();

        when(priceMatrixService.savePricingForPitchType(eq(1L), any(SavePitchTypePricingRequest.class)))
                .thenReturn(sampleMatrixResponse);

        mockMvc.perform(put("/api/v1/admin/pricing/pitch-types/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pitchTypeId").value(1));

        verify(priceMatrixService, times(1)).savePricingForPitchType(eq(1L), any(SavePitchTypePricingRequest.class));
    }

    @Test
    @DisplayName("API PUT /api/v1/admin/pricing/pitch-types/{id} - Lỗi validation khi mảng rates rỗng")
    void savePricingForPitchType_Validation_EmptyRates() throws Exception {
        SavePitchTypePricingRequest request = SavePitchTypePricingRequest.builder()
                .rates(Collections.emptyList())
                .build();

        mockMvc.perform(put("/api/v1/admin/pricing/pitch-types/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message").value(ErrorCode.PRICE_ITEMS_REQUIRED.getMessage()));

        verify(priceMatrixService, never()).savePricingForPitchType(anyLong(), any());
    }

    @Test
    @DisplayName("API PUT /api/v1/admin/pricing/pitch-types/{id} - Lỗi validation khi giá < 1000")
    void savePricingForPitchType_Validation_PriceBelow1000() throws Exception {
        SavePitchTypePricingRequest request = SavePitchTypePricingRequest.builder()
                .rates(List.of(
                        PriceRateItemRequest.builder()
                                .dayType(DayType.WEEKDAY)
                                .isPeakHour(false)
                                .price(new BigDecimal("500"))
                                .build()
                ))
                .build();

        mockMvc.perform(put("/api/v1/admin/pricing/pitch-types/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message").value(ErrorCode.PRICE_INVALID.getMessage()));

        verify(priceMatrixService, never()).savePricingForPitchType(anyLong(), any());
    }

    @Test
    @DisplayName("API GET /api/v1/admin/pricing/matrix - Lấy ma trận biểu giá toàn bộ loại sân thành công")
    void getAllPricingMatrices_Success() throws Exception {
        when(priceMatrixService.getAllPricingMatrices()).thenReturn(List.of(sampleMatrixResponse));

        mockMvc.perform(get("/api/v1/admin/pricing/matrix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].pitchTypeId").value(1));

        verify(priceMatrixService, times(1)).getAllPricingMatrices();
    }

    @Test
    @DisplayName("API DELETE /api/v1/admin/pricing/{id} - Xóa ô giá thành công")
    void deleteSinglePrice_Success() throws Exception {
        doNothing().when(priceMatrixService).deleteSinglePrice(1L);

        mockMvc.perform(delete("/api/v1/admin/pricing/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Xóa cấu hình giá thành công"));

        verify(priceMatrixService, times(1)).deleteSinglePrice(1L);
    }
}
