package com.fpms.integration;

import com.fpms.dto.request.HolidayRequest;
import com.fpms.entity.Holiday;
import com.fpms.exception.ErrorCode;
import com.fpms.repository.HolidayRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Bộ kiểm thử tích hợp chuẩn hóa toàn diện cho Phân hệ Quản lý Ngày lễ (Subtask FPMS-128 / FPMS-104).
 * Áp dụng quy chuẩn kỹ năng: acceptance-criteria-and-test-design (6 khía cạnh kiểm thử).
 * Kiểm thử thông suốt từ Controller -> Service -> Repository -> CSDL PostgreSQL (fpms_test).
 */
class HolidayManagementIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private HolidayRepository holidayRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_HOLI_01: [Happy Path] Thêm mới ngày lễ hợp lệ thành công")
    void testCreateHoliday_Success() throws Exception {
        LocalDate holidayDate = LocalDate.of(2026, 9, 2);
        HolidayRequest request = HolidayRequest.builder()
                .holidayDate(holidayDate)
                .name("Quốc khánh 2/9")
                .description("Phụ thu ngày lễ toàn quốc")
                .build();

        mockMvc.perform(post("/api/v1/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Quốc khánh 2/9"))
                .andExpect(jsonPath("$.data.holidayDate").value("2026-09-02"));

        // Nghiệm thu thực tế trong CSDL PostgreSQL
        assertTrue(holidayRepository.existsByHolidayDate(holidayDate));
        Holiday saved = holidayRepository.findAll().stream()
                .filter(h -> h.getHolidayDate().equals(holidayDate))
                .findFirst()
                .orElse(null);
        assertNotNull(saved);
        assertEquals("Quốc khánh 2/9", saved.getName());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_HOLI_02: [Happy Path] Lấy danh sách ngày lễ có lọc theo năm")
    void testGetAllHolidays_FilterByYear_Success() throws Exception {
        holidayRepository.save(Holiday.builder()
                .holidayDate(LocalDate.of(2026, 1, 1))
                .name("Tết Dương Lịch 2026")
                .build());

        holidayRepository.save(Holiday.builder()
                .holidayDate(LocalDate.of(2027, 1, 1))
                .name("Tết Dương Lịch 2027")
                .build());

        mockMvc.perform(get("/api/v1/admin/holidays")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[*].holidayDate", everyItem(startsWith("2026"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_HOLI_03: [Happy Path] Cập nhật thông tin ngày lễ thành công")
    void testUpdateHoliday_Success() throws Exception {
        Holiday holiday = holidayRepository.save(Holiday.builder()
                .holidayDate(LocalDate.of(2026, 4, 30))
                .name("Giải phóng miền Nam")
                .description("Ghi chú cũ")
                .build());

        HolidayRequest updateRequest = HolidayRequest.builder()
                .holidayDate(LocalDate.of(2026, 4, 30))
                .name("Ngày Thống nhất đất nước")
                .description("Ghi chú mới đã cập nhật")
                .build();

        mockMvc.perform(put("/api/v1/admin/holidays/{id}", holiday.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Ngày Thống nhất đất nước"))
                .andExpect(jsonPath("$.data.description").value("Ghi chú mới đã cập nhật"));

        Holiday updated = holidayRepository.findById(holiday.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals("Ngày Thống nhất đất nước", updated.getName());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_HOLI_04: [Happy Path] Xóa ngày lễ khỏi hệ thống")
    void testDeleteHoliday_Success() throws Exception {
        Holiday holiday = holidayRepository.save(Holiday.builder()
                .holidayDate(LocalDate.of(2026, 5, 1))
                .name("Quốc tế Lao động")
                .build());

        mockMvc.perform(delete("/api/v1/admin/holidays/{id}", holiday.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xóa ngày lễ thành công"));

        assertFalse(holidayRepository.existsById(holiday.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_HOLI_05: [Negative] Chặn thêm ngày lễ trùng ngày đã có")
    void testCreateHoliday_DuplicateDate_ReturnsConflict() throws Exception {
        LocalDate duplicateDate = LocalDate.of(2026, 11, 20);
        holidayRepository.save(Holiday.builder()
                .holidayDate(duplicateDate)
                .name("Ngày Nhà giáo Việt Nam")
                .build());

        HolidayRequest request = HolidayRequest.builder()
                .holidayDate(duplicateDate)
                .name("Ngày Nhà giáo trùng lặp")
                .build();

        mockMvc.perform(post("/api/v1/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.HOLIDAY_DATE_ALREADY_EXISTS.getCode()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_HOLI_06: [Negative] Cập nhật hoặc Xóa ngày lễ với ID không tồn tại")
    void testUpdateAndDeleteHoliday_NotFound_Returns404() throws Exception {
        HolidayRequest request = HolidayRequest.builder()
                .holidayDate(LocalDate.of(2026, 12, 25))
                .name("Giáng Sinh")
                .build();

        mockMvc.perform(put("/api/v1/admin/holidays/{id}", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.HOLIDAY_NOT_FOUND.getCode()));

        mockMvc.perform(delete("/api/v1/admin/holidays/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.HOLIDAY_NOT_FOUND.getCode()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_HOLI_07: [Negative] Bỏ trống trường ngày lễ")
    void testCreateHoliday_MissingHolidayDate_ReturnsBadRequest() throws Exception {
        HolidayRequest request = HolidayRequest.builder()
                .holidayDate(null)
                .name("Ngày Lễ Không Ngày")
                .build();

        mockMvc.perform(post("/api/v1/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors[0].message").value(ErrorCode.HOLIDAY_DATE_REQUIRED.getMessage()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_HOLI_08: [Negative] Bỏ trống tên ngày lễ")
    void testCreateHoliday_BlankName_ReturnsBadRequest() throws Exception {
        HolidayRequest request = HolidayRequest.builder()
                .holidayDate(LocalDate.of(2026, 8, 19))
                .name("   ")
                .build();

        mockMvc.perform(post("/api/v1/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors[0].message").value(ErrorCode.HOLIDAY_NAME_REQUIRED.getMessage()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_HOLI_09: [Boundary] Tên ngày lễ đúng 1 ký tự (Cận biên dưới)")
    void testCreateHoliday_BoundaryName1Char_Success() throws Exception {
        HolidayRequest request = HolidayRequest.builder()
                .holidayDate(LocalDate.of(2026, 6, 1))
                .name("A")
                .build();

        mockMvc.perform(post("/api/v1/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("A"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_HOLI_10: [Boundary] Tên ngày lễ đúng 150 ký tự (Cận biên trên)")
    void testCreateHoliday_BoundaryName150Chars_Success() throws Exception {
        String name150 = "N".repeat(150);
        HolidayRequest request = HolidayRequest.builder()
                .holidayDate(LocalDate.of(2026, 6, 2))
                .name(name150)
                .build();

        mockMvc.perform(post("/api/v1/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value(name150));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_HOLI_11: [Boundary] Tên ngày lễ 151 ký tự (Vượt cận biên trên)")
    void testCreateHoliday_ExceedName151Chars_ReturnsBadRequest() throws Exception {
        String name151 = "N".repeat(151);
        HolidayRequest request = HolidayRequest.builder()
                .holidayDate(LocalDate.of(2026, 6, 3))
                .name(name151)
                .build();

        mockMvc.perform(post("/api/v1/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors[0].message").value(ErrorCode.HOLIDAY_NAME_MAX_LENGTH.getMessage()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_HOLI_12: [Edge Case] Tự động trim khoảng trắng thừa đầu/cuối của tên và mô tả")
    void testCreateHoliday_AutoTrimWhitespace_Success() throws Exception {
        HolidayRequest request = HolidayRequest.builder()
                .holidayDate(LocalDate.of(2026, 10, 10))
                .name("   Giải phóng Thủ đô   ")
                .description("   Kỷ niệm ngày giải phóng   ")
                .build();

        mockMvc.perform(post("/api/v1/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Giải phóng Thủ đô"))
                .andExpect(jsonPath("$.data.description").value("Kỷ niệm ngày giải phóng"));

        Holiday saved = holidayRepository.findAll().stream()
                .filter(h -> h.getHolidayDate().equals(LocalDate.of(2026, 10, 10)))
                .findFirst()
                .orElse(null);
        assertNotNull(saved);
        assertEquals("Giải phóng Thủ đô", saved.getName());
        assertEquals("Kỷ niệm ngày giải phóng", saved.getDescription());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("TC_HOLI_13: [Security] Khách hàng ROLE_CUSTOMER cố thêm ngày lễ")
    void testCreateHoliday_CustomerRole_Forbidden() throws Exception {
        HolidayRequest request = HolidayRequest.builder()
                .holidayDate(LocalDate.of(2026, 10, 20))
                .name("Ngày Phụ nữ Việt Nam")
                .build();

        mockMvc.perform(post("/api/v1/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC_HOLI_14: [Security] Người dùng ẩn danh (Anonymous) gọi API ngày lễ")
    void testCreateHoliday_Anonymous_UnauthorizedOrForbidden() throws Exception {
        HolidayRequest request = HolidayRequest.builder()
                .holidayDate(LocalDate.of(2026, 10, 20))
                .name("Ngày Phụ nữ Việt Nam")
                .build();

        mockMvc.perform(post("/api/v1/admin/holidays")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> assertTrue(
                        result.getResponse().getStatus() == 401 || result.getResponse().getStatus() == 403,
                        "Expected 401 Unauthorized or 403 Forbidden but was " + result.getResponse().getStatus()
                ));
    }
}
