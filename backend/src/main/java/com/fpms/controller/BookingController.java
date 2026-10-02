package com.fpms.controller;

import com.fpms.common.response.ApiResponse;
import com.fpms.dto.response.ScheduleGridResponse;
import com.fpms.entity.Booking;
import com.fpms.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.fpms.security.UserPrincipal;
import jakarta.validation.Valid;
import com.fpms.dto.request.BookingCreationRequest;
import com.fpms.dto.response.BookingResponse;
import com.fpms.service.BookingService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @GetMapping("/schedule-grid")
    public ResponseEntity<ApiResponse<ScheduleGridResponse>> getScheduleGrid(
            @RequestParam LocalDate date,
            @RequestParam(required = false) Long pitchTypeId
    ) {
        ScheduleGridResponse response = bookingService.getScheduleGrid(date, pitchTypeId);
        return ResponseEntity.ok(ApiResponse.success("Success", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingCreationRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        BookingResponse response = bookingService.createBooking(request, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success("Đặt sân thành công", response));
    }
}
