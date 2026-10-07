package com.fpms.service;

import com.fpms.dto.request.SavePitchTypePricingRequest;
import com.fpms.dto.response.PitchTypePricingMatrixResponse;
import com.fpms.dto.response.PriceMatrixResponse;

import java.util.List;

public interface PriceMatrixService {

    PitchTypePricingMatrixResponse getPricingByPitchTypeId(Long pitchTypeId);

    PitchTypePricingMatrixResponse savePricingForPitchType(Long pitchTypeId, SavePitchTypePricingRequest request);

    List<PitchTypePricingMatrixResponse> getAllPricingMatrices();

    List<PriceMatrixResponse> getAllPrices(Long pitchTypeId);

    PriceMatrixResponse getPriceById(Long id);

    void deleteSinglePrice(Long id);
}
