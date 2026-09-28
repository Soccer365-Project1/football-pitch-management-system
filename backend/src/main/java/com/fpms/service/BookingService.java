package com.fpms.service;

import com.fpms.dto.request.BookingCreationRequest;
import com.fpms.dto.response.BookingResponse;

import java.time.LocalDate;
import com.fpms.dto.response.ScheduleGridResponse;
import com.fpms.security.UserPrincipal;

public interface BookingService {
    BookingResponse createBooking(BookingCreationRequest request, UserPrincipal userPrincipal);
    ScheduleGridResponse getScheduleGrid(LocalDate date, Long pitchTypeId);
}
