package com.fpms.mapper;

import com.fpms.dto.request.PitchRequest;
import com.fpms.dto.response.PitchResponse;
import com.fpms.entity.Pitch;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {PitchTypeMapper.class}, builder = @Builder(disableBuilder = true))
public interface PitchMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pitchType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    Pitch toPitch(PitchRequest request);

    PitchResponse toPitchResponse(Pitch pitch);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pitchType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    void updatePitchFromRequest(PitchRequest request, @MappingTarget Pitch pitch);
}
