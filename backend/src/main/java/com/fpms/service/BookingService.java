package com.fpms.service;

import com.fpms.dto.request.BookingCreationRequest;
import com.fpms.dto.request.BookingSearchRequest;
import com.fpms.dto.response.BookingResponse;

import java.time.LocalDate;
import com.fpms.dto.response.ScheduleGridResponse;
import com.fpms.security.UserPrincipal;

import com.fpms.common.response.PageResponse;
import com.fpms.entity.enums.BookingStatus;
import com.fpms.dto.response.BookingDepositResponse;
import com.fpms.dto.response.BookingSummaryResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface BookingService {
    BookingResponse createBooking(BookingCreationRequest request, UserPrincipal userPrincipal);
    BookingDepositResponse createBookingWithDepositPayment(BookingCreationRequest request, UserPrincipal userPrincipal, HttpServletRequest httpRequest);
    ScheduleGridResponse getScheduleGrid(LocalDate date, Long pitchTypeId);
    PageResponse<BookingSummaryResponse> getMyBookings(
            BookingSearchRequest request,
            UserPrincipal currentUser
    );
    BookingResponse getMyBookingDetail(Long bookingId, UserPrincipal currentUser);
    BookingResponse cancelBooking(Long bookingId, com.fpms.dto.request.BookingCancelRequest request, UserPrincipal currentUser);
    BookingDepositResponse repayBooking(Long bookingId, UserPrincipal userPrincipal, HttpServletRequest httpRequest);
}
