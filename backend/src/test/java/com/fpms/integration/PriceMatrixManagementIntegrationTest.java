package com.fpms.integration;

import com.fpms.dto.request.PriceRateItemRequest;
import com.fpms.dto.request.SavePitchTypePricingRequest;
import com.fpms.entity.PitchType;
import com.fpms.entity.PriceMatrix;
import com.fpms.entity.enums.DayType;
import com.fpms.exception.ErrorCode;
import com.fpms.repository.PitchTypeRepository;
import com.fpms.repository.PriceMatrixRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


class PriceMatrixManagementIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PriceMatrixRepository priceMatrixRepository;

    @Autowired
    private PitchTypeRepository pitchTypeRepository;

    private PitchType pitchType5;

    @BeforeEach
    void setUp() {
        // Tạo một PitchType mới hoàn toàn cho mỗi test method để bảo đảm 100% tính cô lập dữ liệu
        pitchType5 = pitchTypeRepository.save(PitchType.builder()
                .name("Sân 5 Test IT " + UUID.randomUUID())
                .playerCapacity(10)
                .description("Sân 5 tiêu chuẩn kiểm thử tích hợp")
                .build());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PRICE_01: [Happy Path] Lưu bảng giá 6 ô hoàn chỉnh cho Sân 5")
    void testSavePricingForPitchType_FullMatrix_Success() throws Exception {
        SavePitchTypePricingRequest request = SavePitchTypePricingRequest.builder()
                .rates(List.of(
                        new PriceRateItemRequest(DayType.WEEKDAY, false, new BigDecimal("200000.00")),
                        new PriceRateItemRequest(DayType.WEEKDAY, true, new BigDecimal("300000.00")),
                        new PriceRateItemRequest(DayType.WEEKEND, false, new BigDecimal("250000.00")),
                        new PriceRateItemRequest(DayType.WEEKEND, true, new BigDecimal("350000.00")),
                        new PriceRateItemRequest(DayType.HOLIDAY, false, new BigDecimal("300000.00")),
                        new PriceRateItemRequest(DayType.HOLIDAY, true, new BigDecimal("400000.00"))
                ))
                .build();

        mockMvc.perform(put("/api/v1/admin/pricing/pitch-types/{pitchTypeId}", pitchType5.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pitchTypeId").value(pitchType5.getId()))
                .andExpect(jsonPath("$.data.rates", hasSize(6)));

        // Nghiệm thu thực tế trong CSDL PostgreSQL
        List<PriceMatrix> savedRates = priceMatrixRepository.findAllByPitchTypeIdOrderByDayTypeAscIsPeakHourAsc(pitchType5.getId());
        assertEquals(6, savedRates.size());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PRICE_02: [Happy Path] Cơ chế Upsert: Cập nhật đè giá ô đã có giữ nguyên ID bản ghi")
    void testSavePricingForPitchType_UpsertInPlace_PreservesId() throws Exception {
        // 1. Tạo ô giá ban đầu
        PriceMatrix initialMatrix = priceMatrixRepository.save(PriceMatrix.builder()
                .pitchType(pitchType5)
                .dayType(DayType.WEEKDAY)
                .isPeakHour(false)
                .price(new BigDecimal("200000.00"))
                .build());
        Long initialId = initialMatrix.getId();

        // 2. Gửi request cập nhật mức giá mới cho ô này
        SavePitchTypePricingRequest updateRequest = SavePitchTypePricingRequest.builder()
                .rates(List.of(
                        new PriceRateItemRequest(DayType.WEEKDAY, false, new BigDecimal("250000.00"))
                ))
                .build();

        mockMvc.perform(put("/api/v1/admin/pricing/pitch-types/{pitchTypeId}", pitchType5.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rates[0].price").value(250000.00));

        // 3. Nghiệm thu: ID bản ghi giữ nguyên, mức giá trong CSDL được update
        PriceMatrix updatedMatrix = priceMatrixRepository.findById(initialId).orElse(null);
        assertNotNull(updatedMatrix);
        assertEquals(0, new BigDecimal("250000.00").compareTo(updatedMatrix.getPrice()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PRICE_03: [Happy Path] Lấy bảng giá theo Loại sân")
    void testGetPricingByPitchTypeId_Success() throws Exception {
        priceMatrixRepository.save(PriceMatrix.builder()
                .pitchType(pitchType5)
                .dayType(DayType.WEEKDAY)
                .isPeakHour(false)
                .price(new BigDecimal("180000.00"))
                .build());

        mockMvc.perform(get("/api/v1/admin/pricing/pitch-types/{pitchTypeId}", pitchType5.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pitchTypeId").value(pitchType5.getId()))
                .andExpect(jsonPath("$.data.rates[0].price").value(180000.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PRICE_04: [Happy Path] Lấy toàn bộ ma trận giá hệ thống")
    void testGetAllPricingMatrices_Success() throws Exception {
        priceMatrixRepository.save(PriceMatrix.builder()
                .pitchType(pitchType5)
                .dayType(DayType.WEEKDAY)
                .isPeakHour(false)
                .price(new BigDecimal("190000.00"))
                .build());

        mockMvc.perform(get("/api/v1/admin/pricing/matrix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PRICE_05: [Negative] Lưu giá với pitchTypeId không tồn tại")
    void testSavePricing_PitchTypeNotFound_Returns404() throws Exception {
        SavePitchTypePricingRequest request = SavePitchTypePricingRequest.builder()
                .rates(List.of(
                        new PriceRateItemRequest(DayType.WEEKDAY, false, new BigDecimal("200000.00"))
                ))
                .build();

        mockMvc.perform(put("/api/v1/admin/pricing/pitch-types/{pitchTypeId}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PITCH_TYPE_NOT_FOUND.getCode()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PRICE_06: [Negative] Danh sách rates rỗng")
    void testSavePricing_EmptyRates_ReturnsBadRequest() throws Exception {
        SavePitchTypePricingRequest request = SavePitchTypePricingRequest.builder()
                .rates(Collections.emptyList())
                .build();

        mockMvc.perform(put("/api/v1/admin/pricing/pitch-types/{pitchTypeId}", pitchType5.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors[0].message").value(ErrorCode.PRICE_ITEMS_REQUIRED.getMessage()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PRICE_07: [Negative] Thiếu trường dayType trong rate item")
    void testSavePricing_MissingDayType_ReturnsBadRequest() throws Exception {
        String invalidJson = """
                {
                    "rates": [
                        {
                            "dayType": null,
                            "isPeakHour": false,
                            "price": 200000.00
                        }
                    ]
                }
                """;

        mockMvc.perform(put("/api/v1/admin/pricing/pitch-types/{pitchTypeId}", pitchType5.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors[0].message").value(ErrorCode.DAY_TYPE_REQUIRED.getMessage()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PRICE_08: [Negative] Thiếu trường isPeakHour trong rate item")
    void testSavePricing_MissingIsPeakHour_ReturnsBadRequest() throws Exception {
        String invalidJson = """
                {
                    "rates": [
                        {
                            "dayType": "WEEKDAY",
                            "isPeakHour": null,
                            "price": 200000.00
                        }
                    ]
                }
                """;

        mockMvc.perform(put("/api/v1/admin/pricing/pitch-types/{pitchTypeId}", pitchType5.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors[0].message").value(ErrorCode.IS_PEAK_HOUR_REQUIRED.getMessage()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PRICE_09: [Boundary] Giá đúng bằng 1.000 VNĐ (Cận biên dưới)")
    void testSavePricing_MinPrice_Success() throws Exception {
        SavePitchTypePricingRequest request = SavePitchTypePricingRequest.builder()
                .rates(List.of(
                        new PriceRateItemRequest(DayType.WEEKDAY, false, new BigDecimal("1000.00"))
                ))
                .build();

        mockMvc.perform(put("/api/v1/admin/pricing/pitch-types/{pitchTypeId}", pitchType5.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rates[0].price").value(1000.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PRICE_10: [Boundary] Giá 999 VNĐ (Dưới cận biên tối thiểu)")
    void testSavePricing_BelowMinPrice_ReturnsBadRequest() throws Exception {
        SavePitchTypePricingRequest request = SavePitchTypePricingRequest.builder()
                .rates(List.of(
                        new PriceRateItemRequest(DayType.WEEKDAY, false, new BigDecimal("999.00"))
                ))
                .build();

        mockMvc.perform(put("/api/v1/admin/pricing/pitch-types/{pitchTypeId}", pitchType5.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors[0].message").value(ErrorCode.PRICE_INVALID.getMessage()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PRICE_11: [Boundary] Giá số âm (-50.000 VNĐ)")
    void testSavePricing_NegativePrice_ReturnsBadRequest() throws Exception {
        SavePitchTypePricingRequest request = SavePitchTypePricingRequest.builder()
                .rates(List.of(
                        new PriceRateItemRequest(DayType.WEEKDAY, false, new BigDecimal("-50000.00"))
                ))
                .build();

        mockMvc.perform(put("/api/v1/admin/pricing/pitch-types/{pitchTypeId}", pitchType5.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors[0].message").value(ErrorCode.PRICE_INVALID.getMessage()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PRICE_12: [Boundary] Giá đúng bằng 50.000.000 VNĐ (Cận biên trên)")
    void testSavePricing_MaxPrice_Success() throws Exception {
        SavePitchTypePricingRequest request = SavePitchTypePricingRequest.builder()
                .rates(List.of(
                        new PriceRateItemRequest(DayType.WEEKDAY, false, new BigDecimal("50000000.00"))
                ))
                .build();

        mockMvc.perform(put("/api/v1/admin/pricing/pitch-types/{pitchTypeId}", pitchType5.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rates[0].price").value(50000000.00));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PRICE_13: [Boundary] Giá 50.000.001 VNĐ (Vượt cận biên trên)")
    void testSavePricing_ExceedMaxPrice_ReturnsBadRequest() throws Exception {
        SavePitchTypePricingRequest request = SavePitchTypePricingRequest.builder()
                .rates(List.of(
                        new PriceRateItemRequest(DayType.WEEKDAY, false, new BigDecimal("50000001.00"))
                ))
                .build();

        mockMvc.perform(put("/api/v1/admin/pricing/pitch-types/{pitchTypeId}", pitchType5.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors[0].message").value(ErrorCode.PRICE_MAX_EXCEEDED.getMessage()));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("TC_PRICE_14: [Security] Khách hàng ROLE_CUSTOMER gọi API lưu giá")
    void testSavePricing_CustomerRole_Forbidden() throws Exception {
        SavePitchTypePricingRequest request = SavePitchTypePricingRequest.builder()
                .rates(List.of(
                        new PriceRateItemRequest(DayType.WEEKDAY, false, new BigDecimal("200000.00"))
                ))
                .build();

        mockMvc.perform(put("/api/v1/admin/pricing/pitch-types/{pitchTypeId}", pitchType5.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC_PRICE_15: [Security] Người dùng ẩn danh (Anonymous) gọi API")
    void testSavePricing_Anonymous_UnauthorizedOrForbidden() throws Exception {
        SavePitchTypePricingRequest request = SavePitchTypePricingRequest.builder()
                .rates(List.of(
                        new PriceRateItemRequest(DayType.WEEKDAY, false, new BigDecimal("200000.00"))
                ))
                .build();

        mockMvc.perform(put("/api/v1/admin/pricing/pitch-types/{pitchTypeId}", pitchType5.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> assertTrue(
                        result.getResponse().getStatus() == 401 || result.getResponse().getStatus() == 403,
                        "Expected 401 Unauthorized or 403 Forbidden but was " + result.getResponse().getStatus()
                ));
    }
}
