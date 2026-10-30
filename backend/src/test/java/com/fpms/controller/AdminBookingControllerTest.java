package com.fpms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fpms.common.response.PageResponse;
import com.fpms.dto.request.AdminBookingCancelRequest;
import com.fpms.dto.request.AdminBookingSearchRequest;
import com.fpms.dto.request.CollectPaymentRequest;
import com.fpms.dto.request.CounterBookingCreationRequest;
import com.fpms.dto.response.AdminBookingDetailResponse;
import com.fpms.dto.response.AdminBookingSummaryResponse;
import com.fpms.entity.enums.BookingStatus;
import com.fpms.entity.enums.BookingType;
import com.fpms.entity.enums.PaymentMethodCode;
import com.fpms.exception.GlobalExceptionHandler;
import com.fpms.service.AdminBookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminBookingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AdminBookingService adminBookingService;

    @InjectMocks
    private AdminBookingController adminBookingController;

    private AdminBookingDetailResponse sampleDetail;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminBookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleDetail = AdminBookingDetailResponse.builder()
                .id(1L)
                .bookingCode("#B173000000000")
                .customerName("Nguyễn Văn Khách")
                .customerPhone("0987654321")
                .pitchName("Sân A1")
                .pitchTypeName("Sân 5 người")
                .bookingDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(19, 30))
                .totalPitchAmount(BigDecimal.valueOf(500000))
                .depositAmount(BigDecimal.valueOf(150000))
                .remainingAmount(BigDecimal.valueOf(350000))
                .additionalFee(BigDecimal.ZERO)
                .grandTotal(BigDecimal.valueOf(500000))
                .bookingType(BookingType.AT_COUNTER)
                .status(BookingStatus.CONFIRMED)
                .transactions(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/bookings: Lấy danh sách đơn phân trang thành công")
    void getBookings_Success() throws Exception {
        AdminBookingSummaryResponse summary = AdminBookingSummaryResponse.builder()
                .id(1L)
                .bookingCode("#B173000000000")
                .customerName("Nguyễn Văn Khách")
                .customerPhone("0987654321")
                .pitchName("Sân A1")
                .bookingDate(LocalDate.now().plusDays(1))
                .status(BookingStatus.CONFIRMED)
                .totalAmount(BigDecimal.valueOf(500000))
                .build();

        PageResponse<AdminBookingSummaryResponse> pageResponse = PageResponse.<AdminBookingSummaryResponse>builder()
                .pageNo(1)
                .pageSize(10)
                .totalElements(1L)
                .totalPages(1)
                .isLast(true)
                .items(List.of(summary))
                .build();

        when(adminBookingService.getBookings(any(AdminBookingSearchRequest.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/admin/bookings")
                        .param("keyword", "0987")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items[0].bookingCode").value("#B173000000000"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/bookings/{id}: Xem chi tiết đơn thành công")
    void getBookingDetail_Success() throws Exception {
        when(adminBookingService.getBookingDetail(1L)).thenReturn(sampleDetail);

        mockMvc.perform(get("/api/v1/admin/bookings/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.bookingCode").value("#B173000000000"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/bookings/counter: Tạo đơn tại quầy thành công (UC011.1)")
    void createCounterBooking_Success() throws Exception {
        CounterBookingCreationRequest request = CounterBookingCreationRequest.builder()
                .pitchId(1L)
                .timeSlotId(1L)
                .bookingDate(LocalDate.now().plusDays(1))
                .guestName("Nguyễn Văn Khách")
                .guestPhone("0987654321")
                .paidAmount(BigDecimal.valueOf(150000))
                .paymentMethod(PaymentMethodCode.CASH)
                .build();

        when(adminBookingService.createCounterBooking(any(CounterBookingCreationRequest.class), any()))
                .thenReturn(sampleDetail);

        mockMvc.perform(post("/api/v1/admin/bookings/counter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.bookingType").value("AT_COUNTER"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/bookings/{id}/approve: Duyệt đơn online thành công (UC011.2)")
    void approveBooking_Success() throws Exception {
        sampleDetail.setStatus(BookingStatus.CONFIRMED);
        when(adminBookingService.approveBooking(eq(1L), any())).thenReturn(sampleDetail);

        mockMvc.perform(patch("/api/v1/admin/bookings/1/approve")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/bookings/{id}/cancel-refund: Hủy đơn và hoàn cọc thành công (UC011.3)")
    void cancelAndRefundBooking_Success() throws Exception {
        sampleDetail.setStatus(BookingStatus.REFUNDED);
        AdminBookingCancelRequest request = AdminBookingCancelRequest.builder()
                .cancelReason("Mưa to ngập sân bóng")
                .refundMethod(PaymentMethodCode.CASH)
                .refundAmount(BigDecimal.valueOf(150000))
                .build();

        when(adminBookingService.cancelAndRefundBooking(eq(1L), any(AdminBookingCancelRequest.class), any()))
                .thenReturn(sampleDetail);

        mockMvc.perform(post("/api/v1/admin/bookings/1/cancel-refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/bookings/{id}/collect-payment: Thu tiền còn lại và đóng ca (UC011.4)")
    void collectPayment_Success() throws Exception {
        sampleDetail.setStatus(BookingStatus.COMPLETED);
        CollectPaymentRequest request = CollectPaymentRequest.builder()
                .additionalFee(BigDecimal.valueOf(50000))
                .paymentMethod(PaymentMethodCode.CASH)
                .paymentNote("Nước uống 5 chai")
                .build();

        when(adminBookingService.collectPayment(eq(1L), any(CollectPaymentRequest.class), any()))
                .thenReturn(sampleDetail);

        mockMvc.perform(post("/api/v1/admin/bookings/1/collect-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }
}
