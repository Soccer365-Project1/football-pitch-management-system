package com.fpms.service;

import com.fpms.common.response.PageResponse;
import com.fpms.dto.request.AdminBookingCancelRequest;
import com.fpms.dto.request.AdminBookingSearchRequest;
import com.fpms.dto.request.CollectPaymentRequest;
import com.fpms.dto.request.CounterBookingCreationRequest;
import com.fpms.dto.response.AdminBookingDetailResponse;
import com.fpms.dto.response.AdminBookingSummaryResponse;
import com.fpms.security.UserPrincipal;

public interface AdminBookingService {

    /**
     * Tra cứu và lọc danh sách đơn đặt sân phân trang theo đa tiêu chí.
     */
    PageResponse<AdminBookingSummaryResponse> getBookings(AdminBookingSearchRequest request);

    /**
     * Lấy thông tin chi tiết một đơn đặt sân kèm lịch sử các giao dịch thanh toán.
     */
    AdminBookingDetailResponse getBookingDetail(Long id);

    /**
     * SRS UC011.1: Tạo đơn đặt sân trực tiếp tại quầy cho khách vãng lai hoặc khách hội viên.
     */
    AdminBookingDetailResponse createCounterBooking(CounterBookingCreationRequest request, UserPrincipal currentUser);

    /**
     * SRS UC011.2: Duyệt đơn đặt sân trực tuyến đã cọc tiền chuyển sang trạng thái ĐÃ XÁC NHẬN.
     */
    AdminBookingDetailResponse approveBooking(Long id, UserPrincipal currentUser);

    /**
     * SRS UC011.3: Hủy đơn đặt sân, xử lý hoàn tiền cọc nếu có và giải phóng ca đá ngay lập tức.
     */
    AdminBookingDetailResponse cancelAndRefundBooking(Long id, AdminBookingCancelRequest request, UserPrincipal currentUser);

    /**
     * SRS UC011.4: Thu tiền còn lại (70%) + phụ phí phát sinh, lưu giao dịch và hoàn tất đóng ca.
     */
    AdminBookingDetailResponse collectPayment(Long id, CollectPaymentRequest request, UserPrincipal currentUser);
}
