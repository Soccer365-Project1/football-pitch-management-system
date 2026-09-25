package com.fpms.service.impl;

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
import com.fpms.repository.BookingRepository;
import com.fpms.repository.PitchRepository;
import com.fpms.repository.PitchTypeRepository;
import com.fpms.service.PitchService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PitchServiceImpl implements PitchService {

    private final PitchRepository pitchRepository;
    private final PitchTypeRepository pitchTypeRepository;
    private final BookingRepository bookingRepository;
    private final PitchMapper pitchMapper;
    private final PitchTypeMapper pitchTypeMapper;

    @Override
    public PageResponse<PitchResponse> getPitches(String keyword, Long pitchTypeId, PitchStatus status, int page, int size) {
        log.info("Truy vấn danh sách sân bóng - keyword: {}, pitchTypeId: {}, status: {}, page: {}, size: {}",
                keyword, pitchTypeId, status, page, size);

        Specification<Pitch> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (StringUtils.hasText(keyword)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + keyword.trim().toLowerCase() + "%"));
            }
            if (pitchTypeId != null) {
                predicates.add(cb.equal(root.get("pitchType").get("id"), pitchTypeId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Pitch> pitchPage = pitchRepository.findAll(spec, pageable);

        Page<PitchResponse> dtoPage = pitchPage.map(pitchMapper::toPitchResponse);
        return PageResponse.from(dtoPage);
    }

    @Override
    public PitchResponse getPitchById(Long id) {
        log.info("Lấy chi tiết sân bóng ID: {}", id);
        Pitch pitch = pitchRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.PITCH_NOT_FOUND));
        return pitchMapper.toPitchResponse(pitch);
    }

    @Override
    @Transactional
    public PitchResponse createPitch(PitchRequest request) {
        String trimmedName = request.getName().trim();
        log.info("Tạo mới sân bóng với tên: {}, loại sân ID: {}", trimmedName, request.getPitchTypeId());

        if (pitchRepository.existsByNameIgnoreCaseAndIsDeletedFalse(trimmedName)) {
            log.warn("Tên sân bóng đã tồn tại: {}", trimmedName);
            throw new AppException(ErrorCode.PITCH_NAME_ALREADY_EXISTS);
        }

        PitchType pitchType = pitchTypeRepository.findByIdAndIsDeletedFalse(request.getPitchTypeId())
                .orElseThrow(() -> new AppException(ErrorCode.PITCH_TYPE_NOT_FOUND));

        Pitch pitch = pitchMapper.toPitch(request);
        pitch.setName(trimmedName);
        pitch.setPitchType(pitchType);
        pitch.setStatus(PitchStatus.ACTIVE);
        pitch.setIsDeleted(false);

        Pitch savedPitch = pitchRepository.save(pitch);
        log.info("Tạo sân bóng thành công với ID: {}", savedPitch.getId());
        return pitchMapper.toPitchResponse(savedPitch);
    }

    @Override
    @Transactional
    public PitchResponse updatePitch(Long id, PitchRequest request) {
        String trimmedName = request.getName().trim();
        log.info("Cập nhật sân bóng ID: {} với tên mới: {}", id, trimmedName);

        Pitch pitch = pitchRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.PITCH_NOT_FOUND));

        if (pitchRepository.existsByNameIgnoreCaseAndIdNotAndIsDeletedFalse(trimmedName, id)) {
            log.warn("Tên sân bóng đã tồn tại trên sân khác: {}", trimmedName);
            throw new AppException(ErrorCode.PITCH_NAME_ALREADY_EXISTS);
        }

        if (!pitch.getPitchType().getId().equals(request.getPitchTypeId())) {
            PitchType pitchType = pitchTypeRepository.findByIdAndIsDeletedFalse(request.getPitchTypeId())
                    .orElseThrow(() -> new AppException(ErrorCode.PITCH_TYPE_NOT_FOUND));
            pitch.setPitchType(pitchType);
        }

        pitch.setName(trimmedName);
        pitch.setDescription(request.getDescription());

        Pitch updatedPitch = pitchRepository.save(pitch);
        log.info("Cập nhật thông tin sân bóng ID: {} thành công", id);
        return pitchMapper.toPitchResponse(updatedPitch);
    }

    @Override
    @Transactional
    public PitchResponse updatePitchStatus(Long id, UpdatePitchStatusRequest request) {
        log.info("Cập nhật trạng thái sân bóng ID: {} sang {}", id, request.getStatus());

        if (request.getStatus() == null) {
            throw new AppException(ErrorCode.PITCH_STATUS_INVALID);
        }

        Pitch pitch = pitchRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.PITCH_NOT_FOUND));

        pitch.setStatus(request.getStatus());
        Pitch updatedPitch = pitchRepository.save(pitch);
        log.info("Đổi trạng thái sân bóng ID: {} sang {} thành công", id, updatedPitch.getStatus());
        return pitchMapper.toPitchResponse(updatedPitch);
    }

    @Override
    @Transactional
    public void deletePitch(Long id) {
        log.info("Xóa sân bóng ID: {}", id);
        Pitch pitch = pitchRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.PITCH_NOT_FOUND));

        // Ràng buộc nghiệp vụ: Nếu sân đã từng phát sinh đơn đặt sân (Booking), không cho phép xóa
        if (bookingRepository.existsByPitchId(id)) {
            log.warn("Chặn xóa sân bóng ID: {} do đã có dữ liệu đặt sân trong lịch sử", id);
            throw new AppException(ErrorCode.PITCH_HAS_BOOKINGS);
        }

        pitch.setIsDeleted(true);
        pitchRepository.save(pitch);
        log.info("Xóa mềm sân bóng ID: {} thành công", id);
    }

    @Override
    public List<PitchTypeResponse> getAllPitchTypes() {
        log.info("Lấy danh sách các loại sân bóng");
        List<PitchType> pitchTypes = pitchTypeRepository.findAllByIsDeletedFalse();
        return pitchTypeMapper.toPitchTypeResponseList(pitchTypes);
    }
}
