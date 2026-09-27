package com.fpms.integration;

import com.fpms.dto.request.PitchRequest;
import com.fpms.dto.request.UpdatePitchStatusRequest;
import com.fpms.entity.Booking;
import com.fpms.entity.Pitch;
import com.fpms.entity.PitchType;
import com.fpms.entity.TimeSlot;
import com.fpms.entity.enums.BookingStatus;
import com.fpms.entity.enums.PitchStatus;
import com.fpms.repository.BookingRepository;
import com.fpms.repository.PitchRepository;
import com.fpms.repository.PitchTypeRepository;
import com.fpms.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Bộ kiểm thử tích hợp chuẩn hóa toàn diện cho Phân hệ Quản lý Sân bóng (Subtask ST-05).
 * Áp dụng quy chuẩn kỹ năng: acceptance-criteria-and-test-design (6 khía cạnh kiểm thử).
 * Kiểm thử thông suốt từ Controller -> Service -> Repository -> CSDL PostgreSQL (fpms_test).
 */
class PitchManagementIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PitchRepository pitchRepository;

    @Autowired
    private PitchTypeRepository pitchTypeRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private PitchType pitchType5;

    @BeforeEach
    void setUp() {
        pitchType5 = pitchTypeRepository.findByNameIgnoreCaseAndIsDeletedFalse("Sân 5 người")
                .orElseGet(() -> {
                    PitchType type = PitchType.builder()
                            .name("Sân 5 người")
                            .playerCapacity(10)
                            .description("Sân 5 tiêu chuẩn")
                            .build();
                    type.setIsDeleted(false);
                    return pitchTypeRepository.save(type);
                });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PITCH_CREATE_01: [Happy Path] Thêm sân bóng mới tiêu chuẩn hợp lệ")
    void testCreatePitch_Standard_Success() throws Exception {
        PitchRequest request = PitchRequest.builder()
                .name("Sân 5C - Test IT")
                .pitchTypeId(pitchType5.getId())
                .description("Cỏ nhân tạo FIFA")
                .build();

        mockMvc.perform(post("/api/v1/admin/pitches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Sân 5C - Test IT"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // Nghiệm thu thực tế trong CSDL PostgreSQL
        Pitch savedPitch = pitchRepository.findByNameIgnoreCaseAndIsDeletedFalse("Sân 5C - Test IT").orElse(null);
        assertNotNull(savedPitch);
        assertEquals(PitchStatus.ACTIVE, savedPitch.getStatus());
        assertEquals(pitchType5.getId(), savedPitch.getPitchType().getId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PITCH_CREATE_02: [Negative] Chặn thêm sân trùng tên đã có (không phân biệt hoa thường)")
    void testCreatePitch_DuplicateName_ReturnsBadRequest() throws Exception {
        pitchRepository.save(Pitch.builder()
                .name("Sân 5 Trùng Tên")
                .pitchType(pitchType5)
                .status(PitchStatus.ACTIVE)
                .build());

        PitchRequest duplicateRequest = PitchRequest.builder()
                .name("sân 5 trùng tên") // Viết thường để kiểm tra Case-Insensitive
                .pitchTypeId(pitchType5.getId())
                .build();

        mockMvc.perform(post("/api/v1/admin/pitches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3002));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PITCH_CREATE_03: [Negative] Thêm sân với loại sân không tồn tại")
    void testCreatePitch_InvalidPitchType_ReturnsNotFound() throws Exception {
        PitchRequest request = PitchRequest.builder()
                .name("Sân Loại Ảo")
                .pitchTypeId(999999L)
                .build();

        mockMvc.perform(post("/api/v1/admin/pitches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(3003));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PITCH_CREATE_04: [Negative / BVA Min-1] Bỏ trống tên sân bóng gây lỗi Validation")
    void testCreatePitch_BlankName_ReturnsBadRequest() throws Exception {
        PitchRequest request = PitchRequest.builder()
                .name("")
                .pitchTypeId(pitchType5.getId())
                .build();

        mockMvc.perform(post("/api/v1/admin/pitches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PITCH_CREATE_05: [BVA Min] Tên sân bóng đạt độ dài tối thiểu 1 ký tự")
    void testCreatePitch_BoundaryName1Char_Success() throws Exception {
        PitchRequest request = PitchRequest.builder()
                .name("A")
                .pitchTypeId(pitchType5.getId())
                .description("Test boundary min 1 char")
                .build();

        mockMvc.perform(post("/api/v1/admin/pitches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("A"));

        assertTrue(pitchRepository.existsByNameIgnoreCaseAndIsDeletedFalse("A"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PITCH_CREATE_06: [BVA Max] Tên sân bóng đạt đúng độ dài tối đa 100 ký tự")
    void testCreatePitch_BoundaryName100Chars_Success() throws Exception {
        String longName = "A".repeat(100);
        PitchRequest request = PitchRequest.builder()
                .name(longName)
                .pitchTypeId(pitchType5.getId())
                .description("Test boundary 100 chars")
                .build();

        mockMvc.perform(post("/api/v1/admin/pitches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value(longName));

        assertTrue(pitchRepository.existsByNameIgnoreCaseAndIsDeletedFalse(longName));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PITCH_CREATE_07: [Negative / BVA Max+1] Tên sân bóng vượt quá 100 ký tự (101 ký tự)")
    void testCreatePitch_BoundaryName101Chars_ReturnsBadRequest() throws Exception {
        String overLengthName = "A".repeat(101);
        PitchRequest request = PitchRequest.builder()
                .name(overLengthName)
                .pitchTypeId(pitchType5.getId())
                .build();

        mockMvc.perform(post("/api/v1/admin/pitches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PITCH_CREATE_08: [Edge Case] Tên sân có khoảng trắng thừa đầu cuối và dấu Tiếng Việt")
    void testCreatePitch_LeadingTrailingWhitespaceAndVietnamese_TrimmedAndSaved() throws Exception {
        PitchRequest request = PitchRequest.builder()
                .name("   Sân Cỏ Nhân Tạo Chuẩn FIFA 5A   ")
                .pitchTypeId(pitchType5.getId())
                .description("Thử nghiệm trim khoảng trắng")
                .build();

        mockMvc.perform(post("/api/v1/admin/pitches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Sân Cỏ Nhân Tạo Chuẩn FIFA 5A"));

        // Nghiệm thu trong CSDL chuỗi đã được trim sạch sẽ
        assertTrue(pitchRepository.existsByNameIgnoreCaseAndIsDeletedFalse("Sân Cỏ Nhân Tạo Chuẩn FIFA 5A"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PITCH_GET_01: [Happy Path] Lọc danh sách sân theo keyword, loại sân, trạng thái và phân trang")
    void testGetPitches_WithFilterAndPagination_Success() throws Exception {
        pitchRepository.save(Pitch.builder()
                .name("Sân Mỹ Đình 1")
                .pitchType(pitchType5)
                .status(PitchStatus.ACTIVE)
                .build());

        pitchRepository.save(Pitch.builder()
                .name("Sân Mỹ Đình 2")
                .pitchType(pitchType5)
                .status(PitchStatus.MAINTENANCE)
                .build());

        mockMvc.perform(get("/api/v1/admin/pitches")
                        .param("keyword", "Mỹ Đình")
                        .param("status", "ACTIVE")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].name").value("Sân Mỹ Đình 1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PITCH_GET_02: [Happy Path] Lấy chi tiết sân bóng theo ID hợp lệ")
    void testGetPitchById_Success() throws Exception {
        Pitch pitch = pitchRepository.save(Pitch.builder()
                .name("Sân Lấy Chi Tiết")
                .pitchType(pitchType5)
                .status(PitchStatus.ACTIVE)
                .build());

        mockMvc.perform(get("/api/v1/admin/pitches/" + pitch.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(pitch.getId()))
                .andExpect(jsonPath("$.data.name").value("Sân Lấy Chi Tiết"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PITCH_GET_03: [Negative] Lấy chi tiết sân bóng theo ID không tồn tại")
    void testGetPitchById_NotFound_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/admin/pitches/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(3001));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PITCH_UPDATE_01: [Happy Path] Chỉnh sửa tên và mô tả sân bóng")
    void testUpdatePitch_Success() throws Exception {
        Pitch pitch = pitchRepository.save(Pitch.builder()
                .name("Sân Cũ Cần Đổi")
                .pitchType(pitchType5)
                .status(PitchStatus.ACTIVE)
                .build());

        PitchRequest updateRequest = PitchRequest.builder()
                .name("Sân Đã Cập Nhật Mới")
                .pitchTypeId(pitchType5.getId())
                .description("Mô tả mới sau khi sửa")
                .build();

        mockMvc.perform(put("/api/v1/admin/pitches/" + pitch.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Sân Đã Cập Nhật Mới"));

        Pitch updated = pitchRepository.findById(pitch.getId()).orElseThrow();
        assertEquals("Sân Đã Cập Nhật Mới", updated.getName());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PITCH_UPDATE_02: [Negative] Cập nhật tên sân trùng với tên của một sân khác")
    void testUpdatePitch_DuplicateNameWithOtherPitch_ReturnsBadRequest() throws Exception {
        pitchRepository.save(Pitch.builder()
                .name("Sân A Cố Định")
                .pitchType(pitchType5)
                .status(PitchStatus.ACTIVE)
                .build());

        Pitch pitchB = pitchRepository.save(Pitch.builder()
                .name("Sân B Sắp Đổi")
                .pitchType(pitchType5)
                .status(PitchStatus.ACTIVE)
                .build());

        PitchRequest conflictRequest = PitchRequest.builder()
                .name("Sân A Cố Định")
                .pitchTypeId(pitchType5.getId())
                .build();

        mockMvc.perform(put("/api/v1/admin/pitches/" + pitchB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3002));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PITCH_STATUS_01: [State Change] Chuyển đổi trạng thái sân ACTIVE sang MAINTENANCE và khôi phục")
    void testUpdatePitchStatus_Transitions_Success() throws Exception {
        Pitch pitch = pitchRepository.save(Pitch.builder()
                .name("Sân Trạng Thái Test")
                .pitchType(pitchType5)
                .status(PitchStatus.ACTIVE)
                .build());

        // 1. Chuyển sang MAINTENANCE
        mockMvc.perform(patch("/api/v1/admin/pitches/" + pitch.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePitchStatusRequest(PitchStatus.MAINTENANCE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MAINTENANCE"));

        Pitch updated1 = pitchRepository.findById(pitch.getId()).orElseThrow();
        assertEquals(PitchStatus.MAINTENANCE, updated1.getStatus());

        // 2. Khôi phục lại ACTIVE
        mockMvc.perform(patch("/api/v1/admin/pitches/" + pitch.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePitchStatusRequest(PitchStatus.ACTIVE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        Pitch updated2 = pitchRepository.findById(pitch.getId()).orElseThrow();
        assertEquals(PitchStatus.ACTIVE, updated2.getStatus());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PITCH_DELETE_01: [Happy Path] Xóa cứng sân bóng mới tạo chưa có đơn đặt trong lịch sử")
    void testDeletePitch_Success() throws Exception {
        Pitch pitch = pitchRepository.save(Pitch.builder()
                .name("Sân Rác Cần Xóa")
                .pitchType(pitchType5)
                .status(PitchStatus.ACTIVE)
                .build());

        mockMvc.perform(delete("/api/v1/admin/pitches/" + pitch.getId()))
                .andExpect(status().isOk());

        assertFalse(pitchRepository.existsById(pitch.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_PITCH_DELETE_02: [Negative / Data Integrity] Chặn xóa sân bóng đã có lịch sử đơn đặt sân")
    void testDeletePitch_WithExistingBookings_ReturnsBadRequest() throws Exception {
        Pitch pitch = pitchRepository.save(Pitch.builder()
                .name("Sân Đã Có Booking")
                .pitchType(pitchType5)
                .status(PitchStatus.ACTIVE)
                .build());

        TimeSlot timeSlot = timeSlotRepository.save(TimeSlot.builder()
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 30))
                .isPeakHour(false)
                .isActive(true)
                .build());

        // Tạo bản ghi đơn đặt sân liên kết với sân này
        bookingRepository.save(Booking.builder()
                .bookingCode("BK-TEST-DEL-01")
                .pitch(pitch)
                .timeSlot(timeSlot)
                .bookingDate(LocalDate.now().plusDays(1))
                .priceSnapshot(new BigDecimal("200000"))
                .totalPitchAmount(new BigDecimal("200000"))
                .status(BookingStatus.CONFIRMED)
                .build());

        mockMvc.perform(delete("/api/v1/admin/pitches/" + pitch.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3005)); // PITCH_HAS_BOOKINGS

        // Kiểm tra sân bóng vẫn tồn tại nguyên vẹn
        assertTrue(pitchRepository.existsById(pitch.getId()));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("TC_PITCH_SEC_01: [Security / RBAC] Khách hàng thường gọi API quản trị sân bóng bị từ chối 403 Forbidden")
    void testPitchEndpoints_UnauthorizedCustomer_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/pitches"))
                .andExpect(status().isForbidden());

        PitchRequest request = PitchRequest.builder()
                .name("Sân Hack")
                .pitchTypeId(pitchType5.getId())
                .build();

        mockMvc.perform(post("/api/v1/admin/pitches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
