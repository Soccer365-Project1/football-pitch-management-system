package com.fpms.service;

import com.fpms.dto.request.BookingCancelRequest;
import com.fpms.entity.Booking;
import com.fpms.entity.BookingCancellation;
import com.fpms.entity.User;
import com.fpms.entity.enums.RefundStatus;
import com.fpms.mapper.BookingCancellationMapper;
import com.fpms.repository.BookingCancellationRepository;
import com.fpms.service.impl.BookingCancellationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingCancellationServiceTest {

    @Mock
    private BookingCancellationRepository bookingCancellationRepository;

    @Mock
    private BookingCancellationMapper bookingCancellationMapper;

    @InjectMocks
    private BookingCancellationServiceImpl bookingCancellationService;

    @Test
    @DisplayName("createCancellation - Thành công với đầy đủ lý do và tiền cọc")
    void createCancellation_Success_WithReasonAndDeposit() {
        Booking booking = Booking.builder()
                .bookingCode("BK123")
                .depositAmount(BigDecimal.valueOf(150000))
                .build();
        booking.setId(10L);

        User customer = new User();
        customer.setId(1L);

        BookingCancelRequest request = new BookingCancelRequest();
        request.setReason("Bận đột xuất");

        BookingCancellation mappedEntity = BookingCancellation.builder()
                .id(1L)
                .booking(booking)
                .requestedBy(customer)
                .reason("Bận đột xuất")
                .refundAmount(BigDecimal.valueOf(150000))
                .refundStatus(RefundStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        when(bookingCancellationMapper.toEntity(
                eq(booking),
                eq(customer),
                eq("Bận đột xuất"),
                eq(BigDecimal.valueOf(150000))
        )).thenReturn(mappedEntity);

        when(bookingCancellationRepository.save(mappedEntity)).thenReturn(mappedEntity);

        BookingCancellation result = bookingCancellationService.createCancellation(booking, customer, request);

        assertNotNull(result);
        assertEquals("Bận đột xuất", result.getReason());
        assertEquals(BigDecimal.valueOf(150000), result.getRefundAmount());
        assertEquals(RefundStatus.PENDING, result.getRefundStatus());

        verify(bookingCancellationMapper, times(1)).toEntity(
                eq(booking), eq(customer), eq("Bận đột xuất"), eq(BigDecimal.valueOf(150000))
        );
        verify(bookingCancellationRepository, times(1)).save(mappedEntity);
    }

    @Test
    @DisplayName("createCancellation - Thành công với lý do mặc định khi request rỗng")
    void createCancellation_Success_WithDefaultReason() {
        Booking booking = Booking.builder()
                .bookingCode("BK124")
                .depositAmount(null) // Không có tiền cọc
                .build();
        booking.setId(11L);

        User customer = new User();
        customer.setId(2L);

        BookingCancellation mappedEntity = BookingCancellation.builder()
                .id(2L)
                .booking(booking)
                .requestedBy(customer)
                .reason("Khách hàng yêu cầu hủy đơn")
                .refundAmount(BigDecimal.ZERO)
                .refundStatus(RefundStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        when(bookingCancellationMapper.toEntity(
                eq(booking),
                eq(customer),
                eq("Khách hàng yêu cầu hủy đơn"),
                eq(BigDecimal.ZERO)
        )).thenReturn(mappedEntity);

        when(bookingCancellationRepository.save(mappedEntity)).thenReturn(mappedEntity);

        BookingCancellation result = bookingCancellationService.createCancellation(booking, customer, null);

        assertNotNull(result);
        assertEquals("Khách hàng yêu cầu hủy đơn", result.getReason());
        assertEquals(BigDecimal.ZERO, result.getRefundAmount());
        verify(bookingCancellationRepository, times(1)).save(mappedEntity);
    }
}
