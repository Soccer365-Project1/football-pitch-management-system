package com.fpms.service;

import com.fpms.dto.request.HolidayRequest;
import com.fpms.dto.response.HolidayResponse;

import java.util.List;

public interface HolidayService {

    List<HolidayResponse> getAllHolidays(Integer year);

    HolidayResponse getHolidayById(Long id);

    HolidayResponse createHoliday(HolidayRequest request);

    HolidayResponse updateHoliday(Long id, HolidayRequest request);

    void deleteHoliday(Long id);
}
