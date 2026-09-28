package com.fpms.mapper;

import com.fpms.dto.response.PitchTypeResponse;
import com.fpms.entity.PitchType;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PitchTypeMapper {

    PitchTypeResponse toPitchTypeResponse(PitchType pitchType);

    List<PitchTypeResponse> toPitchTypeResponseList(List<PitchType> pitchTypes);
}
