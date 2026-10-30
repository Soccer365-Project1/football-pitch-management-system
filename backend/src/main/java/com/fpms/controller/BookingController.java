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
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import com.fpms.dto.request.BookingCheckOutRequest;
import com.fpms.dto.request.BookingCancelRequest;
import com.fpms.dto.request.BookingCreationRequest;
import com.fpms.dto.request.BookingSearchRequest;
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

    @Operation(summary = "Thanh toán lại tiền cọc", description = "Tạo URL thanh toán VNPAY mới cho đơn đặt sân đang chờ giữ chỗ")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/{id}/repay")
    public ResponseEntity<ApiResponse<com.fpms.dto.response.BookingDepositResponse>> repayBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        com.fpms.dto.response.BookingDepositResponse response = bookingService.repayBooking(id, userPrincipal, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Khởi tạo liên kết thanh toán lại thành công", response));
    }

    @Operation(summary = "Lấy lịch sử đặt sân cá nhân", description = "Lấy danh sách các đơn đặt sân của tài khoản đang đăng nhập, hỗ trợ tìm kiếm theo từ khóa, lọc theo trạng thái, ngày đá và phân trang")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/my-bookings")
    public ResponseEntity<ApiResponse<PageResponse<BookingSummaryResponse>>> getMyBookings(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @ModelAttribute BookingSearchRequest request
    ) {
        PageResponse<BookingSummaryResponse> response = bookingService.getMyBookings(request, userPrincipal);
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

    @Operation(summary = "Lấy ma trận ca đá cho nhân viên/quản trị viên", description = "Xem chi tiết ca đá kèm thông tin khách hàng, check-in, check-out")
    @SecurityRequirement(name = "BearerAuth")
    @GetMapping("/admin/timeline")
    public ResponseEntity<ApiResponse<com.fpms.dto.response.AdminTimelineResponse>> getAdminTimeline(
            @RequestParam LocalDate date,
            @RequestParam(required = false) Long pitchTypeId
    ) {
        com.fpms.dto.response.AdminTimelineResponse response = bookingService.getAdminTimeline(date, pitchTypeId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin ca đá thành công", response));
    }

    @Operation(summary = "Duyệt đơn đặt sân", description = "Chuyển trạng thái từ Chờ duyệt sang Đã xác nhận")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PutMapping("/admin/{id}/approve")
    public ResponseEntity<ApiResponse<BookingResponse>> approveBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        BookingResponse response = bookingService.approveBooking(id, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success("Duyệt đơn đặt sân thành công", response));
    }

    @Operation(summary = "Nhận sân (Check-in)", description = "Chuyển trạng thái từ Đã xác nhận sang Đang sử dụng khi giờ hiện tại >= giờ bắt đầu ca đá")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PutMapping("/admin/{id}/check-in")
    public ResponseEntity<ApiResponse<BookingResponse>> checkInBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        BookingResponse response = bookingService.checkInBooking(id, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success("Nhận sân thành công", response));
    }

    @Operation(summary = "Trả sân & Kết thúc ca đá (Check-out)", description = "Thu tiền nốt 70% (CASH hoặc BANK_TRANSFER) và chuyển trạng thái sang Hoàn tất")
    @SecurityRequirement(name = "BearerAuth")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PutMapping("/admin/{id}/check-out")
    public ResponseEntity<ApiResponse<BookingResponse>> checkOutBooking(
            @PathVariable Long id,
            @Valid @RequestBody BookingCheckOutRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        BookingResponse response = bookingService.checkOutBooking(id, request, userPrincipal);
        return ResponseEntity.ok(ApiResponse.success("Trả sân và thanh toán thành công", response));
    }
}