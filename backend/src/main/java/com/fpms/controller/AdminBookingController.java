package com.fpms.controller;

import com.fpms.common.response.ApiResponse;
import com.fpms.common.response.PageResponse;
import com.fpms.dto.request.AdminBookingCancelRequest;
import com.fpms.dto.request.AdminBookingSearchRequest;
import com.fpms.dto.request.CollectPaymentRequest;
import com.fpms.dto.request.CounterBookingCreationRequest;
import com.fpms.dto.response.AdminBookingDetailResponse;
import com.fpms.dto.response.AdminBookingSummaryResponse;
import com.fpms.security.UserPrincipal;
import com.fpms.service.AdminBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "4. Admin Booking Operations", description = "Các API Phân hệ Quản lý đơn đặt sân dành cho Admin & Staff (SRS UC011)")
@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    @Operation(summary = "Lấy danh sách đơn đặt sân phân trang", description = "Lọc theo khoảng ngày, trạng thái, sân, loại sân và tìm kiếm nhanh theo SĐT/Tên/Mã đơn")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminBookingSummaryResponse>>> getBookings(
            @Valid @ModelAttribute AdminBookingSearchRequest request) {
        PageResponse<AdminBookingSummaryResponse> response = adminBookingService.getBookings(request);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn đặt sân thành công", response));
    }

    @Operation(summary = "Xem chi tiết một đơn đặt sân", description = "Lấy đầy đủ thông tin khách hàng, ca đá, snapshot giá và lịch sử giao dịch thanh toán")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminBookingDetailResponse>> getBookingDetail(@PathVariable Long id) {
        AdminBookingDetailResponse response = adminBookingService.getBookingDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đơn đặt sân thành công", response));
    }

    @Operation(summary = "UC011.1: Tạo đơn đặt sân trực tiếp tại quầy", description = "Tạo đơn tại quầy cho khách vãng lai hoặc khách có sẵn tài khoản, chốt giá và xác nhận đơn ngay lập tức")
    @PostMapping("/counter")
    public ResponseEntity<ApiResponse<AdminBookingDetailResponse>> createCounterBooking(
            @Valid @RequestBody CounterBookingCreationRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        AdminBookingDetailResponse response = adminBookingService.createCounterBooking(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo đơn đặt sân tại quầy thành công", response));
    }

    @Operation(summary = "UC011.2: Duyệt đơn đặt sân online", description = "Chuyển đơn đặt online từ Chờ duyệt (WAITING_APPROVAL) sang Đã xác nhận (CONFIRMED)")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<AdminBookingDetailResponse>> approveBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        AdminBookingDetailResponse response = adminBookingService.approveBooking(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Duyệt đơn đặt sân thành công", response));
    }

    @Operation(summary = "UC011.3: Hủy đơn đặt sân và hoàn cọc", description = "Hủy đơn với lý do bắt buộc, chọn phương thức hoàn cọc và giải phóng ca đá ngay lập tức")
    @PostMapping("/{id}/cancel-refund")
    public ResponseEntity<ApiResponse<AdminBookingDetailResponse>> cancelAndRefundBooking(
            @PathVariable Long id,
            @Valid @RequestBody AdminBookingCancelRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        AdminBookingDetailResponse response = adminBookingService.cancelAndRefundBooking(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn đặt sân thành công", response));
    }

    @Operation(summary = "UC011.4: Thu tiền còn lại và đóng ca đá", description = "Ghi nhận phụ phí phát sinh, tính số tiền 70% còn lại, lưu giao dịch thanh toán và chuyển trạng thái sang COMPLETED")
    @PostMapping("/{id}/collect-payment")
    public ResponseEntity<ApiResponse<AdminBookingDetailResponse>> collectPayment(
            @PathVariable Long id,
            @Valid @RequestBody CollectPaymentRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        AdminBookingDetailResponse response = adminBookingService.collectPayment(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Thu tiền và đóng ca đá thành công", response));
    }
}
