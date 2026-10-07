package com.fpms.service.impl;

import com.fpms.dto.request.PriceRateItemRequest;
import com.fpms.dto.request.SavePitchTypePricingRequest;
import com.fpms.dto.response.PitchTypePricingMatrixResponse;
import com.fpms.dto.response.PriceMatrixResponse;
import com.fpms.dto.response.PriceRateResponse;
import com.fpms.entity.PitchType;
import com.fpms.entity.PriceMatrix;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.mapper.PriceMatrixMapper;
import com.fpms.repository.PitchTypeRepository;
import com.fpms.repository.PriceMatrixRepository;
import com.fpms.service.PriceMatrixService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceMatrixServiceImpl implements PriceMatrixService {

    private final PriceMatrixRepository priceMatrixRepository;
    private final PitchTypeRepository pitchTypeRepository;
    private final PriceMatrixMapper priceMatrixMapper;

    @Override
    @Transactional(readOnly = true)
    public PitchTypePricingMatrixResponse getPricingByPitchTypeId(Long pitchTypeId) {
        log.info("Lấy bảng giá theo loại sân, pitchTypeId: {}", pitchTypeId);
        PitchType pitchType = pitchTypeRepository.findByIdAndIsDeletedFalse(pitchTypeId)
                .orElseThrow(() -> new AppException(ErrorCode.PITCH_TYPE_NOT_FOUND));

        List<PriceMatrix> matrices = priceMatrixRepository
                .findAllByPitchTypeIdOrderByDayTypeAscIsPeakHourAsc(pitchTypeId);

        List<PriceRateResponse> rates = priceMatrixMapper.toPriceRateResponseList(matrices);

        return PitchTypePricingMatrixResponse.builder()
                .pitchTypeId(pitchType.getId())
                .pitchTypeName(pitchType.getName())
                .playerCapacity(pitchType.getPlayerCapacity())
                .rates(rates)
                .build();
    }

    @Override
    @Transactional
    public PitchTypePricingMatrixResponse savePricingForPitchType(Long pitchTypeId, SavePitchTypePricingRequest request) {
        log.info("Lưu bảng giá (Upsert) cho loại sân ID: {}, số lượng ô giá: {}", pitchTypeId, request.getRates().size());
        PitchType pitchType = pitchTypeRepository.findByIdAndIsDeletedFalse(pitchTypeId)
                .orElseThrow(() -> new AppException(ErrorCode.PITCH_TYPE_NOT_FOUND));

        List<PriceMatrix> toSave = new ArrayList<>();
        for (PriceRateItemRequest item : request.getRates()) {
            Optional<PriceMatrix> existingOpt = priceMatrixRepository
                    .findByPitchTypeIdAndIsPeakHourAndDayType(pitchTypeId, item.getIsPeakHour(), item.getDayType());

            if (existingOpt.isPresent()) {
                PriceMatrix existing = existingOpt.get();
                existing.setPrice(item.getPrice());
                toSave.add(existing);
            } else {
                PriceMatrix newMatrix = PriceMatrix.builder()
                        .pitchType(pitchType)
                        .dayType(item.getDayType())
                        .isPeakHour(item.getIsPeakHour())
                        .price(item.getPrice())
                        .build();
                toSave.add(newMatrix);
            }
        }

        List<PriceMatrix> savedList = priceMatrixRepository.saveAll(toSave);
        log.info("Đã lưu thành công {} ô giá cho loại sân: {}", savedList.size(), pitchType.getName());

        List<PriceRateResponse> rates = priceMatrixMapper.toPriceRateResponseList(savedList);

        return PitchTypePricingMatrixResponse.builder()
                .pitchTypeId(pitchType.getId())
                .pitchTypeName(pitchType.getName())
                .playerCapacity(pitchType.getPlayerCapacity())
                .rates(rates)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PitchTypePricingMatrixResponse> getAllPricingMatrices() {
        log.info("Lấy ma trận biểu giá toàn bộ loại sân");
        List<PriceMatrix> allMatrices = priceMatrixRepository.findAllByOrderByPitchTypeIdAscDayTypeAscIsPeakHourAsc();

        Map<PitchType, List<PriceMatrix>> groupedMap = new LinkedHashMap<>();
        for (PriceMatrix matrix : allMatrices) {
            groupedMap.computeIfAbsent(matrix.getPitchType(), k -> new ArrayList<>()).add(matrix);
        }

        List<PitchTypePricingMatrixResponse> responses = new ArrayList<>();
        for (Map.Entry<PitchType, List<PriceMatrix>> entry : groupedMap.entrySet()) {
            PitchType pt = entry.getKey();
            responses.add(PitchTypePricingMatrixResponse.builder()
                    .pitchTypeId(pt.getId())
                    .pitchTypeName(pt.getName())
                    .playerCapacity(pt.getPlayerCapacity())
                    .rates(priceMatrixMapper.toPriceRateResponseList(entry.getValue()))
                    .build());
        }

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceMatrixResponse> getAllPrices(Long pitchTypeId) {
        List<PriceMatrix> matrices;
        if (pitchTypeId != null) {
            matrices = priceMatrixRepository.findAllByPitchTypeIdOrderByDayTypeAscIsPeakHourAsc(pitchTypeId);
        } else {
            matrices = priceMatrixRepository.findAllByOrderByPitchTypeIdAscDayTypeAscIsPeakHourAsc();
        }
        return priceMatrixMapper.toPriceMatrixResponseList(matrices);
    }

    @Override
    @Transactional(readOnly = true)
    public PriceMatrixResponse getPriceById(Long id) {
        PriceMatrix matrix = priceMatrixRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRICE_MATRIX_NOT_FOUND));
        return priceMatrixMapper.toPriceMatrixResponse(matrix);
    }

    @Override
    @Transactional
    public void deleteSinglePrice(Long id) {
        if (!priceMatrixRepository.existsById(id)) {
            throw new AppException(ErrorCode.PRICE_MATRIX_NOT_FOUND);
        }
        priceMatrixRepository.deleteById(id);
        log.info("Đã xóa ô giá ID: {}", id);
    }
}
