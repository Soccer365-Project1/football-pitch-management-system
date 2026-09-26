package com.fpms.repository;

import com.fpms.entity.Booking;
import com.fpms.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;

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
}
