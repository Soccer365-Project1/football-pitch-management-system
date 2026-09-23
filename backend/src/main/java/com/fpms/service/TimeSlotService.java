package com.fpms.service;

import com.fpms.dto.request.TimeSlotRequest;
import com.fpms.dto.response.TimeSlotResponse;

import java.util.List;

public interface TimeSlotService {

    List<TimeSlotResponse> getAllTimeSlots(Boolean activeOnly);

    TimeSlotResponse getTimeSlotById(Long id);

    TimeSlotResponse createTimeSlot(TimeSlotRequest request);

    TimeSlotResponse updateTimeSlot(Long id, TimeSlotRequest request);

    void deleteTimeSlot(Long id);
}
