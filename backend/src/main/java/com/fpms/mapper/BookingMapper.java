package com.fpms.mapper;

import com.fpms.dto.request.BookingCreationRequest;
import com.fpms.dto.response.BookingResponse;
import com.fpms.entity.Booking;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface BookingMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bookingCode", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "staff", ignore = true)
    @Mapping(target = "pitch", ignore = true)
    @Mapping(target = "timeSlot", ignore = true)
    @Mapping(target = "priceSnapshot", ignore = true)
    @Mapping(target = "startTimeSnapshot", ignore = true)
    @Mapping(target = "endTimeSnapshot", ignore = true)
    @Mapping(target = "pitchNameSnapshot", ignore = true)
    @Mapping(target = "totalPitchAmount", ignore = true)
    @Mapping(target = "depositAmount", ignore = true)
    @Mapping(target = "additionalFee", ignore = true)
    @Mapping(target = "remainingAmount", ignore = true)
    @Mapping(target = "bookingType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "holdExpiresAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    Booking toBooking(BookingCreationRequest request);

    @Mapping(source = "staff.fullName", target = "staffName")
    BookingResponse toBookingResponse(Booking booking);
}
