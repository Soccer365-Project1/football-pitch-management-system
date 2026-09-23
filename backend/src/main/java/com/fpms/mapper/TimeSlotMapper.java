package com.fpms.mapper;

import com.fpms.dto.request.TimeSlotRequest;
import com.fpms.dto.response.TimeSlotResponse;
import com.fpms.entity.TimeSlot;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface TimeSlotMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    TimeSlot toTimeSlot(TimeSlotRequest request);

    TimeSlotResponse toTimeSlotResponse(TimeSlot timeSlot);

    List<TimeSlotResponse> toTimeSlotResponseList(List<TimeSlot> timeSlots);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    void updateTimeSlotFromRequest(TimeSlotRequest request, @MappingTarget TimeSlot timeSlot);
}
