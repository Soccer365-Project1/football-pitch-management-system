package com.fpms.mapper;

import com.fpms.dto.response.TransactionDetailResponse;
import com.fpms.dto.response.TransactionSummaryResponse;
import com.fpms.entity.Booking;
import com.fpms.entity.PaymentTransaction;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalTime;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PaymentTransactionMapper {

    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "paymentMethodCode", source = "paymentMethod.code")
    @Mapping(target = "paymentMethodName", source = "paymentMethod.name")
    TransactionSummaryResponse toSummaryResponse(PaymentTransaction txn);

    @Mapping(target = "paymentMethodId", source = "paymentMethod.id")
    @Mapping(target = "paymentMethodCode", source = "paymentMethod.code")
    @Mapping(target = "paymentMethodName", source = "paymentMethod.name")
    @Mapping(target = "processedById", source = "processedBy.id")
    @Mapping(target = "processedByName", source = "processedBy.fullName")
    @Mapping(target = "processedByEmail", source = "processedBy.email")
    @Mapping(target = "bookingId", source = "booking.id")
    @Mapping(target = "bookingCode", source = "booking.bookingCode")
    @Mapping(target = "bookingStatus", source = "booking.status")
    @Mapping(target = "customerName", source = "booking", qualifiedByName = "mapCustomerName")
    @Mapping(target = "customerPhone", source = "booking", qualifiedByName = "mapCustomerPhone")
    @Mapping(target = "pitchName", source = "booking.pitchNameSnapshot")
    @Mapping(target = "bookingDate", source = "booking.bookingDate")
    @Mapping(target = "timeSlot", source = "booking", qualifiedByName = "mapTimeSlot")
    @Mapping(target = "bookingTotalAmount", source = "booking.totalPitchAmount")
    @Mapping(target = "bookingAdditionalFee", source = "booking.additionalFee")
    @Mapping(target = "bookingDepositAmount", source = "booking.depositAmount")
    @Mapping(target = "bookingRemainingAmount", source = "booking.remainingAmount")
    TransactionDetailResponse toDetailResponse(PaymentTransaction txn);

    @Named("mapCustomerName")
    default String mapCustomerName(Booking booking) {
        if (booking == null) return null;
        if (booking.getCustomer() != null && booking.getCustomer().getFullName() != null) {
            return booking.getCustomer().getFullName();
        }
        return booking.getGuestName();
    }

    @Named("mapCustomerPhone")
    default String mapCustomerPhone(Booking booking) {
        if (booking == null) return null;
        if (booking.getCustomer() != null && booking.getCustomer().getPhoneNumber() != null) {
            return booking.getCustomer().getPhoneNumber();
        }
        return booking.getGuestPhone();
    }

    @Named("mapTimeSlot")
    default String mapTimeSlot(Booking booking) {
        if (booking == null) return null;
        LocalTime start = booking.getStartTimeSnapshot();
        LocalTime end = booking.getEndTimeSnapshot();
        if (start != null && end != null) {
            return start.toString().substring(0, 5) + " - " + end.toString().substring(0, 5);
        }
        return null;
    }
}

