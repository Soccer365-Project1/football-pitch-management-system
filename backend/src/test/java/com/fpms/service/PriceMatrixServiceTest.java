package com.fpms.service;

import com.fpms.dto.request.PriceRateItemRequest;
import com.fpms.dto.request.SavePitchTypePricingRequest;
import com.fpms.dto.response.PitchTypePricingMatrixResponse;
import com.fpms.dto.response.PriceMatrixResponse;
import com.fpms.dto.response.PriceRateResponse;
import com.fpms.entity.PitchType;
import com.fpms.entity.PriceMatrix;
import com.fpms.entity.enums.DayType;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.mapper.PriceMatrixMapper;
import com.fpms.repository.PitchTypeRepository;
import com.fpms.repository.PriceMatrixRepository;
import com.fpms.service.impl.PriceMatrixServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceMatrixServiceTest {

    @Mock
    private PriceMatrixRepository priceMatrixRepository;

    @Mock
    private PitchTypeRepository pitchTypeRepository;

    @Mock
    private PriceMatrixMapper priceMatrixMapper;

    @InjectMocks
    private PriceMatrixServiceImpl priceMatrixService;

    private PitchType pitchType5;
    private PriceMatrix priceMatrixWeekdayRegular;
    private PriceRateResponse priceRateResponseWeekdayRegular;

    @BeforeEach
    void setUp() {
        pitchType5 = PitchType.builder()
                .name("Sân 5 người")
                .playerCapacity(10)
                .build();
        pitchType5.setId(1L);

        priceMatrixWeekdayRegular = PriceMatrix.builder()
                .pitchType(pitchType5)
                .dayType(DayType.WEEKDAY)
                .isPeakHour(false)
                .price(new BigDecimal("200000.00"))
                .build();
        priceMatrixWeekdayRegular.setId(101L);

        priceRateResponseWeekdayRegular = PriceRateResponse.builder()
                .id(101L)
                .dayType(DayType.WEEKDAY)
                .isPeakHour(false)
                .price(new BigDecimal("200000.00"))
                .build();
    }

    @Test
    @DisplayName("TC01: Lấy bảng giá loại sân thành công khi đã có dữ liệu")
    void getPricingByPitchTypeId_Success_HasExistingRates() {
        when(pitchTypeRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(pitchType5));
        when(priceMatrixRepository.findAllByPitchTypeIdOrderByDayTypeAscIsPeakHourAsc(1L))
                .thenReturn(List.of(priceMatrixWeekdayRegular));
        when(priceMatrixMapper.toPriceRateResponseList(anyList()))
                .thenReturn(List.of(priceRateResponseWeekdayRegular));

        PitchTypePricingMatrixResponse response = priceMatrixService.getPricingByPitchTypeId(1L);

        assertNotNull(response);
        assertEquals(1L, response.getPitchTypeId());
        assertEquals("Sân 5 người", response.getPitchTypeName());
        assertEquals(10, response.getPlayerCapacity());
        assertEquals(1, response.getRates().size());
        assertEquals(new BigDecimal("200000.00"), response.getRates().get(0).getPrice());
        verify(pitchTypeRepository, times(1)).findByIdAndIsDeletedFalse(1L);
    }

    @Test
    @DisplayName("TC02: Lấy bảng giá loại sân thành công khi chưa có dữ liệu (trả về rates rỗng)")
    void getPricingByPitchTypeId_Success_EmptyRates() {
        when(pitchTypeRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(pitchType5));
        when(priceMatrixRepository.findAllByPitchTypeIdOrderByDayTypeAscIsPeakHourAsc(1L))
                .thenReturn(Collections.emptyList());
        when(priceMatrixMapper.toPriceRateResponseList(Collections.emptyList()))
                .thenReturn(Collections.emptyList());

        PitchTypePricingMatrixResponse response = priceMatrixService.getPricingByPitchTypeId(1L);

        assertNotNull(response);
        assertEquals(1L, response.getPitchTypeId());
        assertTrue(response.getRates().isEmpty());
    }

    @Test
    @DisplayName("TC03: Lấy bảng giá loại sân thất bại khi loại sân không tồn tại")
    void getPricingByPitchTypeId_PitchTypeNotFound() {
        when(pitchTypeRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> priceMatrixService.getPricingByPitchTypeId(999L));
        assertEquals(ErrorCode.PITCH_TYPE_NOT_FOUND, ex.getErrorCode());
        verify(priceMatrixRepository, never()).findAllByPitchTypeIdOrderByDayTypeAscIsPeakHourAsc(anyLong());
    }

    @Test
    @DisplayName("TC04: Lưu bảng giá (Upsert) - Tạo mới ô giá khi chưa có trong CSDL")
    void savePricingForPitchType_InsertNewRates() {
        SavePitchTypePricingRequest request = SavePitchTypePricingRequest.builder()
                .rates(List.of(
                        PriceRateItemRequest.builder()
                                .dayType(DayType.WEEKDAY)
                                .isPeakHour(false)
                                .price(new BigDecimal("200000.00"))
                                .build()
                ))
                .build();

        when(pitchTypeRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(pitchType5));
        when(priceMatrixRepository.findByPitchTypeIdAndIsPeakHourAndDayType(1L, false, DayType.WEEKDAY))
                .thenReturn(Optional.empty());
        when(priceMatrixRepository.saveAll(anyList())).thenReturn(List.of(priceMatrixWeekdayRegular));
        when(priceMatrixMapper.toPriceRateResponseList(anyList()))
                .thenReturn(List.of(priceRateResponseWeekdayRegular));

        PitchTypePricingMatrixResponse response = priceMatrixService.savePricingForPitchType(1L, request);

        assertNotNull(response);
        assertEquals(1L, response.getPitchTypeId());
        assertEquals(1, response.getRates().size());
        verify(priceMatrixRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("TC05: Lưu bảng giá (Upsert) - Cập nhật giá ô đã tồn tại trong CSDL")
    void savePricingForPitchType_UpdateExistingRates() {
        SavePitchTypePricingRequest request = SavePitchTypePricingRequest.builder()
                .rates(List.of(
                        PriceRateItemRequest.builder()
                                .dayType(DayType.WEEKDAY)
                                .isPeakHour(false)
                                .price(new BigDecimal("250000.00"))
                                .build()
                ))
                .build();

        when(pitchTypeRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(pitchType5));
        when(priceMatrixRepository.findByPitchTypeIdAndIsPeakHourAndDayType(1L, false, DayType.WEEKDAY))
                .thenReturn(Optional.of(priceMatrixWeekdayRegular));
        when(priceMatrixRepository.saveAll(anyList())).thenReturn(List.of(priceMatrixWeekdayRegular));
        when(priceMatrixMapper.toPriceRateResponseList(anyList()))
                .thenReturn(List.of(priceRateResponseWeekdayRegular));

        PitchTypePricingMatrixResponse response = priceMatrixService.savePricingForPitchType(1L, request);

        assertNotNull(response);
        assertEquals(new BigDecimal("250000.00"), priceMatrixWeekdayRegular.getPrice());
        verify(priceMatrixRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("TC06: Lưu bảng giá thất bại khi loại sân không tồn tại")
    void savePricingForPitchType_PitchTypeNotFound() {
        SavePitchTypePricingRequest request = SavePitchTypePricingRequest.builder()
                .rates(List.of(
                        PriceRateItemRequest.builder()
                                .dayType(DayType.WEEKDAY)
                                .isPeakHour(false)
                                .price(new BigDecimal("200000.00"))
                                .build()
                ))
                .build();

        when(pitchTypeRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> priceMatrixService.savePricingForPitchType(999L, request));
        assertEquals(ErrorCode.PITCH_TYPE_NOT_FOUND, ex.getErrorCode());
        verify(priceMatrixRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("TC07: Xóa 1 ô giá thành công")
    void deleteSinglePrice_Success() {
        when(priceMatrixRepository.existsById(1L)).thenReturn(true);

        priceMatrixService.deleteSinglePrice(1L);

        verify(priceMatrixRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("TC08: Xóa 1 ô giá thất bại khi ID không tồn tại")
    void deleteSinglePrice_NotFound() {
        when(priceMatrixRepository.existsById(999L)).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> priceMatrixService.deleteSinglePrice(999L));
        assertEquals(ErrorCode.PRICE_MATRIX_NOT_FOUND, ex.getErrorCode());
        verify(priceMatrixRepository, never()).deleteById(anyLong());
    }
}
