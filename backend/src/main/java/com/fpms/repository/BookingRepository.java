package com.fpms.repository;

import com.fpms.entity.Booking;
import com.fpms.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    boolean existsByTimeSlotIdAndBookingDateGreaterThanEqualAndStatusInAndIsDeletedFalse(
            Long timeSlotId,
            LocalDate bookingDate,
            Collection<BookingStatus> statuses
    );

    @org.springframework.data.jpa.repository.Query("SELECT b FROM Booking b WHERE b.bookingDate = :date " +
       "AND b.status NOT IN ('CANCELLED', 'REFUNDED') " +
       "AND (:pitchTypeId IS NULL OR b.pitch.pitchType.id = :pitchTypeId)")
    java.util.List<Booking> findOccupyingBookings(
            @org.springframework.data.repository.query.Param("date") LocalDate date, 
            @org.springframework.data.repository.query.Param("pitchTypeId") Long pitchTypeId
    );
    boolean existsByPitchIdAndTimeSlotIdAndBookingDateAndStatusNotIn(
            Long pitchId,
            Long timeSlotId,
            LocalDate bookingDate,
            Collection<BookingStatus> statuses
    );

    boolean existsByPitchId(Long pitchId);

    java.util.Optional<Booking> findByBookingCode(String bookingCode);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Booking b SET b.status = 'CANCELLED', b.holdExpiresAt = null WHERE b.status = 'PENDING_HOLD' AND (b.holdExpiresAt IS NULL OR b.holdExpiresAt <= :now)")
    int cancelExpiredHolds(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);


    java.util.Optional<Booking> findByIdAndCustomerIdAndIsDeletedFalse(Long id, Long customerId);
    // ==================== BÁO CÁO THỐNG KÊ (REPORTS & ANALYTICS) ====================

    /**
     * Gộp tính tổng doanh thu (SUM) và đếm số lượng đơn (COUNT) theo từng trạng thái (GROUP BY status)
     * chỉ trong 1 lần quét Composite Index idx_bookings_date_status.
     * Mỗi dòng trả về Object[]: [0] = BookingStatus, [1] = Long (count), [2] = BigDecimal (revenue).
     */
    @Query("SELECT b.status, COUNT(b), " +
           "COALESCE(SUM(COALESCE(b.totalPitchAmount, 0) + COALESCE(b.additionalFee, 0)), 0) " +
           "FROM Booking b " +
           "WHERE b.isDeleted = false " +
           "AND b.bookingDate >= :startDate AND b.bookingDate <= :endDate " +
           "GROUP BY b.status")
    List<Object[]> getBookingStatsByStatusBetweenDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Đếm trực tiếp số lượng khách hàng quay lại (có từ 2 đơn hợp lệ trở lên trong kỳ)
     * ngay tại CSDL, tránh tải danh sách ID khách hàng lên bộ nhớ JVM.
     */
    @Query(value = "SELECT COUNT(*) FROM (" +
                   "    SELECT b.customer_id " +
                   "    FROM bookings b " +
                   "    WHERE b.is_deleted = false " +
                   "      AND b.customer_id IS NOT NULL " +
                   "      AND b.booking_date >= :startDate AND b.booking_date <= :endDate " +
                   "      AND b.status NOT IN ('CANCELLED', 'REFUNDED') " +
                   "    GROUP BY b.customer_id " +
                   "    HAVING COUNT(b.id) >= 2" +
                   ") AS repeat_customers_subquery",
           nativeQuery = true)
    long countRepeatCustomersBetweenDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(COALESCE(b.totalPitchAmount, 0) + COALESCE(b.additionalFee, 0)), 0) FROM Booking b " +
           "WHERE b.isDeleted = false " +
           "AND b.bookingDate >= :startDate AND b.bookingDate <= :endDate " +
           "AND b.status = BookingStatus.COMPLETED")
    java.math.BigDecimal sumRevenueBetweenDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT b.status, COUNT(b) FROM Booking b " +
           "WHERE b.isDeleted = false " +
           "AND b.bookingDate >= :startDate AND b.bookingDate <= :endDate " +
           "GROUP BY b.status")
    List<Object[]> countBookingsByStatusBetweenDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT b.bookingDate, COALESCE(SUM(COALESCE(b.totalPitchAmount, 0) + COALESCE(b.additionalFee, 0)), 0), COUNT(b) FROM Booking b " +
           "WHERE b.isDeleted = false " +
           "AND b.bookingDate >= :startDate AND b.bookingDate <= :endDate " +
           "AND b.status = BookingStatus.COMPLETED " +
           "GROUP BY b.bookingDate " +
           "ORDER BY b.bookingDate ASC")
    List<Object[]> getDailyRevenueBetweenDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT b.bookingDate, COUNT(b) FROM Booking b " +
           "WHERE b.isDeleted = false " +
           "AND b.bookingDate >= :startDate AND b.bookingDate <= :endDate " +
           "AND b.status NOT IN (BookingStatus.CANCELLED, BookingStatus.REFUNDED) " +
           "GROUP BY b.bookingDate")
    List<Object[]> getDailyBookingCountBetweenDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT pt.id, pt.name, COALESCE(SUM(COALESCE(b.totalPitchAmount, 0) + COALESCE(b.additionalFee, 0)), 0), COUNT(b) " +
           "FROM Booking b " +
           "JOIN b.pitch p " +
           "JOIN p.pitchType pt " +
           "WHERE b.isDeleted = false " +
           "AND b.bookingDate >= :startDate AND b.bookingDate <= :endDate " +
           "AND b.status = BookingStatus.COMPLETED " +
           "GROUP BY pt.id, pt.name")
    List<Object[]> getRevenueByPitchTypeBetweenDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(DISTINCT b.customer.id) FROM Booking b " +
           "WHERE b.isDeleted = false " +
           "AND b.customer IS NOT NULL " +
           "AND b.bookingDate >= :startDate AND b.bookingDate <= :endDate " +
           "AND b.status NOT IN (BookingStatus.CANCELLED, BookingStatus.REFUNDED)")
    long countDistinctCustomersBetweenDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT b.customer.id, COUNT(b) FROM Booking b " +
           "WHERE b.isDeleted = false " +
           "AND b.customer IS NOT NULL " +
           "AND b.bookingDate >= :startDate AND b.bookingDate <= :endDate " +
           "AND b.status NOT IN (BookingStatus.CANCELLED, BookingStatus.REFUNDED) " +
           "GROUP BY b.customer.id " +
           "HAVING COUNT(b) >= 2")
    List<Object[]> findRepeatCustomerIdsBetweenDates(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT b FROM Booking b " +
           "LEFT JOIN FETCH b.customer c " +
           "JOIN FETCH b.pitch p " +
           "JOIN FETCH p.pitchType pt " +
           "JOIN FETCH b.timeSlot ts " +
           "WHERE b.isDeleted = false " +
           "AND b.bookingDate >= :startDate AND b.bookingDate <= :endDate " +
           "AND b.status = BookingStatus.COMPLETED " +
           "ORDER BY b.bookingDate DESC, b.createdAt DESC")
    List<Booking> findBookingsForReportExport(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
