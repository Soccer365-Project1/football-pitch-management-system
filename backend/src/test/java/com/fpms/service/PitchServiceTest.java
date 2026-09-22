package com.fpms.service;

import com.fpms.common.response.PageResponse;
import com.fpms.dto.request.PitchRequest;
import com.fpms.dto.request.UpdatePitchStatusRequest;
import com.fpms.dto.response.PitchResponse;
import com.fpms.dto.response.PitchTypeResponse;
import com.fpms.entity.Pitch;
import com.fpms.entity.PitchType;
import com.fpms.entity.enums.PitchStatus;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.mapper.PitchMapper;
import com.fpms.mapper.PitchTypeMapper;
import com.fpms.repository.PitchRepository;
import com.fpms.repository.PitchTypeRepository;
import com.fpms.service.impl.PitchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PitchServiceTest {

    @Mock
    private PitchRepository pitchRepository;

    @Mock
    private PitchTypeRepository pitchTypeRepository;

    @Mock
    private PitchMapper pitchMapper;

    @Mock
    private PitchTypeMapper pitchTypeMapper;

    @InjectMocks
    private PitchServiceImpl pitchService;

    private PitchType pitchType5;
    private PitchType pitchType7;
    private PitchTypeResponse pitchTypeResponse5;
    private Pitch pitch1;
    private PitchResponse pitchResponse1;

    @BeforeEach
    void setUp() {
        pitchType5 = PitchType.builder()
                .name("Sân 5 người")
                .playerCapacity(10)
                .description("Sân bóng 5 người tiêu chuẩn")
                .build();
        pitchType5.setId(1L);
        pitchType5.setIsDeleted(false);

        pitchType7 = PitchType.builder()
                .name("Sân 7 người")
                .playerCapacity(14)
                .description("Sân bóng 7 người tiêu chuẩn")
                .build();
        pitchType7.setId(2L);
        pitchType7.setIsDeleted(false);

        pitchTypeResponse5 = PitchTypeResponse.builder()
                .id(1L)
                .name("Sân 5 người")
                .playerCapacity(10)
                .description("Sân bóng 5 người tiêu chuẩn")
                .build();

        pitch1 = Pitch.builder()
                .name("Sân 5A")
                .pitchType(pitchType5)
                .status(PitchStatus.ACTIVE)
                .description("Sân gần cổng vào")
                .build();
        pitch1.setId(10L);
        pitch1.setIsDeleted(false);
        pitch1.setCreatedAt(LocalDateTime.now());
        pitch1.setUpdatedAt(LocalDateTime.now());

        pitchResponse1 = PitchResponse.builder()
                .id(10L)
                .name("Sân 5A")
                .pitchType(pitchTypeResponse5)
                .status(PitchStatus.ACTIVE)
                .description("Sân gần cổng vào")
                .createdAt(pitch1.getCreatedAt())
                .updatedAt(pitch1.getUpdatedAt())
                .build();
    }

    // ================= 1. Test Cases Cho getPitches (Phân trang & Lọc) =================

    @Test
    @DisplayName("UT-01: Lấy danh sách sân bóng thành công với phân trang")
    void getPitches_Success() {
        Page<Pitch> page = new PageImpl<>(List.of(pitch1));
        when(pitchRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(pitchMapper.toPitchResponse(pitch1)).thenReturn(pitchResponse1);

        PageResponse<PitchResponse> result = pitchService.getPitches("5A", 1L, PitchStatus.ACTIVE, 1, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getItems().size());
        assertEquals("Sân 5A", result.getItems().get(0).getName());
        verify(pitchRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    // ================= 2. Test Cases Cho getAllPitchTypes =================

    @Test
    @DisplayName("UT-02: Lấy danh sách các loại sân bóng thành công")
    void getAllPitchTypes_Success() {
        when(pitchTypeRepository.findAllByIsDeletedFalse()).thenReturn(List.of(pitchType5, pitchType7));
        when(pitchTypeMapper.toPitchTypeResponseList(anyList())).thenReturn(List.of(pitchTypeResponse5));

        List<PitchTypeResponse> result = pitchService.getAllPitchTypes();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(pitchTypeRepository, times(1)).findAllByIsDeletedFalse();
    }

    // ================= 3. Test Cases Cho getPitchById =================

    @Test
    @DisplayName("UT-03: Tìm thấy sân bóng theo ID hợp lệ")
    void getPitchById_Success() {
        when(pitchRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(pitch1));
        when(pitchMapper.toPitchResponse(pitch1)).thenReturn(pitchResponse1);

        PitchResponse result = pitchService.getPitchById(10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Sân 5A", result.getName());
        verify(pitchRepository, times(1)).findByIdAndIsDeletedFalse(10L);
    }

    @Test
    @DisplayName("UT-04: Bắn ngoại lệ PITCH_NOT_FOUND khi ID không tồn tại")
    void getPitchById_NotFound_ThrowsException() {
        when(pitchRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> pitchService.getPitchById(99L));

        assertEquals(ErrorCode.PITCH_NOT_FOUND, ex.getErrorCode());
        verify(pitchRepository, times(1)).findByIdAndIsDeletedFalse(99L);
    }

    // ================= 4. Test Cases Cho createPitch (UC015.1) =================

    @Test
    @DisplayName("UT-05: Thêm mới sân bóng thành công với trạng thái mặc định ACTIVE")
    void createPitch_Success() {
        PitchRequest request = PitchRequest.builder()
                .name("Sân 5B")
                .pitchTypeId(1L)
                .description("Sân mới hoàn thành")
                .build();

        Pitch newPitch = Pitch.builder()
                .name("Sân 5B")
                .pitchType(pitchType5)
                .status(PitchStatus.ACTIVE)
                .description("Sân mới hoàn thành")
                .build();

        Pitch savedPitch = Pitch.builder()
                .name("Sân 5B")
                .pitchType(pitchType5)
                .status(PitchStatus.ACTIVE)
                .description("Sân mới hoàn thành")
                .build();
        savedPitch.setId(11L);

        PitchResponse response = PitchResponse.builder()
                .id(11L)
                .name("Sân 5B")
                .pitchType(pitchTypeResponse5)
                .status(PitchStatus.ACTIVE)
                .build();

        when(pitchRepository.existsByNameIgnoreCaseAndIsDeletedFalse("Sân 5B")).thenReturn(false);
        when(pitchTypeRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(pitchType5));
        when(pitchMapper.toPitch(request)).thenReturn(newPitch);
        when(pitchRepository.save(any(Pitch.class))).thenReturn(savedPitch);
        when(pitchMapper.toPitchResponse(savedPitch)).thenReturn(response);

        PitchResponse result = pitchService.createPitch(request);

        assertNotNull(result);
        assertEquals(11L, result.getId());
        assertEquals("Sân 5B", result.getName());
        assertEquals(PitchStatus.ACTIVE, result.getStatus());
        verify(pitchRepository, times(1)).existsByNameIgnoreCaseAndIsDeletedFalse("Sân 5B");
        verify(pitchTypeRepository, times(1)).findByIdAndIsDeletedFalse(1L);
        verify(pitchRepository, times(1)).save(any(Pitch.class));
    }

    @Test
    @DisplayName("UT-06: Bắn ngoại lệ PITCH_NAME_ALREADY_EXISTS khi tên sân đã tồn tại")
    void createPitch_DuplicateName_ThrowsException() {
        PitchRequest request = PitchRequest.builder()
                .name("Sân 5A")
                .pitchTypeId(1L)
                .build();

        when(pitchRepository.existsByNameIgnoreCaseAndIsDeletedFalse("Sân 5A")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> pitchService.createPitch(request));

        assertEquals(ErrorCode.PITCH_NAME_ALREADY_EXISTS, ex.getErrorCode());
        verify(pitchRepository, times(1)).existsByNameIgnoreCaseAndIsDeletedFalse("Sân 5A");
        verify(pitchRepository, never()).save(any(Pitch.class));
    }

    @Test
    @DisplayName("UT-07: Bắn ngoại lệ PITCH_TYPE_NOT_FOUND khi loại sân không tồn tại")
    void createPitch_PitchTypeNotFound_ThrowsException() {
        PitchRequest request = PitchRequest.builder()
                .name("Sân Mới")
                .pitchTypeId(99L)
                .build();

        when(pitchRepository.existsByNameIgnoreCaseAndIsDeletedFalse("Sân Mới")).thenReturn(false);
        when(pitchTypeRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> pitchService.createPitch(request));

        assertEquals(ErrorCode.PITCH_TYPE_NOT_FOUND, ex.getErrorCode());
        verify(pitchTypeRepository, times(1)).findByIdAndIsDeletedFalse(99L);
        verify(pitchRepository, never()).save(any(Pitch.class));
    }

    // ================= 5. Test Cases Cho updatePitch (UC015.2) =================

    @Test
    @DisplayName("UT-08: Cập nhật thông tin tên và mô tả sân bóng thành công")
    void updatePitch_Success() {
        PitchRequest request = PitchRequest.builder()
                .name("Sân 5A VIP")
                .pitchTypeId(1L)
                .description("Nâng cấp cỏ nhân tạo")
                .build();

        when(pitchRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(pitch1));
        when(pitchRepository.existsByNameIgnoreCaseAndIdNotAndIsDeletedFalse("Sân 5A VIP", 10L)).thenReturn(false);
        when(pitchRepository.save(pitch1)).thenReturn(pitch1);
        when(pitchMapper.toPitchResponse(pitch1)).thenReturn(pitchResponse1);

        PitchResponse result = pitchService.updatePitch(10L, request);

        assertNotNull(result);
        verify(pitchRepository, times(1)).findByIdAndIsDeletedFalse(10L);
        verify(pitchRepository, times(1)).existsByNameIgnoreCaseAndIdNotAndIsDeletedFalse("Sân 5A VIP", 10L);
        verify(pitchRepository, times(1)).save(pitch1);
    }

    @Test
    @DisplayName("UT-09: Cập nhật loại sân thành công khi đổi từ sân 5 sang sân 7")
    void updatePitch_ChangePitchType_Success() {
        PitchRequest request = PitchRequest.builder()
                .name("Sân 5A")
                .pitchTypeId(2L)
                .build();

        when(pitchRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(pitch1));
        when(pitchRepository.existsByNameIgnoreCaseAndIdNotAndIsDeletedFalse("Sân 5A", 10L)).thenReturn(false);
        when(pitchTypeRepository.findByIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(pitchType7));
        when(pitchRepository.save(pitch1)).thenReturn(pitch1);
        when(pitchMapper.toPitchResponse(pitch1)).thenReturn(pitchResponse1);

        PitchResponse result = pitchService.updatePitch(10L, request);

        assertNotNull(result);
        assertEquals(pitchType7, pitch1.getPitchType());
        verify(pitchTypeRepository, times(1)).findByIdAndIsDeletedFalse(2L);
    }

    @Test
    @DisplayName("UT-10: Bắn ngoại lệ PITCH_NAME_ALREADY_EXISTS khi đổi tên trùng với sân khác")
    void updatePitch_DuplicateName_ThrowsException() {
        PitchRequest request = PitchRequest.builder()
                .name("Sân 5B")
                .pitchTypeId(1L)
                .build();

        when(pitchRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(pitch1));
        when(pitchRepository.existsByNameIgnoreCaseAndIdNotAndIsDeletedFalse("Sân 5B", 10L)).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> pitchService.updatePitch(10L, request));

        assertEquals(ErrorCode.PITCH_NAME_ALREADY_EXISTS, ex.getErrorCode());
        verify(pitchRepository, never()).save(any(Pitch.class));
    }

    // ================= 6. Test Cases Cho updatePitchStatus (UC015.3) =================

    @Test
    @DisplayName("UT-11: Chuyển trạng thái sân từ ACTIVE sang MAINTENANCE (Bảo trì)")
    void updatePitchStatus_ToMaintenance_Success() {
        UpdatePitchStatusRequest request = UpdatePitchStatusRequest.builder()
                .status(PitchStatus.MAINTENANCE)
                .build();

        when(pitchRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(pitch1));
        when(pitchRepository.save(pitch1)).thenReturn(pitch1);

        pitchResponse1.setStatus(PitchStatus.MAINTENANCE);
        when(pitchMapper.toPitchResponse(pitch1)).thenReturn(pitchResponse1);

        PitchResponse result = pitchService.updatePitchStatus(10L, request);

        assertNotNull(result);
        assertEquals(PitchStatus.MAINTENANCE, pitch1.getStatus());
        verify(pitchRepository, times(1)).save(pitch1);
    }

    @Test
    @DisplayName("UT-12: Chuyển trạng thái sân từ MAINTENANCE sang ACTIVE (Hoạt động)")
    void updatePitchStatus_ToActive_Success() {
        pitch1.setStatus(PitchStatus.MAINTENANCE);
        UpdatePitchStatusRequest request = UpdatePitchStatusRequest.builder()
                .status(PitchStatus.ACTIVE)
                .build();

        when(pitchRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(pitch1));
        when(pitchRepository.save(pitch1)).thenReturn(pitch1);

        pitchResponse1.setStatus(PitchStatus.ACTIVE);
        when(pitchMapper.toPitchResponse(pitch1)).thenReturn(pitchResponse1);

        PitchResponse result = pitchService.updatePitchStatus(10L, request);

        assertNotNull(result);
        assertEquals(PitchStatus.ACTIVE, pitch1.getStatus());
        verify(pitchRepository, times(1)).save(pitch1);
    }

    @Test
    @DisplayName("UT-13: Bắn ngoại lệ PITCH_STATUS_INVALID khi status truyền vào là null")
    void updatePitchStatus_NullStatus_ThrowsException() {
        UpdatePitchStatusRequest request = new UpdatePitchStatusRequest();

        AppException ex = assertThrows(AppException.class, () -> pitchService.updatePitchStatus(10L, request));

        assertEquals(ErrorCode.PITCH_STATUS_INVALID, ex.getErrorCode());
        verify(pitchRepository, never()).save(any(Pitch.class));
    }

    // ================= 7. Test Cases Cho deletePitch (Xóa mềm) =================

    @Test
    @DisplayName("UT-14: Xóa mềm sân bóng thành công")
    void deletePitch_Success() {
        when(pitchRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(pitch1));

        pitchService.deletePitch(10L);

        assertTrue(pitch1.getIsDeleted());
        verify(pitchRepository, times(1)).save(pitch1);
    }

    @Test
    @DisplayName("UT-15: Bắn ngoại lệ PITCH_NOT_FOUND khi xóa ID không tồn tại")
    void deletePitch_NotFound_ThrowsException() {
        when(pitchRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> pitchService.deletePitch(99L));

        assertEquals(ErrorCode.PITCH_NOT_FOUND, ex.getErrorCode());
        verify(pitchRepository, never()).save(any(Pitch.class));
    }
}
