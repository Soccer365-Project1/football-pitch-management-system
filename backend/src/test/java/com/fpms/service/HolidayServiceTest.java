package com.fpms.service;

import com.fpms.dto.request.HolidayRequest;
import com.fpms.dto.response.HolidayResponse;
import com.fpms.entity.Holiday;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.mapper.HolidayMapper;
import com.fpms.repository.HolidayRepository;
import com.fpms.service.impl.HolidayServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private HolidayMapper holidayMapper;

    @InjectMocks
    private HolidayServiceImpl holidayService;

    private Holiday holiday1;
    private Holiday holiday2;
    private HolidayResponse holidayResponse1;
    private HolidayResponse holidayResponse2;
    private HolidayRequest validRequest;

    @BeforeEach
    void setUp() {
        holiday1 = Holiday.builder()
                .id(1L)
                .holidayDate(LocalDate.of(2026, 1, 1))
                .name("Tết Dương Lịch")
                .description("Nghỉ Tết Dương Lịch")
                .build();

        holiday2 = Holiday.builder()
                .id(2L)
                .holidayDate(LocalDate.of(2026, 4, 30))
                .name("Ngày Giải Phóng Miền Nam")
                .description("Nghỉ 30/4")
                .build();

        holidayResponse1 = HolidayResponse.builder()
                .id(1L)
                .holidayDate(LocalDate.of(2026, 1, 1))
                .name("Tết Dương Lịch")
                .description("Nghỉ Tết Dương Lịch")
                .build();

        holidayResponse2 = HolidayResponse.builder()
                .id(2L)
                .holidayDate(LocalDate.of(2026, 4, 30))
                .name("Ngày Giải Phóng Miền Nam")
                .description("Nghỉ 30/4")
                .build();

        validRequest = HolidayRequest.builder()
                .holidayDate(LocalDate.of(2026, 9, 2))
                .name("Quốc Khánh 2/9")
                .description("Nghỉ lễ Quốc Khánh")
                .build();
    }

    // =========================================================================
    // 1. getAllHolidays()
    // =========================================================================

    @Test
    @DisplayName("TC01: Lấy danh sách ngày lễ không lọc năm - Thành công")
    void getAllHolidays_WithoutYear_Success() {
        // Arrange
        List<Holiday> holidays = List.of(holiday1, holiday2);
        List<HolidayResponse> expectedResponses = List.of(holidayResponse1, holidayResponse2);

        when(holidayRepository.findAllByOrderByHolidayDateAsc()).thenReturn(holidays);
        when(holidayMapper.toHolidayResponseList(holidays)).thenReturn(expectedResponses);

        // Act
        List<HolidayResponse> actualResponses = holidayService.getAllHolidays(null);

        // Assert
        assertNotNull(actualResponses);
        assertEquals(2, actualResponses.size());
        assertEquals("Tết Dương Lịch", actualResponses.get(0).getName());
        verify(holidayRepository, times(1)).findAllByOrderByHolidayDateAsc();
        verify(holidayRepository, never()).findAllByYearOrderByHolidayDateAsc(anyInt());
    }

    @Test
    @DisplayName("TC02: Lấy danh sách ngày lễ có lọc năm - Thành công")
    void getAllHolidays_WithYear_Success() {
        // Arrange
        int year = 2026;
        List<Holiday> holidays = List.of(holiday1, holiday2);
        List<HolidayResponse> expectedResponses = List.of(holidayResponse1, holidayResponse2);

        when(holidayRepository.findAllByYearOrderByHolidayDateAsc(year)).thenReturn(holidays);
        when(holidayMapper.toHolidayResponseList(holidays)).thenReturn(expectedResponses);

        // Act
        List<HolidayResponse> actualResponses = holidayService.getAllHolidays(year);

        // Assert
        assertNotNull(actualResponses);
        assertEquals(2, actualResponses.size());
        verify(holidayRepository, times(1)).findAllByYearOrderByHolidayDateAsc(year);
        verify(holidayRepository, never()).findAllByOrderByHolidayDateAsc();
    }

    // =========================================================================
    // 2. getHolidayById()
    // =========================================================================

    @Test
    @DisplayName("TC03: Lấy chi tiết ngày lễ theo ID - Thành công")
    void getHolidayById_Success() {
        // Arrange
        when(holidayRepository.findById(1L)).thenReturn(Optional.of(holiday1));
        when(holidayMapper.toHolidayResponse(holiday1)).thenReturn(holidayResponse1);

        // Act
        HolidayResponse response = holidayService.getHolidayById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Tết Dương Lịch", response.getName());
        verify(holidayRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("TC04: Lấy chi tiết ngày lễ theo ID không tồn tại - Ném ngoại lệ HOLIDAY_NOT_FOUND")
    void getHolidayById_NotFound() {
        // Arrange
        when(holidayRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> holidayService.getHolidayById(999L));
        assertEquals(ErrorCode.HOLIDAY_NOT_FOUND, exception.getErrorCode());
        verify(holidayRepository, times(1)).findById(999L);
        verify(holidayMapper, never()).toHolidayResponse(any());
    }

    // =========================================================================
    // 3. createHoliday()
    // =========================================================================

    @Test
    @DisplayName("TC05: Thêm mới ngày lễ hợp lệ - Thành công")
    void createHoliday_Success() {
        // Arrange
        Holiday newHoliday = Holiday.builder()
                .id(3L)
                .holidayDate(validRequest.getHolidayDate())
                .name(validRequest.getName())
                .description(validRequest.getDescription())
                .build();

        HolidayResponse expectedResponse = HolidayResponse.builder()
                .id(3L)
                .holidayDate(validRequest.getHolidayDate())
                .name(validRequest.getName())
                .description(validRequest.getDescription())
                .build();

        when(holidayRepository.existsByHolidayDate(validRequest.getHolidayDate())).thenReturn(false);
        when(holidayMapper.toHoliday(validRequest)).thenReturn(newHoliday);
        when(holidayRepository.save(newHoliday)).thenReturn(newHoliday);
        when(holidayMapper.toHolidayResponse(newHoliday)).thenReturn(expectedResponse);

        // Act
        HolidayResponse response = holidayService.createHoliday(validRequest);

        // Assert
        assertNotNull(response);
        assertEquals(3L, response.getId());
        assertEquals(validRequest.getName(), response.getName());
        verify(holidayRepository, times(1)).existsByHolidayDate(validRequest.getHolidayDate());
        verify(holidayRepository, times(1)).save(newHoliday);
    }

    @Test
    @DisplayName("TC06: Thêm mới ngày lễ bị trùng ngày - Ném ngoại lệ HOLIDAY_DATE_ALREADY_EXISTS")
    void createHoliday_DuplicateDate() {
        // Arrange
        when(holidayRepository.existsByHolidayDate(validRequest.getHolidayDate())).thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> holidayService.createHoliday(validRequest));
        assertEquals(ErrorCode.HOLIDAY_DATE_ALREADY_EXISTS, exception.getErrorCode());
        verify(holidayRepository, times(1)).existsByHolidayDate(validRequest.getHolidayDate());
        verify(holidayRepository, never()).save(any());
    }

    // =========================================================================
    // 4. updateHoliday()
    // =========================================================================

    @Test
    @DisplayName("TC07: Cập nhật ngày lễ hợp lệ - Thành công")
    void updateHoliday_Success() {
        // Arrange
        HolidayRequest updateRequest = HolidayRequest.builder()
                .holidayDate(LocalDate.of(2026, 1, 1))
                .name("Tết Dương Lịch 2026 Đã Đổi Tên")
                .description("Cập nhật mô tả")
                .build();

        Holiday updatedHoliday = Holiday.builder()
                .id(1L)
                .holidayDate(updateRequest.getHolidayDate())
                .name(updateRequest.getName())
                .description(updateRequest.getDescription())
                .build();

        HolidayResponse expectedResponse = HolidayResponse.builder()
                .id(1L)
                .holidayDate(updateRequest.getHolidayDate())
                .name(updateRequest.getName())
                .description(updateRequest.getDescription())
                .build();

        when(holidayRepository.findById(1L)).thenReturn(Optional.of(holiday1));
        when(holidayRepository.existsByHolidayDateAndIdNot(updateRequest.getHolidayDate(), 1L)).thenReturn(false);
        doNothing().when(holidayMapper).updateHolidayFromRequest(updateRequest, holiday1);
        when(holidayRepository.save(holiday1)).thenReturn(updatedHoliday);
        when(holidayMapper.toHolidayResponse(updatedHoliday)).thenReturn(expectedResponse);

        // Act
        HolidayResponse response = holidayService.updateHoliday(1L, updateRequest);

        // Assert
        assertNotNull(response);
        assertEquals("Tết Dương Lịch 2026 Đã Đổi Tên", response.getName());
        verify(holidayRepository, times(1)).findById(1L);
        verify(holidayRepository, times(1)).existsByHolidayDateAndIdNot(updateRequest.getHolidayDate(), 1L);
        verify(holidayRepository, times(1)).save(holiday1);
    }

    @Test
    @DisplayName("TC08: Cập nhật ngày lễ với ID không tồn tại - Ném ngoại lệ HOLIDAY_NOT_FOUND")
    void updateHoliday_NotFound() {
        // Arrange
        when(holidayRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> holidayService.updateHoliday(999L, validRequest));
        assertEquals(ErrorCode.HOLIDAY_NOT_FOUND, exception.getErrorCode());
        verify(holidayRepository, times(1)).findById(999L);
        verify(holidayRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC09: Cập nhật đổi ngày trùng với ngày lễ khác - Ném ngoại lệ HOLIDAY_DATE_ALREADY_EXISTS")
    void updateHoliday_DuplicateDate() {
        // Arrange
        HolidayRequest conflictRequest = HolidayRequest.builder()
                .holidayDate(LocalDate.of(2026, 4, 30))
                .name("Đổi ngày trùng với 30/4")
                .description("Test conflict")
                .build();

        when(holidayRepository.findById(1L)).thenReturn(Optional.of(holiday1));
        when(holidayRepository.existsByHolidayDateAndIdNot(conflictRequest.getHolidayDate(), 1L)).thenReturn(true);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> holidayService.updateHoliday(1L, conflictRequest));
        assertEquals(ErrorCode.HOLIDAY_DATE_ALREADY_EXISTS, exception.getErrorCode());
        verify(holidayRepository, times(1)).findById(1L);
        verify(holidayRepository, times(1)).existsByHolidayDateAndIdNot(conflictRequest.getHolidayDate(), 1L);
        verify(holidayRepository, never()).save(any());
    }

    // =========================================================================
    // 5. deleteHoliday()
    // =========================================================================

    @Test
    @DisplayName("TC10: Xóa ngày lễ tồn tại - Thành công")
    void deleteHoliday_Success() {
        // Arrange
        when(holidayRepository.existsById(1L)).thenReturn(true);
        doNothing().when(holidayRepository).deleteById(1L);

        // Act
        assertDoesNotThrow(() -> holidayService.deleteHoliday(1L));

        // Assert
        verify(holidayRepository, times(1)).existsById(1L);
        verify(holidayRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("TC11: Xóa ngày lễ không tồn tại - Ném ngoại lệ HOLIDAY_NOT_FOUND")
    void deleteHoliday_NotFound() {
        // Arrange
        when(holidayRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        AppException exception = assertThrows(AppException.class, () -> holidayService.deleteHoliday(999L));
        assertEquals(ErrorCode.HOLIDAY_NOT_FOUND, exception.getErrorCode());
        verify(holidayRepository, times(1)).existsById(999L);
        verify(holidayRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("TC12: Thêm ngày lễ có khoảng trắng thừa - Tự động trim trước khi lưu")
    void createHoliday_WithUntrimmedStrings_ShouldTrimBeforeSave() {
        // Arrange
        HolidayRequest untrimmedRequest = HolidayRequest.builder()
                .holidayDate(LocalDate.of(2026, 12, 25))
                .name("   Giáng Sinh Noel   ")
                .description("   Nghỉ lễ Giáng Sinh   ")
                .build();

        Holiday savedHoliday = Holiday.builder()
                .id(4L)
                .holidayDate(LocalDate.of(2026, 12, 25))
                .name("Giáng Sinh Noel")
                .description("Nghỉ lễ Giáng Sinh")
                .build();

        HolidayResponse expectedResponse = HolidayResponse.builder()
                .id(4L)
                .holidayDate(LocalDate.of(2026, 12, 25))
                .name("Giáng Sinh Noel")
                .description("Nghỉ lễ Giáng Sinh")
                .build();

        when(holidayRepository.existsByHolidayDate(untrimmedRequest.getHolidayDate())).thenReturn(false);
        when(holidayMapper.toHoliday(any(HolidayRequest.class))).thenReturn(savedHoliday);
        when(holidayRepository.save(savedHoliday)).thenReturn(savedHoliday);
        when(holidayMapper.toHolidayResponse(savedHoliday)).thenReturn(expectedResponse);

        // Act
        HolidayResponse response = holidayService.createHoliday(untrimmedRequest);

        // Assert
        assertNotNull(response);
        assertEquals("Giáng Sinh Noel", untrimmedRequest.getName());
        assertEquals("Nghỉ lễ Giáng Sinh", untrimmedRequest.getDescription());
        verify(holidayRepository, times(1)).save(savedHoliday);
    }
}
