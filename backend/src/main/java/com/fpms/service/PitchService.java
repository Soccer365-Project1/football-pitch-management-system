package com.fpms.service;

import com.fpms.common.response.PageResponse;
import com.fpms.dto.request.PitchRequest;
import com.fpms.dto.request.UpdatePitchStatusRequest;
import com.fpms.dto.response.PitchResponse;
import com.fpms.dto.response.PitchTypeResponse;
import com.fpms.entity.enums.PitchStatus;

import java.util.List;

public interface PitchService {

    PageResponse<PitchResponse> getPitches(String keyword, Long pitchTypeId, PitchStatus status, int page, int size);

    PitchResponse getPitchById(Long id);

    PitchResponse createPitch(PitchRequest request);

    PitchResponse updatePitch(Long id, PitchRequest request);

    PitchResponse updatePitchStatus(Long id, UpdatePitchStatusRequest request);

    void deletePitch(Long id);

    List<PitchTypeResponse> getAllPitchTypes();
}
