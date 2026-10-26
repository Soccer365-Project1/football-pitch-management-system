package com.fpms.service.impl;

import com.fpms.common.response.PageResponse;
import com.fpms.dto.request.AdminBookingCancelRequest;
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
    private final AdminBookingMapper adminBookingMapper;
    private final SimpMessagingTemplate messagingTemplate;

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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminBookingSummaryResponse> getBookings(AdminBookingSearchRequest request) {
        Specification<Booking> spec = BookingSpecification.filterAdminBookings(request);
        Pageable pageable = request != null ? request.toPageable() : AdminBookingSearchRequest.builder().build().toPageable();

        Page<Booking> page = bookingRepository.findAll(spec, pageable);
        Page<AdminBookingSummaryResponse> responsePage = page.map(adminBookingMapper::toSummaryResponse);

        return PageResponse.from(responsePage);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminBookingDetailResponse getBookingDetail(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (Boolean.TRUE.equals(booking.getIsDeleted())) {
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
        }

        List<PaymentTransaction> transactions = paymentTransactionRepository.findByBookingIdOrderByCreatedAtDesc(id);
        return adminBookingMapper.toDetailResponse(booking, transactions);
    }

    @Override
    @Transactional
    public AdminBookingDetailResponse createCounterBooking(CounterBookingCreationRequest request, UserPrincipal currentUser) {
        // 1. Kiểm tra Sân hoạt động
        Pitch pitch = pitchRepository.findById(request.getPitchId())
                .orElseThrow(() -> new AppException(ErrorCode.PITCH_NOT_FOUND));

        if (pitch.getStatus() != PitchStatus.ACTIVE) {
            throw new AppException(ErrorCode.PITCH_NOT_ACTIVE);
        }

        // 2. Kiểm tra Ca đá
        TimeSlot timeSlot = timeSlotRepository.findById(request.getTimeSlotId())
                .orElseThrow(() -> new AppException(ErrorCode.TIME_SLOT_NOT_FOUND));

        // 3. Kiểm tra Ngày và Giờ ca đá
        LocalDate today = LocalDate.now();
        if (request.getBookingDate().isBefore(today)) {
            throw new AppException(ErrorCode.BOOKING_DATE_INVALID);
        }
        if (request.getBookingDate().isEqual(today) && timeSlot.getEndTime().isBefore(LocalTime.now())) {
            throw new AppException(ErrorCode.TIME_SLOT_ALREADY_PASSED);
        }

        // 4. Kiểm tra xung đột trùng lịch ca đá
        boolean isOccupied = bookingRepository.existsByPitchIdAndTimeSlotIdAndBookingDateAndStatusNotIn(
                pitch.getId(),
                timeSlot.getId(),
                request.getBookingDate(),
                Arrays.asList(BookingStatus.CANCELLED, BookingStatus.REFUNDED)
        );

        if (isOccupied) {
            throw new AppException(ErrorCode.BOOKING_TIME_SLOT_ALREADY_BOOKED);
        }

        // 5. Xác định đơn giá theo Ma trận giá PriceMatrix
        DayType dayType = determineDayType(request.getBookingDate());
        PriceMatrix priceMatrix = priceMatrixRepository.findByPitchTypeIdAndIsPeakHourAndDayType(
                pitch.getPitchType().getId(),
                timeSlot.getIsPeakHour(),
                dayType
        ).orElseThrow(() -> new AppException(ErrorCode.PRICE_MATRIX_NOT_FOUND));

        // 6. Xác định nhân viên và khách hàng
        User staff = null;
        if (currentUser != null && currentUser.getId() != null) {
            staff = userRepository.findById(currentUser.getId()).orElse(null);
        }

        User customer = null;
        if (request.getCustomerId() != null) {
            customer = userRepository.findById(request.getCustomerId()).orElse(null);
        }

        BigDecimal totalPitchAmount = priceMatrix.getPrice();
        BigDecimal deposit30Percent = totalPitchAmount.multiply(new BigDecimal("0.3"));

        // Xác định số tiền thanh toán trước dựa trên lựa chọn (DEPOSIT_30 hoặc FULL_PAYMENT)
        BigDecimal paidAmount;
        PaymentType pType;

        if (request.getPaymentType() == PaymentType.DEPOSIT_30) {
            paidAmount = deposit30Percent;
            pType = PaymentType.DEPOSIT_30;
        } else if (request.getPaymentType() == PaymentType.FULL_PAYMENT) {
            paidAmount = totalPitchAmount;
            pType = PaymentType.FULL_PAYMENT;
        } else if (request.getPaidAmount() != null) {
            paidAmount = request.getPaidAmount();
            pType = paidAmount.compareTo(totalPitchAmount) >= 0 ? PaymentType.FULL_PAYMENT : PaymentType.DEPOSIT_30;
        } else {
            // Mặc định tại quầy: thanh toán 100% toàn bộ tiền sân
            paidAmount = totalPitchAmount;
            pType = PaymentType.FULL_PAYMENT;
        }

        if (paidAmount.compareTo(totalPitchAmount) > 0) {
            throw new AppException(ErrorCode.PAID_AMOUNT_EXCEEDS_TOTAL);
        }

        BigDecimal depositAmount = paidAmount;
        BigDecimal remainingAmount = totalPitchAmount.subtract(paidAmount);

        // 7. Tạo bản ghi Booking
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

        // 8. Nếu khách có thanh toán trước tại quầy -> Lưu PaymentTransaction
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            PaymentMethodCode methodCode = request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethodCode.CASH;
            PaymentMethod paymentMethod = paymentMethodRepository.findByCode(methodCode).orElse(null);

            String txnCode = "TXN_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .transactionCode(txnCode)
                    .booking(booking)
                    .paymentMethod(paymentMethod)
                    .processedBy(staff)
                    .amount(paidAmount)
                    .paymentType(pType)
                    .status(TransactionStatus.SUCCESS)
                    .note(pType == PaymentType.FULL_PAYMENT
                            ? "Thanh toán toàn bộ 100% tiền sân tại quầy khi tạo đơn"
                            : "Thanh toán tiền cọc 30% tại quầy khi tạo đơn")
                    .build();

            paymentTransactionRepository.save(transaction);
        }

        // 9. Đồng bộ Realtime WebSocket
        sendScheduleEvent("BOOKING_CREATED", booking);

        List<PaymentTransaction> transactions = paymentTransactionRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId());
        return adminBookingMapper.toDetailResponse(booking, transactions);
    }

    @Override
    @Transactional
    public AdminBookingDetailResponse approveBooking(Long id, UserPrincipal currentUser) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (Boolean.TRUE.equals(booking.getIsDeleted())) {
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
        }

        if (booking.getStatus() != BookingStatus.WAITING_APPROVAL) {
            throw new AppException(ErrorCode.BOOKING_NOT_WAITING_APPROVAL);
        }

        if (currentUser != null && currentUser.getId() != null) {
            User staff = userRepository.findById(currentUser.getId()).orElse(null);
            booking.setStaff(staff);
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);

        sendScheduleEvent("BOOKING_APPROVED", booking);

        List<PaymentTransaction> transactions = paymentTransactionRepository.findByBookingIdOrderByCreatedAtDesc(id);
        return adminBookingMapper.toDetailResponse(booking, transactions);
    }

    @Override
    @Transactional
    public AdminBookingDetailResponse cancelAndRefundBooking(Long id, AdminBookingCancelRequest request, UserPrincipal currentUser) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (Boolean.TRUE.equals(booking.getIsDeleted())) {
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
        }

        if (booking.getStatus() == BookingStatus.COMPLETED ||
            booking.getStatus() == BookingStatus.CANCELLED ||
            booking.getStatus() == BookingStatus.REFUNDED) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_CANCELLED_OR_COMPLETED);
        }

        User staff = null;
        if (currentUser != null && currentUser.getId() != null) {
            staff = userRepository.findById(currentUser.getId()).orElse(null);
            booking.setStaff(staff);
        }

        // Cập nhật ghi chú lý do hủy
        String reasonText = "[Nhân viên hủy đơn: " + request.getCancelReason().trim() + "]";
        booking.setCustomerNote(booking.getCustomerNote() != null && !booking.getCustomerNote().isEmpty()
                ? booking.getCustomerNote() + " " + reasonText
                : reasonText);

        BigDecimal refundAmount = request.getRefundAmount() != null ? request.getRefundAmount() : booking.getDepositAmount();
        boolean isRefund = refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) > 0;

        booking.setStatus(isRefund ? BookingStatus.REFUNDED : BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);

        // Lưu bản ghi giao dịch hoàn tiền nếu có
        if (isRefund) {
            PaymentMethod paymentMethod = paymentMethodRepository.findByCode(request.getRefundMethod()).orElse(null);
            String txnCode = "TXN_REFUND_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            PaymentTransaction refundTxn = PaymentTransaction.builder()
                    .transactionCode(txnCode)
                    .booking(booking)
                    .paymentMethod(paymentMethod)
                    .processedBy(staff)
                    .amount(refundAmount)
                    .paymentType(PaymentType.REFUND)
                    .status(TransactionStatus.SUCCESS)
                    .note("Hoàn tiền cọc: " + request.getCancelReason().trim())
                    .build();

            paymentTransactionRepository.save(refundTxn);
        }

        // Bắn WebSocket giải phóng ca đá ngay lập tức
        sendScheduleEvent("BOOKING_CANCELLED", booking);

        List<PaymentTransaction> transactions = paymentTransactionRepository.findByBookingIdOrderByCreatedAtDesc(id);
        return adminBookingMapper.toDetailResponse(booking, transactions);
    }

    @Override
    @Transactional
    public AdminBookingDetailResponse collectPayment(Long id, CollectPaymentRequest request, UserPrincipal currentUser) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (Boolean.TRUE.equals(booking.getIsDeleted())) {
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.BOOKING_CANNOT_COLLECT_PAYMENT);
        }

        BigDecimal additionalFee = request.getAdditionalFee() != null ? request.getAdditionalFee() : BigDecimal.ZERO;
        if (additionalFee.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.ADDITIONAL_FEE_INVALID);
        }

        User staff = null;
        if (currentUser != null && currentUser.getId() != null) {
            staff = userRepository.findById(currentUser.getId()).orElse(null);
            booking.setStaff(staff);
        }

        // Số tiền còn lại phải thu = Số tiền sân còn nợ + Phụ phí phát sinh
        BigDecimal remainingPitchAmount = booking.getRemainingAmount() != null ? booking.getRemainingAmount() : BigDecimal.ZERO;
        BigDecimal amountToCollect = remainingPitchAmount.add(additionalFee);

        booking.setAdditionalFee(additionalFee);
        booking.setRemainingAmount(BigDecimal.ZERO);
        booking.setStatus(BookingStatus.COMPLETED);

        booking = bookingRepository.save(booking);

        // Lưu giao dịch thanh toán nếu có số tiền cần thu (> 0)
        if (amountToCollect.compareTo(BigDecimal.ZERO) > 0) {
            PaymentMethod paymentMethod = paymentMethodRepository.findByCode(request.getPaymentMethod()).orElse(null);
            String txnCode = "TXN_FINAL_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            PaymentTransaction finalTxn = PaymentTransaction.builder()
                    .transactionCode(txnCode)
                    .booking(booking)
                    .paymentMethod(paymentMethod)
                    .processedBy(staff)
                    .amount(amountToCollect)
                    .paymentType(PaymentType.REMAINING_70)
                    .status(TransactionStatus.SUCCESS)
                    .note(request.getPaymentNote() != null && !request.getPaymentNote().isEmpty()
                            ? request.getPaymentNote().trim()
                            : "Thu tiền quyết toán và đóng ca")
                    .build();

            paymentTransactionRepository.save(finalTxn);
        }

        // Bắn WebSocket thông báo đóng ca
        sendScheduleEvent("BOOKING_COMPLETED", booking);

        List<PaymentTransaction> transactions = paymentTransactionRepository.findByBookingIdOrderByCreatedAtDesc(id);
        return adminBookingMapper.toDetailResponse(booking, transactions);
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
