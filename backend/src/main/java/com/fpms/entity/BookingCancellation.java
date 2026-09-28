package com.fpms.entity;

import com.fpms.entity.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "booking_cancellations")
public class BookingCancellation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy; // Nhân viên / Chủ sân xử lý yêu cầu hủy

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_method_id")
    private PaymentMethod refundMethod; // Phương thức hoàn tiền

    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason; // Lý do hủy sân của khách

    @Column(name = "refund_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", length = 20, nullable = false)
    @Builder.Default
    private RefundStatus refundStatus = RefundStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason; // Lý do từ chối (nếu REJECTED)

    @Column(name = "requested_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
