package com.fpms.integration;

import com.fpms.dto.request.TimeSlotRequest;
import com.fpms.entity.TimeSlot;
import com.fpms.repository.TimeSlotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Bộ kiểm thử tích hợp chuẩn hóa toàn diện cho Phân hệ Quản lý Khung giờ (Subtask ST-05).
 * Áp dụng quy chuẩn kỹ năng: acceptance-criteria-and-test-design (6 khía cạnh kiểm thử).
 * Kiểm thử tính năng phân loại Giờ vàng, BVA độ dài ca, chống chồng chéo 4 biến thể, tiếp giáp biên, xóa mềm.
 */
class TimeSlotManagementIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_SLOT_CREATE_01: [Happy Path] Tạo ca đá Giờ thường hợp lệ (90 phút)")
    void testCreateTimeSlot_Standard_Success() throws Exception {
        TimeSlotRequest request = TimeSlotRequest.builder()
                .startTime(LocalTime.of(6, 0))
                .endTime(LocalTime.of(7, 30))
                .isPeakHour(false)
                .build();

        mockMvc.perform(post("/api/v1/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isPeakHour").value(false));

        // Kiểm tra đối soát trực tiếp trong CSDL PostgreSQL
        TimeSlot persistedSlot = timeSlotRepository.findAll().stream()
                .filter(s -> s.getStartTime().equals(LocalTime.of(6, 0)))
                .findFirst()
                .orElse(null);

        assertNotNull(persistedSlot);
        assertFalse(persistedSlot.getIsPeakHour());
        assertTrue(persistedSlot.getIsActive());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_SLOT_CREATE_02: [Happy Path] Tạo ca đá Giờ vàng hợp lệ (90 phút)")
    void testCreateTimeSlot_PeakHour_Success() throws Exception {
        TimeSlotRequest request = TimeSlotRequest.builder()
                .startTime(LocalTime.of(17, 30))
                .endTime(LocalTime.of(19, 0))
                .isPeakHour(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isPeakHour").value(true));

        TimeSlot persistedSlot = timeSlotRepository.findAll().stream()
                .filter(s -> s.getStartTime().equals(LocalTime.of(17, 30)))
                .findFirst()
                .orElse(null);

        assertNotNull(persistedSlot);
        assertTrue(persistedSlot.getIsPeakHour());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_SLOT_CREATE_03: [BVA Min] Thời lượng ca đá đúng bằng tối thiểu 60 phút")
    void testCreateTimeSlot_Duration60Minutes_Success() throws Exception {
        TimeSlotRequest request = TimeSlotRequest.builder()
                .startTime(LocalTime.of(6, 0))
                .endTime(LocalTime.of(7, 0))
                .isPeakHour(false)
                .build();

        mockMvc.perform(post("/api/v1/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_SLOT_CREATE_04: [BVA Max] Thời lượng ca đá đúng bằng tối đa 120 phút")
    void testCreateTimeSlot_Duration120Minutes_Success() throws Exception {
        TimeSlotRequest request = TimeSlotRequest.builder()
                .startTime(LocalTime.of(7, 0))
                .endTime(LocalTime.of(9, 0))
                .isPeakHour(false)
                .build();

        mockMvc.perform(post("/api/v1/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_SLOT_CREATE_05: [Negative / BVA Min-1] Thời lượng ca đá < 60 phút (59 phút)")
    void testCreateTimeSlot_Duration59Minutes_ReturnsBadRequest() throws Exception {
        TimeSlotRequest request = TimeSlotRequest.builder()
                .startTime(LocalTime.of(6, 0))
                .endTime(LocalTime.of(6, 59))
                .isPeakHour(false)
                .build();

        mockMvc.perform(post("/api/v1/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3015)); // TIME_SLOT_INVALID_DURATION
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_SLOT_CREATE_06: [Negative / BVA Max+1] Thời lượng ca đá > 120 phút (121 phút)")
    void testCreateTimeSlot_Duration121Minutes_ReturnsBadRequest() throws Exception {
        TimeSlotRequest request = TimeSlotRequest.builder()
                .startTime(LocalTime.of(6, 0))
                .endTime(LocalTime.of(8, 1))
                .isPeakHour(false)
                .build();

        mockMvc.perform(post("/api/v1/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3015)); // TIME_SLOT_INVALID_DURATION
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_SLOT_CREATE_07: [Negative] Giờ kết thúc trước giờ bắt đầu")
    void testCreateTimeSlot_EndTimeBeforeStartTime_ReturnsBadRequest() throws Exception {
        TimeSlotRequest request = TimeSlotRequest.builder()
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(17, 30))
                .isPeakHour(false)
                .build();

        mockMvc.perform(post("/api/v1/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3012)); // TIME_SLOT_INVALID_TIME
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_SLOT_CREATE_08: [Negative] Giờ kết thúc bằng giờ bắt đầu")
    void testCreateTimeSlot_EndTimeEqualsStartTime_ReturnsBadRequest() throws Exception {
        TimeSlotRequest request = TimeSlotRequest.builder()
                .startTime(LocalTime.of(7, 0))
                .endTime(LocalTime.of(7, 0))
                .isPeakHour(false)
                .build();

        mockMvc.perform(post("/api/v1/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3012)); // TIME_SLOT_INVALID_TIME
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_SLOT_CREATE_09: [Negative / Overlap 1] Trùng khít hoàn toàn thời gian với ca đã có")
    void testCreateTimeSlot_ExactOverlap_ReturnsBadRequest() throws Exception {
        timeSlotRepository.save(TimeSlot.builder()
                .startTime(LocalTime.of(17, 30))
                .endTime(LocalTime.of(19, 0))
                .isPeakHour(true)
                .isActive(true)
                .build());

        TimeSlotRequest request = TimeSlotRequest.builder()
                .startTime(LocalTime.of(17, 30))
                .endTime(LocalTime.of(19, 0))
                .isPeakHour(false)
                .build();

        mockMvc.perform(post("/api/v1/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3013)); // TIME_SLOT_OVERLAPPING
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_SLOT_CREATE_10: [Negative / Overlap 2] Ca mới nằm lọt thỏm bên trong ca đã có")
    void testCreateTimeSlot_EnclosedInsideOverlap_ReturnsBadRequest() throws Exception {
        timeSlotRepository.save(TimeSlot.builder()
                .startTime(LocalTime.of(17, 0))
                .endTime(LocalTime.of(19, 0))
                .isPeakHour(true)
                .isActive(true)
                .build());

        TimeSlotRequest request = TimeSlotRequest.builder()
                .startTime(LocalTime.of(17, 30))
                .endTime(LocalTime.of(18, 30))
                .isPeakHour(false)
                .build();

        mockMvc.perform(post("/api/v1/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3013));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_SLOT_CREATE_11: [Negative / Overlap 3] Ca mới bắt đầu trước và lấn vào ca đã có")
    void testCreateTimeSlot_StraddlingStartOverlap_ReturnsBadRequest() throws Exception {
        timeSlotRepository.save(TimeSlot.builder()
                .startTime(LocalTime.of(17, 30))
                .endTime(LocalTime.of(19, 0))
                .isPeakHour(true)
                .isActive(true)
                .build());

        TimeSlotRequest request = TimeSlotRequest.builder()
                .startTime(LocalTime.of(17, 0))
                .endTime(LocalTime.of(18, 30))
                .isPeakHour(false)
                .build();

        mockMvc.perform(post("/api/v1/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3013));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_SLOT_CREATE_12: [Negative / Overlap 4] Ca mới bắt đầu trong ca cũ và kết thúc sau ca cũ")
    void testCreateTimeSlot_StraddlingEndOverlap_ReturnsBadRequest() throws Exception {
        timeSlotRepository.save(TimeSlot.builder()
                .startTime(LocalTime.of(17, 30))
                .endTime(LocalTime.of(19, 0))
                .isPeakHour(true)
                .isActive(true)
                .build());

        TimeSlotRequest request = TimeSlotRequest.builder()
                .startTime(LocalTime.of(18, 30))
                .endTime(LocalTime.of(20, 0))
                .isPeakHour(false)
                .build();

        mockMvc.perform(post("/api/v1/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3013));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_SLOT_CREATE_13: [Edge Case] Tiếp giáp biên thời gian (Touching Boundary được phép)")
    void testCreateTimeSlot_TouchingBoundary_Success() throws Exception {
        timeSlotRepository.save(TimeSlot.builder()
                .startTime(LocalTime.of(17, 30))
                .endTime(LocalTime.of(19, 0))
                .isPeakHour(true)
                .isActive(true)
                .build());

        // Ca mới bắt đầu đúng thời điểm ca cũ kết thúc (19:00) -> Không coi là chồng chéo
        TimeSlotRequest adjacentRequest = TimeSlotRequest.builder()
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(20, 30))
                .isPeakHour(false)
                .build();

        mockMvc.perform(post("/api/v1/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjacentRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_SLOT_GET_01: [Happy Path] Lấy danh sách toàn bộ ca đá sắp xếp theo startTime ASC")
    void testGetAllTimeSlots_OrderedByStartTime_Success() throws Exception {
        timeSlotRepository.save(TimeSlot.builder()
                .startTime(LocalTime.of(19, 0))
                .endTime(LocalTime.of(20, 30))
                .isPeakHour(false)
                .isActive(true)
                .build());

        timeSlotRepository.save(TimeSlot.builder()
                .startTime(LocalTime.of(6, 0))
                .endTime(LocalTime.of(7, 30))
                .isPeakHour(false)
                .isActive(true)
                .build());

        mockMvc.perform(get("/api/v1/admin/time-slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].startTime").value("06:00"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_SLOT_UPDATE_01: [State Change] Chuyển đổi ca đá từ Giờ thường sang Giờ vàng")
    void testUpdateTimeSlot_ChangeToPeakHour_Success() throws Exception {
        TimeSlot slot = timeSlotRepository.save(TimeSlot.builder()
                .startTime(LocalTime.of(17, 30))
                .endTime(LocalTime.of(19, 0))
                .isPeakHour(false)
                .isActive(true)
                .build());

        TimeSlotRequest updateRequest = TimeSlotRequest.builder()
                .startTime(LocalTime.of(17, 30))
                .endTime(LocalTime.of(19, 0))
                .isPeakHour(true)
                .build();

        mockMvc.perform(put("/api/v1/admin/time-slots/" + slot.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPeakHour").value(true));

        TimeSlot updated = timeSlotRepository.findById(slot.getId()).orElseThrow();
        assertTrue(updated.getIsPeakHour());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("TC_SLOT_DELETE_01: [Happy Path / Soft Delete] Xóa mềm ca đá chưa có đơn đặt trong tương lai")
    void testDeleteTimeSlot_NoActiveBookings_Success() throws Exception {
        TimeSlot slot = timeSlotRepository.save(TimeSlot.builder()
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 30))
                .isPeakHour(false)
                .isActive(true)
                .build());

        mockMvc.perform(delete("/api/v1/admin/time-slots/" + slot.getId()))
                .andExpect(status().isOk());

        TimeSlot deletedSlot = timeSlotRepository.findById(slot.getId()).orElseThrow();
        assertFalse(deletedSlot.getIsActive()); // Đã xóa mềm
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("TC_SLOT_SEC_01: [Security / RBAC] Khách hàng thường gọi API quản trị khung giờ bị từ chối 403 Forbidden")
    void testTimeSlotEndpoints_UnauthorizedCustomer_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/time-slots"))
                .andExpect(status().isForbidden());

        TimeSlotRequest request = TimeSlotRequest.builder()
                .startTime(LocalTime.of(6, 0))
                .endTime(LocalTime.of(7, 30))
                .isPeakHour(false)
                .build();

        mockMvc.perform(post("/api/v1/admin/time-slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
