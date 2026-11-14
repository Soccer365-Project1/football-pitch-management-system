package com.fpms.service.impl;

import com.fpms.common.response.PageResponse;
import com.fpms.dto.request.AdminBookingCancelRequest;
import com.fpms.dto.request.AdminBookingRejectCancelRequest;
import com.fpms.dto.request.AdminBookingSearchRequest;
import com.fpms.dto.request.CollectPaymentRequest;
import com.fpms.dto.request.CounterBookingCreationRequest;
import com.fpms.dto.response.AdminBookingDetailResponse;
import com.fpms.dto.response.AdminBookingSummaryResponse;
import com.fpms.dto.response.BookingEventDto;
import com.fpms.entity.*;
import com.fpms.entity.enums.*;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.mapper.AdminBookingMapper;
import com.fpms.repository.*;
import com.fpms.repository.specification.BookingSpecification;
import com.fpms.security.UserPrincipal;
import com.fpms.service.AdminBookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBookingServiceImpl implements AdminBookingService {

    private final BookingRepository bookingRepository;
    private final PitchRepository pitchRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final HolidayRepository holidayRepository;
    private final PriceMatrixRepository priceMatrixRepository;
    private final UserRepository userRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final BookingCancellationRepository bookingCancellationRepository;
    private final AdminBookingMapper adminBookingMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminBookingSummaryResponse> getBookings(AdminBookingSearchRequest request) {
        Specification<Booking> spec = BookingSpecification.filterAdminBookings(request);
        Pageable pageable = request != null ? request.toPageable() : AdminBookingSearchRequest.builder().build().toPageable();

        Page<Booking> page = bookingRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(adminBookingMapper::toSummaryResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminBookingDetailResponse getBookingDetail(Long id) {
        Booking booking = getValidBooking(id);
        return toDetailResponse(booking);
    }

    @Override
    @Transactional
    public AdminBookingDetailResponse createCounterBooking(CounterBookingCreationRequest request, UserPrincipal currentUser) {
        // 1. Kiểm tra tính khả dụng của sân và ca đá
        Pitch pitch = getActivePitch(request.getPitchId());
        TimeSlot timeSlot = getTimeSlot(request.getTimeSlotId());
        validateBookingSlotAvailability(pitch, timeSlot, request.getBookingDate());

        // 2. Xác định đơn giá theo ma trận giá
        BigDecimal totalPitchAmount = resolvePitchPrice(pitch, timeSlot, request.getBookingDate());

        // 3. Tính toán số tiền thanh toán trước tại quầy
        if (request.getPaidAmount() != null && request.getPaidAmount().compareTo(totalPitchAmount) > 0) {
            throw new AppException(ErrorCode.PAID_AMOUNT_EXCEEDS_TOTAL);
        }

        BigDecimal deposit30Percent = totalPitchAmount.multiply(new BigDecimal("0.3"));
        BigDecimal paidAmount;
        PaymentType paymentType;

        if (request.getPaidAmount() != null) {
            paidAmount = request.getPaidAmount();
            paymentType = paidAmount.compareTo(totalPitchAmount) >= 0 ? PaymentType.FULL_PAYMENT : PaymentType.DEPOSIT_30;
        } else if (request.getPaymentType() == PaymentType.DEPOSIT_30) {
            paidAmount = deposit30Percent;
            paymentType = PaymentType.DEPOSIT_30;
        } else {
            paidAmount = totalPitchAmount;
            paymentType = PaymentType.FULL_PAYMENT;
        }

        if (paidAmount.compareTo(totalPitchAmount) > 0) {
            throw new AppException(ErrorCode.PAID_AMOUNT_EXCEEDS_TOTAL);
        }

        BigDecimal depositAmount = paidAmount;
        BigDecimal remainingAmount = totalPitchAmount.subtract(paidAmount);

        // 4. Lưu Booking
        User staff = resolveStaff(currentUser);
        User customer = request.getCustomerId() != null
                ? userRepository.findById(request.getCustomerId()).orElse(null)
                : null;

        Booking booking = Booking.builder()
                .bookingCode("#B" + System.currentTimeMillis())
                .pitch(pitch)
                .timeSlot(timeSlot)
                .bookingDate(request.getBookingDate())
                .customer(customer)
                .guestName(request.getGuestName())
                .guestPhone(request.getGuestPhone())
                .staff(staff)
                .priceSnapshot(totalPitchAmount)
                .startTimeSnapshot(timeSlot.getStartTime())
                .endTimeSnapshot(timeSlot.getEndTime())
                .pitchNameSnapshot(pitch.getName())
                .totalPitchAmount(totalPitchAmount)
                .depositAmount(depositAmount)
                .additionalFee(BigDecimal.ZERO)
                .remainingAmount(remainingAmount)
                .bookingType(BookingType.AT_COUNTER)
                .status(BookingStatus.CONFIRMED)
                .customerNote(request.getCustomerNote())
                .build();

        booking = bookingRepository.save(booking);

        // 5. Lưu giao dịch thanh toán ban đầu nếu có trả trước
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            String note = paymentType == PaymentType.FULL_PAYMENT
                    ? "Thanh toán toàn bộ 100% tiền sân tại quầy khi tạo đơn"
                    : "Thanh toán tiền cọc 30% tại quầy khi tạo đơn";
            savePaymentTransaction(booking, staff, request.getPaymentMethod(), paidAmount, paymentType, "TXN_", note);
        }

        // 6. Phát WebSocket & trả kết quả
        sendScheduleEvent("BOOKING_CREATED", booking);
        return toDetailResponse(booking);
    }

    @Override
    @Transactional
    public AdminBookingDetailResponse approveBooking(Long id, UserPrincipal currentUser) {
        Booking booking = getValidBooking(id);

        if (booking.getStatus() != BookingStatus.WAITING_APPROVAL) {
            throw new AppException(ErrorCode.BOOKING_NOT_WAITING_APPROVAL);
        }

        booking.setStaff(resolveStaff(currentUser));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);

        sendScheduleEvent("BOOKING_APPROVED", booking);
        return toDetailResponse(booking);
    }

    @Override
    @Transactional
    public AdminBookingDetailResponse cancelBooking(Long id, AdminBookingCancelRequest request, UserPrincipal currentUser) {
        return processCancellation(id, request, currentUser, request.getIsRefund());
    }

    @Override
    @Transactional
    public AdminBookingDetailResponse cancelAndRefundBooking(Long id, AdminBookingCancelRequest request, UserPrincipal currentUser) {
        return processCancellation(id, request, currentUser, true);
    }

    @Override
    @Transactional
    public AdminBookingDetailResponse rejectCancellation(Long id, AdminBookingRejectCancelRequest request, UserPrincipal currentUser) {
        Booking booking = getValidBooking(id);

        if (booking.getStatus() != BookingStatus.WAITING_CANCELLATION) {
            throw new AppException(ErrorCode.BOOKING_NOT_WAITING_CANCELLATION);
        }

        User staff = resolveStaff(currentUser);
        booking.setStaff(staff);
        booking.setStatus(BookingStatus.CONFIRMED);
        appendCustomerNote(booking, "[Từ chối hủy đơn: " + request.getRejectionReason().trim() + "]");
        booking = bookingRepository.save(booking);

        // Cập nhật trạng thái REJECTED cho bản ghi yêu cầu hủy
        bookingCancellationRepository.findByBookingId(id).ifPresent(cancellation -> {
            cancellation.setRefundStatus(RefundStatus.REJECTED);
            cancellation.setRejectionReason(request.getRejectionReason().trim());
            cancellation.setApprovedBy(staff);
            cancellation.setProcessedAt(LocalDateTime.now());
            bookingCancellationRepository.save(cancellation);
        });

        sendScheduleEvent("BOOKING_APPROVED", booking);
        return toDetailResponse(booking);
    }

    @Override
    @Transactional
    public AdminBookingDetailResponse collectPayment(Long id, CollectPaymentRequest request, UserPrincipal currentUser) {
        Booking booking = getValidBooking(id);

        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.BOOKING_CANNOT_COLLECT_PAYMENT);
        }

        BigDecimal additionalFee = request.getAdditionalFee() != null ? request.getAdditionalFee() : BigDecimal.ZERO;
        if (additionalFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.ADDITIONAL_FEE_INVALID);
        }

        User staff = resolveStaff(currentUser);
        booking.setStaff(staff);

        BigDecimal remainingPitchAmount = booking.getRemainingAmount() != null ? booking.getRemainingAmount() : BigDecimal.ZERO;
        BigDecimal amountToCollect = remainingPitchAmount.add(additionalFee);

        booking.setAdditionalFee(additionalFee);
        booking.setRemainingAmount(BigDecimal.ZERO);
        booking.setStatus(BookingStatus.COMPLETED);
        booking = bookingRepository.save(booking);

        // Lưu giao dịch thu tiền còn lại nếu số tiền > 0
        if (amountToCollect.compareTo(BigDecimal.ZERO) > 0) {
            String note = request.getPaymentNote() != null && !request.getPaymentNote().isEmpty()
                    ? request.getPaymentNote().trim()
                    : "Thu tiền quyết toán và đóng ca";
            savePaymentTransaction(booking, staff, request.getPaymentMethod(), amountToCollect, PaymentType.REMAINING_70, "TXN_FINAL_", note);
        }

        sendScheduleEvent("BOOKING_COMPLETED", booking);
        return toDetailResponse(booking);
    }

    private AdminBookingDetailResponse processCancellation(Long id, AdminBookingCancelRequest request, UserPrincipal currentUser, Boolean forceRefund) {
        Booking booking = getValidBooking(id);

        if (booking.getStatus() == BookingStatus.COMPLETED ||
            booking.getStatus() == BookingStatus.CANCELLED ||
            booking.getStatus() == BookingStatus.REFUNDED) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_CANCELLED_OR_COMPLETED);
        }

        User staff = resolveStaff(currentUser);
        booking.setStaff(staff);
        appendCustomerNote(booking, "[Nhân viên hủy đơn: " + request.getCancelReason().trim() + "]");

        boolean shouldRefund = Boolean.TRUE.equals(forceRefund) || Boolean.TRUE.equals(request.getIsRefund());
        BigDecimal depositAmount = booking.getDepositAmount() != null ? booking.getDepositAmount() : BigDecimal.ZERO;
        BigDecimal refundAmount = request.getRefundAmount() != null ? request.getRefundAmount() : depositAmount;

        if (shouldRefund && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (request.getRefundMethod() == null) {
                throw new AppException(ErrorCode.REFUND_METHOD_REQUIRED);
            }
            booking.setStatus(BookingStatus.REFUNDED);
        } else {
            booking.setStatus(BookingStatus.CANCELLED);
            shouldRefund = false;
        }

        booking = bookingRepository.save(booking);

        // Lưu giao dịch hoàn tiền và đồng bộ bản ghi BookingCancellation nếu có
        if (shouldRefund) {
            PaymentMethod paymentMethod = getPaymentMethod(request.getRefundMethod());
            PaymentTransaction refundTxn = PaymentTransaction.builder()
                    .transactionCode(generateTransactionCode("TXN_REFUND_"))
                    .booking(booking)
                    .paymentMethod(paymentMethod)
                    .processedBy(staff)
                    .amount(refundAmount)
                    .paymentType(PaymentType.REFUND)
                    .status(TransactionStatus.SUCCESS)
                    .note("Hoàn tiền cọc: " + request.getCancelReason().trim())
                    .build();

            paymentTransactionRepository.save(refundTxn);

            bookingCancellationRepository.findByBookingId(id).ifPresent(cancellation -> {
                cancellation.setRefundStatus(RefundStatus.COMPLETED);
                cancellation.setRefundMethod(paymentMethod);
                cancellation.setApprovedBy(staff);
                cancellation.setProcessedAt(LocalDateTime.now());
                cancellation.setNote(request.getCancelReason().trim());
                bookingCancellationRepository.save(cancellation);
            });
        }

        sendScheduleEvent("BOOKING_CANCELLED", booking);
        return toDetailResponse(booking);
    }


    private Booking getValidBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (Boolean.TRUE.equals(booking.getIsDeleted())) {
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
        }
        return booking;
    }

    private User resolveStaff(UserPrincipal currentUser) {
        if (currentUser != null && currentUser.getId() != null) {
            return userRepository.findById(currentUser.getId()).orElse(null);
        }
        return null;
    }

    private AdminBookingDetailResponse toDetailResponse(Booking booking) {
        List<PaymentTransaction> transactions = paymentTransactionRepository
                .findByBookingIdOrderByCreatedAtDesc(booking.getId());
        return adminBookingMapper.toDetailResponse(booking, transactions);
    }

    private void appendCustomerNote(Booking booking, String noteText) {
        if (noteText == null || noteText.trim().isEmpty()) {
            return;
        }
        String current = booking.getCustomerNote();
        String trimmed = noteText.trim();
        booking.setCustomerNote(current != null && !current.isEmpty() ? current + " " + trimmed : trimmed);
    }

    private String generateTransactionCode(String prefix) {
        return prefix + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Pitch getActivePitch(Long pitchId) {
        Pitch pitch = pitchRepository.findById(pitchId)
                .orElseThrow(() -> new AppException(ErrorCode.PITCH_NOT_FOUND));
        if (pitch.getStatus() != PitchStatus.ACTIVE) {
            throw new AppException(ErrorCode.PITCH_NOT_ACTIVE);
        }
        return pitch;
    }

    private TimeSlot getTimeSlot(Long timeSlotId) {
        return timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new AppException(ErrorCode.TIME_SLOT_NOT_FOUND));
    }

    private PaymentMethod getPaymentMethod(PaymentMethodCode methodCode) {
        PaymentMethodCode code = methodCode != null ? methodCode : PaymentMethodCode.CASH;
        return paymentMethodRepository.findByCode(code).orElse(null);
    }

    private void validateBookingSlotAvailability(Pitch pitch, TimeSlot timeSlot, LocalDate bookingDate) {
        LocalDate today = LocalDate.now();
        if (bookingDate.isBefore(today)) {
            throw new AppException(ErrorCode.BOOKING_DATE_INVALID);
        }
        if (bookingDate.isAfter(today.plusDays(30))) {
            throw new AppException(ErrorCode.BOOKING_DATE_EXCEEDS_LIMIT);
        }
        if (bookingDate.isEqual(today) && timeSlot.getEndTime().isBefore(LocalTime.now())) {
            throw new AppException(ErrorCode.TIME_SLOT_ALREADY_PASSED);
        }

        boolean isOccupied = bookingRepository.existsByPitchIdAndTimeSlotIdAndBookingDateAndStatusNotIn(
                pitch.getId(),
                timeSlot.getId(),
                bookingDate,
                Arrays.asList(BookingStatus.CANCELLED, BookingStatus.REFUNDED)
        );

        if (isOccupied) {
            throw new AppException(ErrorCode.BOOKING_TIME_SLOT_ALREADY_BOOKED);
        }
    }

    private BigDecimal resolvePitchPrice(Pitch pitch, TimeSlot timeSlot, LocalDate bookingDate) {
        DayType dayType = determineDayType(bookingDate);
        PriceMatrix priceMatrix = priceMatrixRepository.findByPitchTypeIdAndIsPeakHourAndDayType(
                pitch.getPitchType().getId(),
                timeSlot.getIsPeakHour(),
                dayType
        ).orElseThrow(() -> new AppException(ErrorCode.PRICE_MATRIX_NOT_FOUND));
        return priceMatrix.getPrice();
    }

    private void savePaymentTransaction(Booking booking, User staff, PaymentMethodCode methodCode,
                                         BigDecimal amount, PaymentType paymentType, String prefix, String note) {
        PaymentMethod paymentMethod = getPaymentMethod(methodCode);
        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionCode(generateTransactionCode(prefix))
                .booking(booking)
                .paymentMethod(paymentMethod)
                .processedBy(staff)
                .amount(amount)
                .paymentType(paymentType)
                .status(TransactionStatus.SUCCESS)
                .note(note)
                .build();

        paymentTransactionRepository.save(transaction);
    }

    private DayType determineDayType(LocalDate date) {
        if (holidayRepository.existsByHolidayDate(date)) {
            return DayType.HOLIDAY;
        }
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return DayType.WEEKEND;
        }
        return DayType.WEEKDAY;
    }

    private void sendScheduleEvent(String action, Booking booking) {
        BookingEventDto event = BookingEventDto.builder()
                .action(action)
                .pitchId(booking.getPitch().getId())
                .timeSlotId(booking.getTimeSlot().getId())
                .date(booking.getBookingDate().toString())
                .status(booking.getStatus().name())
                .build();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        messagingTemplate.convertAndSend("/topic/schedule", event);
                    } catch (Exception e) {
                        log.error("Failed to send WebSocket message for booking: {}", booking.getBookingCode(), e);
                    }
                }
            });
        } else {
            messagingTemplate.convertAndSend("/topic/schedule", event);
        }
    }
}
