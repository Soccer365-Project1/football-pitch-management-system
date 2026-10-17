package com.fpms.service.impl;

import com.fpms.dto.request.BookingCancelRequest;
import com.fpms.entity.Booking;
import com.fpms.entity.BookingCancellation;
import com.fpms.entity.User;
import com.fpms.mapper.BookingCancellationMapper;
import com.fpms.repository.BookingCancellationRepository;
import com.fpms.service.BookingCancellationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BookingCancellationServiceImpl implements BookingCancellationService {

    private final BookingCancellationRepository bookingCancellationRepository;
    private final BookingCancellationMapper bookingCancellationMapper;

    @Override
    @Transactional
    public BookingCancellation createCancellation(Booking booking, User requestedBy, BookingCancelRequest request) {
        String cancelReason = (request != null && request.getReason() != null && !request.getReason().trim().isEmpty())
                ? request.getReason().trim()
                : "Khách hàng yêu cầu hủy đơn";

        BigDecimal refundAmount = booking.getDepositAmount() != null
                ? booking.getDepositAmount()
                : BigDecimal.ZERO;

        BookingCancellation cancellation = bookingCancellationMapper.toEntity(
                booking,
                requestedBy,
                cancelReason,
                refundAmount
        );

        return bookingCancellationRepository.save(cancellation);
    }
}
