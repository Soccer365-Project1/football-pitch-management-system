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
import com.fpms.dto.request.BookingCancelRequest;
import com.fpms.dto.request.BookingCreationRequest;
import com.fpms.dto.response.BookingResponse;
import com.fpms.service.BookingService;

import java.time.LocalDate;
import java.util.List;

import com.fpms.common.response.PageResponse;
import com.fpms.dto.response.BookingSummaryResponse;
import com.fpms.entity.enums.BookingStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;

@Tag(name = "Booking", description = "Các API liên quan đến quản lý và đặt sân bóng")
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Operation(summary = "Lấy ma trận lưới lịch đặt sân", description = "Xem trạng thái các sân bóng và khung giờ trong một ngày cụ thể")
    @GetMapping("/schedule-grid")
    public ResponseEntity<ApiResponse<ScheduleGridResponse>> getScheduleGrid(
            @RequestParam LocalDate date,
            @RequestParam(required = false) Long pitchTypeId
    ) {
        ScheduleGridResponse response = bookingService.getScheduleGrid(date, pitchTypeId);
        return ResponseEntity.ok(ApiResponse.success("Success", response));
    }

    @Operation(summary = "Tạo mới đơn đặt sân", description = "Tạo đơn đặt sân trực tuyến (giữ chỗ) hoặc tại quầy")
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingCreationRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        BookingResponse response = bookingService.createBooking(request, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success("Đặt sân thành công", response));
    }

    @Operation(summary = "Tạo đơn và lấy URL thanh toán cọc VNPAY")
    @PostMapping("/deposit-checkout")
    public ResponseEntity<ApiResponse<com.fpms.dto.response.BookingDepositResponse>> checkoutDeposit(
            @Valid @RequestBody BookingCreationRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        com.fpms.dto.response.BookingDepositResponse response = bookingService.createBookingWithDepositPayment(request, userPrincipal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Khởi tạo đặt sân và thanh toán cọc thành công", response));
    }

    @Operation(summary = "Lấy lịch sử đặt sân cá nhân", description = "Lấy danh sách các đơn đặt sân của tài khoản đang đăng nhập từ JWT Token")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/my-bookings")
    public ResponseEntity<ApiResponse<PageResponse<BookingSummaryResponse>>> getMyBookings(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<BookingSummaryResponse> response = bookingService.getMyBookings(
                userPrincipal, status, fromDate, toDate, page, size
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử đặt sân thành công", response));
    }

    @Operation(summary = "Xem chi tiết đơn đặt sân cá nhân", description = "Xem chi tiết một đơn đặt sân của tài khoản đang đăng nhập")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/my-bookings/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getMyBookingDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        BookingResponse response = bookingService.getMyBookingDetail(id, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chi tiết đơn thành công", response));
    }

    @Operation(summary = "Hủy đơn đặt sân", description = "Hủy đơn đặt sân nếu trạng thái là Chờ duyệt hoặc Đã xác nhận và hiện tại chưa đến giờ bắt đầu ca đá")
    @SecurityRequirement(name = "BearerAuth")
    @RequestMapping(value = "/{id}/cancel", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Long id,
            @Valid @RequestBody BookingCancelRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        BookingResponse response = bookingService.cancelBooking(id, request, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn đặt sân thành công", response));
    }
}
