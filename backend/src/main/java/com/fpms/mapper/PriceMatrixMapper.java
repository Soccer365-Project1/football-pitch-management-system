package com.fpms.mapper;

import com.fpms.dto.response.PriceMatrixResponse;
import com.fpms.dto.response.PriceRateResponse;
import com.fpms.entity.PriceMatrix;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PriceMatrixMapper {

    @Mapping(target = "pitchTypeId", source = "pitchType.id")
    @Mapping(target = "pitchTypeName", source = "pitchType.name")
    @Mapping(target = "playerCapacity", source = "pitchType.playerCapacity")
    PriceMatrixResponse toPriceMatrixResponse(PriceMatrix priceMatrix);

    List<PriceMatrixResponse> toPriceMatrixResponseList(List<PriceMatrix> priceMatrices);

    PriceRateResponse toPriceRateResponse(PriceMatrix priceMatrix);

    List<PriceRateResponse> toPriceRateResponseList(List<PriceMatrix> priceMatrices);
}
