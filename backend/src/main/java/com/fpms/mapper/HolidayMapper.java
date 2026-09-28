package com.fpms.mapper;

import com.fpms.dto.request.HolidayRequest;
import com.fpms.dto.response.HolidayResponse;
import com.fpms.entity.Holiday;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface HolidayMapper {

    @Mapping(target = "id", ignore = true)
    Holiday toHoliday(HolidayRequest request);

    HolidayResponse toHolidayResponse(Holiday holiday);

    List<HolidayResponse> toHolidayResponseList(List<Holiday> holidays);

    @Mapping(target = "id", ignore = true)
    void updateHolidayFromRequest(HolidayRequest request, @MappingTarget Holiday holiday);
}
