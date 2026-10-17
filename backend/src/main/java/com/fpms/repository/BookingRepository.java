package com.fpms.repository;

import com.fpms.entity.Booking;
import com.fpms.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

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
    @org.springframework.data.jpa.repository.Query("UPDATE Booking b SET b.status = 'CANCELLED' WHERE b.status = 'PENDING_HOLD' AND b.holdExpiresAt <= :now")
    int cancelExpiredHolds(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);

    @org.springframework.data.jpa.repository.Query("SELECT b FROM Booking b WHERE b.customer.id = :customerId " +
           "AND b.isDeleted = false " +
           "AND (:status IS NULL OR b.status = :status) " +
           "AND (:fromDate IS NULL OR b.bookingDate >= :fromDate) " +
           "AND (:toDate IS NULL OR b.bookingDate <= :toDate)")
    org.springframework.data.domain.Page<Booking> findMyBookings(
            @org.springframework.data.repository.query.Param("customerId") Long customerId,
            @org.springframework.data.repository.query.Param("status") BookingStatus status,
            @org.springframework.data.repository.query.Param("fromDate") LocalDate fromDate,
            @org.springframework.data.repository.query.Param("toDate") LocalDate toDate,
            org.springframework.data.domain.Pageable pageable
    );

    java.util.Optional<Booking> findByIdAndCustomerIdAndIsDeletedFalse(Long id, Long customerId);
    // ==================== BÁO CÁO THỐNG KÊ (REPORTS & ANALYTICS) ====================

    @Query("SELECT COALESCE(SUM(b.totalPitchAmount + b.additionalFee), 0) FROM Booking b " +
           "WHERE b.isDeleted = false " +
           "AND b.bookingDate >= :startDate AND b.bookingDate <= :endDate " +
           "AND b.status IN (BookingStatus.CONFIRMED, " +
           "                 BookingStatus.IN_PROGRESS, " +
           "                 BookingStatus.COMPLETED)")
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

    @Query("SELECT b.bookingDate, COALESCE(SUM(b.totalPitchAmount + b.additionalFee), 0), COUNT(b) FROM Booking b " +
           "WHERE b.isDeleted = false " +
           "AND b.bookingDate >= :startDate AND b.bookingDate <= :endDate " +
           "AND b.status IN (BookingStatus.CONFIRMED, " +
           "                 BookingStatus.IN_PROGRESS, " +
           "                 BookingStatus.COMPLETED) " +
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

    @Query("SELECT pt.id, pt.name, COALESCE(SUM(b.totalPitchAmount + b.additionalFee), 0), COUNT(b) " +
           "FROM Booking b " +
           "JOIN b.pitch p " +
           "JOIN p.pitchType pt " +
           "WHERE b.isDeleted = false " +
           "AND b.bookingDate >= :startDate AND b.bookingDate <= :endDate " +
           "AND b.status IN (BookingStatus.CONFIRMED, " +
           "                 BookingStatus.IN_PROGRESS, " +
           "                 BookingStatus.COMPLETED) " +
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
}
