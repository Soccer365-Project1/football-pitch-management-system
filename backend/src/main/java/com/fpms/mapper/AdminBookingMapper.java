package com.fpms.mapper;

import com.fpms.dto.response.AdminBookingDetailResponse;
import com.fpms.dto.response.AdminBookingSummaryResponse;
import com.fpms.dto.response.PaymentTransactionResponse;
import com.fpms.entity.Booking;
import com.fpms.entity.PaymentTransaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AdminBookingMapper {

    public AdminBookingSummaryResponse toSummaryResponse(Booking booking) {
        if (booking == null) {
            return null;
        }

        String customerName = booking.getCustomer() != null && booking.getCustomer().getFullName() != null
                ? booking.getCustomer().getFullName()
                : (booking.getGuestName() != null ? booking.getGuestName() : "");

        String customerPhone = booking.getCustomer() != null && booking.getCustomer().getPhoneNumber() != null
                ? booking.getCustomer().getPhoneNumber()
                : (booking.getGuestPhone() != null ? booking.getGuestPhone() : "");

        String pitchTypeName = booking.getPitch() != null && booking.getPitch().getPitchType() != null
                ? booking.getPitch().getPitchType().getName()
                : "";

        String staffName = booking.getStaff() != null ? booking.getStaff().getFullName() : null;

        BigDecimal totalPitch = booking.getTotalPitchAmount() != null ? booking.getTotalPitchAmount() : BigDecimal.ZERO;
        BigDecimal addFee = booking.getAdditionalFee() != null ? booking.getAdditionalFee() : BigDecimal.ZERO;
        BigDecimal grandTotal = totalPitch.add(addFee);

        return AdminBookingSummaryResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .customerName(customerName)
                .customerPhone(customerPhone)
                .isMember(booking.getCustomer() != null)
                .pitchName(booking.getPitchNameSnapshot() != null ? booking.getPitchNameSnapshot() : (booking.getPitch() != null ? booking.getPitch().getName() : ""))
                .pitchTypeName(pitchTypeName)
                .bookingDate(booking.getBookingDate())
                .startTime(booking.getStartTimeSnapshot() != null ? booking.getStartTimeSnapshot() : (booking.getTimeSlot() != null ? booking.getTimeSlot().getStartTime() : null))
                .endTime(booking.getEndTimeSnapshot() != null ? booking.getEndTimeSnapshot() : (booking.getTimeSlot() != null ? booking.getTimeSlot().getEndTime() : null))
                .depositAmount(booking.getDepositAmount())
                .remainingAmount(booking.getRemainingAmount())
                .additionalFee(booking.getAdditionalFee())
                .totalAmount(grandTotal)
                .bookingType(booking.getBookingType())
                .status(booking.getStatus())
                .staffName(staffName)
                .createdAt(booking.getCreatedAt())
                .build();
    }

    public AdminBookingDetailResponse toDetailResponse(Booking booking, List<PaymentTransaction> transactions) {
        if (booking == null) {
            return null;
        }

        String customerName = booking.getCustomer() != null && booking.getCustomer().getFullName() != null
                ? booking.getCustomer().getFullName()
                : (booking.getGuestName() != null ? booking.getGuestName() : "");

        String customerPhone = booking.getCustomer() != null && booking.getCustomer().getPhoneNumber() != null
                ? booking.getCustomer().getPhoneNumber()
                : (booking.getGuestPhone() != null ? booking.getGuestPhone() : "");

        String customerEmail = booking.getCustomer() != null ? booking.getCustomer().getEmail() : null;
        Long customerId = booking.getCustomer() != null ? booking.getCustomer().getId() : null;

        String staffName = booking.getStaff() != null ? booking.getStaff().getFullName() : null;

        String pitchName = booking.getPitchNameSnapshot() != null ? booking.getPitchNameSnapshot() : (booking.getPitch() != null ? booking.getPitch().getName() : "");
        String pitchTypeName = booking.getPitch() != null && booking.getPitch().getPitchType() != null ? booking.getPitch().getPitchType().getName() : "";

        BigDecimal totalPitch = booking.getTotalPitchAmount() != null ? booking.getTotalPitchAmount() : BigDecimal.ZERO;
        BigDecimal addFee = booking.getAdditionalFee() != null ? booking.getAdditionalFee() : BigDecimal.ZERO;
        BigDecimal grandTotal = totalPitch.add(addFee);

        List<PaymentTransactionResponse> transactionResponses = transactions != null
                ? transactions.stream().map(this::toTransactionResponse).collect(Collectors.toList())
                : Collections.emptyList();

        return AdminBookingDetailResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .customerId(customerId)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .customerEmail(customerEmail)
                .staffName(staffName)
                .pitchName(pitchName)
                .pitchTypeName(pitchTypeName)
                .bookingDate(booking.getBookingDate())
                .startTime(booking.getStartTimeSnapshot() != null ? booking.getStartTimeSnapshot() : (booking.getTimeSlot() != null ? booking.getTimeSlot().getStartTime() : null))
                .endTime(booking.getEndTimeSnapshot() != null ? booking.getEndTimeSnapshot() : (booking.getTimeSlot() != null ? booking.getTimeSlot().getEndTime() : null))
                .totalPitchAmount(booking.getTotalPitchAmount())
                .depositAmount(booking.getDepositAmount())
                .additionalFee(booking.getAdditionalFee())
                .remainingAmount(booking.getRemainingAmount())
                .grandTotal(grandTotal)
                .bookingType(booking.getBookingType())
                .status(booking.getStatus())
                .customerNote(booking.getCustomerNote())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .transactions(transactionResponses)
                .build();
    }

    public PaymentTransactionResponse toTransactionResponse(PaymentTransaction txn) {
        if (txn == null) {
            return null;
        }

        String methodName = txn.getPaymentMethod() != null ? txn.getPaymentMethod().getName() : null;
        String methodCode = txn.getPaymentMethod() != null && txn.getPaymentMethod().getCode() != null
                ? txn.getPaymentMethod().getCode().name()
                : null;
        String processedByName = txn.getProcessedBy() != null ? txn.getProcessedBy().getFullName() : null;

        return PaymentTransactionResponse.builder()
                .id(txn.getId())
                .transactionCode(txn.getTransactionCode())
                .amount(txn.getAmount())
                .paymentType(txn.getPaymentType())
                .paymentMethodName(methodName)
                .paymentMethodCode(methodCode)
                .status(txn.getStatus())
                .referenceCode(txn.getReferenceCode())
                .note(txn.getNote())
                .processedByName(processedByName)
                .createdAt(txn.getCreatedAt())
                .build();
    }
}
