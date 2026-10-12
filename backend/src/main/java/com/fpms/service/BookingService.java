package com.fpms.service;

import com.fpms.dto.request.BookingCreationRequest;
import com.fpms.dto.response.BookingResponse;

import java.time.LocalDate;
import com.fpms.dto.response.ScheduleGridResponse;
import com.fpms.security.UserPrincipal;

import com.fpms.dto.response.BookingDepositResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface BookingService {
    BookingResponse createBooking(BookingCreationRequest request, UserPrincipal userPrincipal);
    BookingDepositResponse createBookingWithDepositPayment(BookingCreationRequest request, UserPrincipal userPrincipal, HttpServletRequest httpRequest);
    ScheduleGridResponse getScheduleGrid(LocalDate date, Long pitchTypeId);
}
