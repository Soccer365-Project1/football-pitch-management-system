package com.fpms.entity;

import com.fpms.common.entity.BaseEntity;
import com.fpms.entity.enums.BookingStatus;
import com.fpms.entity.enums.BookingType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "bookings")
public class Booking extends BaseEntity {

    @Column(name = "booking_code", length = 30, nullable = false, unique = true)
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private User customer; // NULL nếu khách vãng lai đặt tại quầy

    @Column(name = "guest_name", length = 100)
    private String guestName;

    @Column(name = "guest_phone", length = 15)
    private String guestPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private User staff; // Nhân viên tạo đơn hoặc duyệt đơn

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pitch_id", nullable = false)
    private Pitch pitch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private TimeSlot timeSlot;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "price_snapshot", precision = 12, scale = 2, nullable = false)
    private BigDecimal priceSnapshot; // Đơn giá chốt tại thời điểm đặt (Bảo toàn lịch sử)

    @Column(name = "total_pitch_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalPitchAmount; // Tổng tiền sân

    @Column(name = "deposit_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal depositAmount = BigDecimal.ZERO; // Tiền cọc 30%

    @Column(name = "additional_fee", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal additionalFee = BigDecimal.ZERO; // Phụ phí phát sinh

    @Column(name = "remaining_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal remainingAmount = BigDecimal.ZERO; // Tiền còn lại phải thu khi đóng ca

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type", length = 20, nullable = false)
    @Builder.Default
    private BookingType bookingType = BookingType.ONLINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING_HOLD;

    @Column(name = "hold_expires_at")
    private LocalDateTime holdExpiresAt; // Thời điểm hết hạn tạm khóa 10 phút

    @Column(name = "customer_note", columnDefinition = "TEXT")
    private String customerNote;
}
