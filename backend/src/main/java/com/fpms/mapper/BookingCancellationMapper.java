package com.fpms.mapper;

import com.fpms.dto.response.BookingCancellationResponse;
import com.fpms.entity.Booking;
import com.fpms.entity.BookingCancellation;
import com.fpms.entity.User;
import com.fpms.entity.enums.RefundStatus;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface BookingCancellationMapper {

    @Mapping(source = "booking.id", target = "bookingId")
    @Mapping(source = "booking.bookingCode", target = "bookingCode")
    BookingCancellationResponse toResponse(BookingCancellation cancellation);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "refundMethod", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "processedAt", ignore = true)
    @Mapping(target = "note", ignore = true)
    @Mapping(target = "refundStatus", constant = "PENDING")
    @Mapping(target = "requestedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(source = "booking", target = "booking")
    @Mapping(source = "requestedBy", target = "requestedBy")
    @Mapping(source = "reason", target = "reason")
    @Mapping(source = "refundAmount", target = "refundAmount")
    BookingCancellation toEntity(Booking booking, User requestedBy, String reason, BigDecimal refundAmount);
}
