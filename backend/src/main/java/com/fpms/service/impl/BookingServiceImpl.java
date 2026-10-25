package com.fpms.service.impl;

import com.fpms.dto.request.BookingCancelRequest;
import com.fpms.dto.request.BookingCreationRequest;
import com.fpms.dto.request.BookingSearchRequest;
import com.fpms.repository.specification.BookingSpecification;
import org.springframework.data.jpa.domain.Specification;
import com.fpms.dto.response.BookingEventDto;
import com.fpms.dto.response.BookingResponse;
import com.fpms.dto.response.BookingSummaryResponse;
import com.fpms.dto.response.ScheduleGridResponse;
import com.fpms.dto.response.ScheduleGridItemResponse;
import com.fpms.dto.response.PriceItemResponse;
import com.fpms.entity.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import com.fpms.entity.enums.BookingStatus;
import com.fpms.entity.enums.BookingType;
import com.fpms.entity.enums.DayType;
import com.fpms.entity.enums.PitchStatus;
import com.fpms.entity.enums.RoleName;
import com.fpms.exception.AppException;
import com.fpms.exception.ErrorCode;
import com.fpms.mapper.BookingMapper;
import com.fpms.repository.*;
import com.fpms.security.UserPrincipal;
import com.fpms.service.BookingCancellationService;
import com.fpms.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.fpms.common.response.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fpms.config.VNPayConfig;
import com.fpms.dto.response.BookingDepositResponse;
import com.fpms.entity.enums.PaymentMethodCode;
import com.fpms.entity.enums.PaymentType;
import com.fpms.entity.enums.TransactionStatus;
import com.fpms.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final PitchRepository pitchRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final HolidayRepository holidayRepository;
    private final PriceMatrixRepository priceMatrixRepository;
    private final BookingMapper bookingMapper;
    private final UserRepository userRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final VNPayConfig vnPayConfig;
    private final SimpMessagingTemplate messagingTemplate;
    private final BookingCancellationService bookingCancellationService;
    private final BookingCancellationRepository bookingCancellationRepository;

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
    @Transactional
    public BookingResponse createBooking(BookingCreationRequest request, UserPrincipal userPrincipal) {
        // 1. Validate Pitch and TimeSlot
        Pitch pitch = pitchRepository.findById(request.getPitchId())
                .orElseThrow(() -> new AppException(ErrorCode.PITCH_NOT_FOUND));

        if (pitch.getStatus() != PitchStatus.ACTIVE) {
            throw new AppException(ErrorCode.PITCH_NOT_ACTIVE);
        }

        TimeSlot timeSlot = timeSlotRepository.findById(request.getTimeSlotId())
                .orElseThrow(() -> new AppException(ErrorCode.TIME_SLOT_NOT_FOUND));

        java.time.LocalDate today = java.time.LocalDate.now();
        if (request.getBookingDate().isBefore(today)) {
            throw new AppException(ErrorCode.BOOKING_DATE_INVALID);
        }
        if (request.getBookingDate().isEqual(today)) {
            if (timeSlot.getEndTime().isBefore(java.time.LocalTime.now())) {
                throw new AppException(ErrorCode.TIME_SLOT_ALREADY_PASSED);
            }
        }

        // 2. Check for overlapping bookings
        boolean isOccupied = bookingRepository.existsByPitchIdAndTimeSlotIdAndBookingDateAndStatusNotIn(
                pitch.getId(),
                timeSlot.getId(),
                request.getBookingDate(),
                Arrays.asList(BookingStatus.CANCELLED, BookingStatus.REFUNDED)
        );

        if (isOccupied) {
            throw new AppException(ErrorCode.BOOKING_TIME_SLOT_ALREADY_BOOKED); 
        }

        // 3. Determine Price
        DayType dayType = determineDayType(request.getBookingDate());
        PriceMatrix priceMatrix = priceMatrixRepository.findByPitchTypeIdAndIsPeakHourAndDayType(
                pitch.getPitchType().getId(),
                timeSlot.getIsPeakHour(),
                dayType
        ).orElseThrow(() -> new RuntimeException("Price matrix not configured for this time slot"));

        // 4. Determine User and Booking Type
        User user = null;
        if (userPrincipal != null && userPrincipal.getId() != null) {
            user = userRepository.findById(userPrincipal.getId()).orElse(null);
        }

        Booking booking = bookingMapper.toBooking(request);
        
        if (user != null) {
            if (user.getRole().getRoleName() == RoleName.ROLE_STAFF || user.getRole().getRoleName() == RoleName.ROLE_ADMIN) {
                booking.setStaff(user);
                booking.setBookingType(BookingType.AT_COUNTER);
            } else {
                booking.setCustomer(user);
                booking.setBookingType(BookingType.ONLINE);
            }
        } else {
            booking.setBookingType(BookingType.ONLINE);
        }

        booking.setBookingCode("#B" + System.currentTimeMillis());
        booking.setPitch(pitch);
        booking.setTimeSlot(timeSlot);
        booking.setPriceSnapshot(priceMatrix.getPrice());
        booking.setStartTimeSnapshot(timeSlot.getStartTime());
        booking.setEndTimeSnapshot(timeSlot.getEndTime());
        booking.setPitchNameSnapshot(pitch.getName());
        booking.setTotalPitchAmount(priceMatrix.getPrice());
        booking.setDepositAmount(priceMatrix.getPrice().multiply(new BigDecimal("0.3")));
        booking.setAdditionalFee(BigDecimal.ZERO);
        booking.setRemainingAmount(priceMatrix.getPrice().subtract(booking.getDepositAmount()));

        if (booking.getBookingType() == BookingType.AT_COUNTER) {
            booking.setStatus(BookingStatus.CONFIRMED);
        } else {
            booking.setStatus(BookingStatus.PENDING_HOLD);
            booking.setHoldExpiresAt(java.time.LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusMinutes(10));
        }

        booking = bookingRepository.save(booking);

        // Notify clients about the new booking ONLY AFTER transaction commits
        BookingEventDto event = BookingEventDto.builder()
                .action("BOOKING_CREATED")
                .pitchId(booking.getPitch().getId())
                .timeSlotId(booking.getTimeSlot().getId())
                .date(booking.getBookingDate().toString())
                .status(booking.getStatus().name())
                .build();
                
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend("/topic/schedule", event);
            }
        });

        // 5. Map to Response
        return bookingMapper.toBookingResponse(booking);
    }

    @Override
    @Transactional
    public BookingDepositResponse createBookingWithDepositPayment(
            BookingCreationRequest request,
            UserPrincipal userPrincipal,
            HttpServletRequest httpRequest
    ) {
        // 1. Tạo đơn booking (giữ chỗ tạm 10 phút, tính cọc 30%)
        BookingResponse bookingResponse = createBooking(request, userPrincipal);
        Booking booking = bookingRepository.findByBookingCode(bookingResponse.getBookingCode())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        // 2. Lấy phương thức VNPAY
        PaymentMethod vnpayMethod = paymentMethodRepository.findByCode(PaymentMethodCode.VNPAY)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_METHOD_NOT_FOUND));

        // 3. Khởi tạo PaymentTransaction PENDING
        String txnCode = "TXN_" + System.currentTimeMillis() + "_" + booking.getId();
        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionCode(txnCode)
                .booking(booking)
                .paymentMethod(vnpayMethod)
                .amount(booking.getDepositAmount())
                .paymentType(PaymentType.DEPOSIT_30)
                .status(TransactionStatus.PENDING)
                .note("Đặt cọc 30% đơn " + booking.getBookingCode())
                .build();
        paymentTransactionRepository.save(transaction);

        // 4. Sinh URL thanh toán VNPAY (thời gian hết hạn đồng bộ theo holdExpiresAt của booking)
        String paymentUrl = buildVNPayUrl(booking, txnCode, httpRequest);

        return BookingDepositResponse.builder()
                .bookingCode(booking.getBookingCode())
                .bookingId(booking.getId())
                .depositAmount(booking.getDepositAmount())
                .paymentUrl(paymentUrl)
                .build();
    }

    @Override
    public BookingDepositResponse repayBooking(Long bookingId, UserPrincipal userPrincipal, HttpServletRequest httpRequest) {
        if (userPrincipal == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (Boolean.TRUE.equals(booking.getIsDeleted())) {
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
        }

        if (booking.getCustomer() == null || !booking.getCustomer().getId().equals(userPrincipal.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (booking.getStatus() != BookingStatus.PENDING_HOLD) {
            throw new AppException(ErrorCode.BOOKING_CANNOT_REPAY);
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        if (booking.getHoldExpiresAt() == null || !booking.getHoldExpiresAt().isAfter(now.plusMinutes(2))) {
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setHoldExpiresAt(null);
            bookingRepository.save(booking);

            if (booking.getPitch() != null && booking.getTimeSlot() != null) {
                BookingEventDto event = BookingEventDto.builder()
                        .action("BOOKING_CANCELLED")
                        .pitchId(booking.getPitch().getId())
                        .timeSlotId(booking.getTimeSlot().getId())
                        .date(booking.getBookingDate() != null ? booking.getBookingDate().toString() : null)
                        .status(BookingStatus.CANCELLED.name())
                        .build();
                messagingTemplate.convertAndSend("/topic/schedule", event);
            }

            throw new AppException(ErrorCode.HOLDING_TIME_TOO_SHORT);
        }

        PaymentMethod vnpayMethod = paymentMethodRepository.findByCode(PaymentMethodCode.VNPAY)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_METHOD_NOT_FOUND));

        String txnCode = "TXN_" + System.currentTimeMillis() + "_" + booking.getId();
        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionCode(txnCode)
                .booking(booking)
                .paymentMethod(vnpayMethod)
                .amount(booking.getDepositAmount())
                .paymentType(PaymentType.DEPOSIT_30)
                .status(TransactionStatus.PENDING)
                .note("Thanh toán lại cọc 30% đơn " + booking.getBookingCode())
                .build();
        paymentTransactionRepository.save(transaction);

        String paymentUrl = buildVNPayUrl(booking, txnCode, httpRequest);

        return BookingDepositResponse.builder()
                .bookingCode(booking.getBookingCode())
                .bookingId(booking.getId())
                .depositAmount(booking.getDepositAmount())
                .paymentUrl(paymentUrl)
                .build();
    }

    private String buildVNPayUrl(Booking booking, String txnCode, HttpServletRequest httpRequest) {
        long amountInVnpay = booking.getDepositAmount().multiply(BigDecimal.valueOf(100)).longValue();
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amountInVnpay));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", txnCode);
        vnpParams.put("vnp_OrderInfo", "Dat coc san bong don " + booking.getBookingCode());
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", VNPayUtil.getIpAddress(httpRequest));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        ZoneId vnZone = ZoneId.of("Asia/Ho_Chi_Minh");
        ZonedDateTime now = ZonedDateTime.now(vnZone);
        vnpParams.put("vnp_CreateDate", now.format(formatter));

        LocalDateTime holdExpiresAt = booking.getHoldExpiresAt();
        if (holdExpiresAt == null) {
            holdExpiresAt = now.toLocalDateTime().plusMinutes(10);
        }
        ZonedDateTime expireZoned = holdExpiresAt.atZone(vnZone);
        vnpParams.put("vnp_ExpireDate", expireZoned.format(formatter));

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII)).append('&');
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII)).append('=')
                     .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII)).append('&');
            }
        }
        if (!hashData.isEmpty()) {
            hashData.setLength(hashData.length() - 1);
        }
        if (!query.isEmpty()) {
            query.setLength(query.length() - 1);
        }

        String secureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        return vnPayConfig.getPayUrl() + "?" + query.toString() + "&vnp_SecureHash=" + secureHash;
    }

    @Override
    public ScheduleGridResponse getScheduleGrid(LocalDate date, Long pitchTypeId) {
        if (date.isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.PAST_DATE_NOT_ALLOWED);
        }

        ScheduleGridResponse response = new ScheduleGridResponse();

        // 1. Dữ liệu Bookings
        List<Booking> bookings = bookingRepository.findOccupyingBookings(date, pitchTypeId);

        List<ScheduleGridItemResponse> gridItems = bookings.stream()
                .map(b -> new ScheduleGridItemResponse(
                        b.getPitch().getId(),
                        b.getTimeSlot().getId(),
                        b.getStatus().name()
                ))
                .collect(Collectors.toList());

        response.setBookings(gridItems);

        // 2. Dữ liệu Bảng giá
        DayType currentDayType = determineDayType(date);
        List<PriceMatrix> matrices = priceMatrixRepository.findByDayType(currentDayType);

        List<PriceItemResponse> priceItems = matrices.stream()
                .map(m -> new PriceItemResponse(m.getPitchType().getId(), m.getIsPeakHour(), m.getPrice()))
                .collect(Collectors.toList());

        response.setPrices(priceItems);

        return response;
    }

    @Override
    public PageResponse<BookingSummaryResponse> getMyBookings(
            BookingSearchRequest request,
            UserPrincipal currentUser
    ) {
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        BookingSearchRequest searchRequest = (request != null) ? request : new BookingSearchRequest();
        Specification<Booking> spec = BookingSpecification.filterMyBookings(searchRequest, currentUser.getId());
        Pageable pageable = searchRequest.toPageable("createdAt", Sort.Direction.DESC);

        Page<Booking> bookingPage = bookingRepository.findAll(spec, pageable);
        Page<BookingSummaryResponse> responsePage = bookingPage.map(bookingMapper::toBookingSummaryResponse);
        return PageResponse.from(responsePage);
    }

    @Override
    public BookingResponse getMyBookingDetail(Long bookingId, UserPrincipal currentUser) {
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Booking booking = bookingRepository.findByIdAndCustomerIdAndIsDeletedFalse(bookingId, currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        return bookingMapper.toBookingResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long bookingId, BookingCancelRequest request, UserPrincipal currentUser) {
        if (currentUser == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (request != null && request.getReason() != null && request.getReason().trim().isEmpty()) {
            throw new AppException(ErrorCode.BOOKING_CANCEL_REASON_REQUIRED);
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (Boolean.TRUE.equals(booking.getIsDeleted())) {
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND);
        }

        // Kiểm tra đúng đơn của mình (API dành cho khách hàng)
        if (booking.getCustomer() == null || !booking.getCustomer().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 1. Kiểm tra trạng thái: chỉ cho phép hủy khi ở trạng thái WAITING_APPROVAL hoặc CONFIRMED
        if (booking.getStatus() != BookingStatus.WAITING_APPROVAL && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.CANNOT_CANCEL_BOOKING_STATUS);
        }

        // 2. Kiểm tra thời gian: phải hủy trước thời gian bắt đầu ca đá ít nhất 24 giờ
        LocalTime startTime = booking.getStartTimeSnapshot() != null 
                ? booking.getStartTimeSnapshot() 
                : (booking.getTimeSlot() != null ? booking.getTimeSlot().getStartTime() : null);

        if (startTime == null) {
            throw new AppException(ErrorCode.TIME_SLOT_NOT_FOUND);
        }

        LocalDateTime startDateTime = LocalDateTime.of(booking.getBookingDate(), startTime);
        LocalDateTime now = LocalDateTime.now();

        if (now.plusHours(24).isAfter(startDateTime)) {
            throw new AppException(ErrorCode.CANNOT_CANCEL_WITHIN_24_HOURS);
        }

        // 3. Tạo bản ghi yêu cầu hủy đơn qua BookingCancellationService
        bookingCancellationService.createCancellation(booking, booking.getCustomer(), request);

        // 4. Chuyển trạng thái đơn sang WAITING_CANCELLATION
        booking.setStatus(BookingStatus.WAITING_CANCELLATION);
        if (request != null && request.getReason() != null && !request.getReason().trim().isEmpty()) {
            String currentNote = booking.getCustomerNote();
            String cancelReasonNote = "[Yêu cầu hủy: " + request.getReason().trim() + "]";
            booking.setCustomerNote(currentNote != null && !currentNote.isEmpty() 
                    ? currentNote + " " + cancelReasonNote 
                    : cancelReasonNote);
        }

        booking = bookingRepository.save(booking);

        // 5. Phát WebSocket thông báo trạng thái cập nhật cho bảng lịch
        BookingEventDto event = BookingEventDto.builder()
                .action("BOOKING_CANCEL_REQUESTED")
                .pitchId(booking.getPitch().getId())
                .timeSlotId(booking.getTimeSlot().getId())
                .date(booking.getBookingDate().toString())
                .status(BookingStatus.WAITING_CANCELLATION.name())
                .build();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messagingTemplate.convertAndSend("/topic/schedule", event);
                }
            });
        } else {
            messagingTemplate.convertAndSend("/topic/schedule", event);
        }

        return bookingMapper.toBookingResponse(booking);
    }

    @Override
    public com.fpms.dto.response.AdminTimelineResponse getAdminTimeline(LocalDate date, Long pitchTypeId) {
        if (date == null) {
            date = LocalDate.now();
        }

        // 1. Lấy danh sách sân
        List<com.fpms.entity.Pitch> pitches = pitchRepository.findAllByIsDeletedFalse();
        if (pitchTypeId != null && pitchTypeId > 0) {
            pitches = pitches.stream()
                    .filter(p -> p.getPitchType() != null && p.getPitchType().getId().equals(pitchTypeId))
                    .collect(Collectors.toList());
        }

        // 2. Lấy danh sách khung giờ hoạt động
        List<com.fpms.entity.TimeSlot> timeSlots = timeSlotRepository.findAllByIsActiveTrueOrderByStartTimeAsc();

        // 3. Lấy tất cả đơn đặt sân trong ngày
        List<Booking> bookings = bookingRepository.findOccupyingBookings(date, pitchTypeId);
        Map<String, Booking> bookingMap = new HashMap<>();
        for (Booking b : bookings) {
            if (b.getPitch() != null && b.getTimeSlot() != null) {
                String key = b.getPitch().getId() + "_" + b.getTimeSlot().getId();
                bookingMap.put(key, b);
            }
        }

        List<com.fpms.dto.response.AdminTimelineSlotResponse> slotResponses = new ArrayList<>();

        for (com.fpms.entity.Pitch pitch : pitches) {
            for (com.fpms.entity.TimeSlot slot : timeSlots) {
                String key = pitch.getId() + "_" + slot.getId();
                Booking booking = bookingMap.get(key);

                String status;
                Long bookingId = null;
                String bookingCode = null;
                String guestName = null;
                String guestPhone = null;
                BigDecimal totalAmount = null;
                BigDecimal depositAmount = null;
                BigDecimal remainingAmount = null;
                String customerNote = null;
                String cancellationReason = null;

                if (pitch.getStatus() == com.fpms.entity.enums.PitchStatus.MAINTENANCE || pitch.getStatus() == com.fpms.entity.enums.PitchStatus.INACTIVE) {
                    status = "MAINTENANCE";
                } else if (booking != null) {
                    status = booking.getStatus().name();
                    bookingId = booking.getId();
                    bookingCode = booking.getBookingCode();
                    guestName = booking.getGuestName();
                    guestPhone = booking.getGuestPhone();
                    totalAmount = booking.getTotalPitchAmount() != null
                            ? booking.getTotalPitchAmount().add(booking.getAdditionalFee() != null ? booking.getAdditionalFee() : BigDecimal.ZERO)
                            : BigDecimal.ZERO;
                    depositAmount = booking.getDepositAmount();
                    remainingAmount = booking.getRemainingAmount();
                    customerNote = booking.getCustomerNote();

                    if (booking.getStatus() == BookingStatus.WAITING_CANCELLATION) {
                        cancellationReason = bookingCancellationRepository.findByBookingId(booking.getId())
                                .map(com.fpms.entity.BookingCancellation::getReason)
                                .orElse(null);
                    }
                } else {
                    status = "AVAILABLE";
                }

                slotResponses.add(com.fpms.dto.response.AdminTimelineSlotResponse.builder()
                        .pitchId(pitch.getId())
                        .pitchName(pitch.getName())
                        .pitchTypeName(pitch.getPitchType() != null ? pitch.getPitchType().getName() : "")
                        .timeSlotId(slot.getId())
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .bookingDate(date)
                        .status(status)
                        .bookingId(bookingId)
                        .bookingCode(bookingCode)
                        .guestName(guestName)
                        .guestPhone(guestPhone)
                        .totalAmount(totalAmount)
                        .depositAmount(depositAmount)
                        .remainingAmount(remainingAmount)
                        .customerNote(customerNote)
                        .cancellationReason(cancellationReason)
                        .build());
            }
        }

        return com.fpms.dto.response.AdminTimelineResponse.builder()
                .date(date)
                .bookings(slotResponses)
                .build();
    }
}
